package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CategoryCore
import com.example.data.LedgerDatabase
import com.example.data.LedgerRepository
import com.example.data.Transaction
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale

enum class Screen {
    LOG, LEDGER, ANALYSE, CONFIG
}

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LedgerDatabase.getDatabase(application)
    private val repository = LedgerRepository(database.categoryCoreDao(), database.transactionDao())

    private val sharedPrefs = application.getSharedPreferences("ledger_sys_prefs", Context.MODE_PRIVATE)

    // Global UI Settings
    val isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("is_dark_theme", true))
    val activeCurrency = MutableStateFlow(sharedPrefs.getString("active_currency", "$") ?: "$")
    val customCurrencySymbol = MutableStateFlow(sharedPrefs.getString("custom_currency_symbol", "") ?: "")
    val isReminderEnabled = MutableStateFlow(sharedPrefs.getBoolean("reminder_enabled", true))
    val reminderTime = MutableStateFlow(sharedPrefs.getString("reminder_time", "20:00") ?: "20:00")

    // Google Cloud Sync settings
    val isGoogleSignedIn = MutableStateFlow(sharedPrefs.getBoolean("google_signed_in", false))
    val googleUserEmail = MutableStateFlow(sharedPrefs.getString("google_user_email", "") ?: "")
    val googleUserName = MutableStateFlow(sharedPrefs.getString("google_user_name", "") ?: "")
    val googleUserAvatar = MutableStateFlow(sharedPrefs.getString("google_user_avatar", "") ?: "")
    val cloudSyncStatus = MutableStateFlow("IDLE")

    // Internal Google Auth Token
    private var googleIdToken: String? = sharedPrefs.getString("google_id_token", null)

    // Navigation state
    val currentScreen = MutableStateFlow(Screen.LOG)

    // Data streams
    val allCores: StateFlow<List<CategoryCore>> = repository.allCores.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Transaction Logging inputs
    val loggingMemo = MutableStateFlow("")
    val loggingAmountBuffer = MutableStateFlow("0") // Keypad entry buffer
    val selectedLoggingCategory = MutableStateFlow("food") // matches category core systemKey

    // Ledger view filters
    val ledgerSearchQuery = MutableStateFlow("")
    val ledgerCategoryFilter = MutableStateFlow("ALL CORES") // "ALL CORES" or core systemKey

    // Status notifications for actions
    val statusMessage = MutableStateFlow("")

    // Admin Core Registration form state
    val newCoreName = MutableStateFlow("")
    val newCoreIcon = MutableStateFlow("link") // default selected icon name

    // Analytics Granularity: DAILY, WEEKLY, MONTHLY
    val analyticsInterval = MutableStateFlow("DAILY")

    init {
        // Automatically match first logging category when cores are loaded
        viewModelScope.launch {
            allCores.collect { cores ->
                if (cores.isNotEmpty() && selectedLoggingCategory.value !in cores.map { it.systemKey }) {
                    selectedLoggingCategory.value = cores.first().systemKey
                }
            }
        }
    }

    // Toggle Theme
    fun toggleTheme() {
        val next = !isDarkTheme.value
        isDarkTheme.value = next
        sharedPrefs.edit().putBoolean("is_dark_theme", next).apply()
        showStatus("THEME CONFIGURED: ${if (next) "DARK" else "LIGHT"} MONOCHROME")
    }

    // Change Currency
    fun setCurrency(symbol: String) {
        activeCurrency.value = symbol
        sharedPrefs.edit().putString("active_currency", symbol).apply()
        showStatus("REGISTRY CURRENCY DEFINED: [$symbol]")
    }

    fun setCustomCurrencySymbol(symbol: String) {
        customCurrencySymbol.value = symbol
        sharedPrefs.edit().putString("custom_currency_symbol", symbol).apply()
    }

    // Toggle Reminder
    fun toggleReminder() {
        val next = !isReminderEnabled.value
        isReminderEnabled.value = next
        sharedPrefs.edit().putBoolean("reminder_enabled", next).apply()
        showStatus("REMINDER STATE UPDATED: ${if (next) "ENABLED" else "DISABLED"}")
    }

    // Save reminder time
    fun setReminderTimeValue(time: String) {
        reminderTime.value = time
        sharedPrefs.edit().putString("reminder_time", time).apply()
    }

    // Helper to post status message
    fun showStatus(msg: String) {
        statusMessage.value = msg
        // Automatically clear status message after some time or let user dismiss
    }

    fun clearStatus() {
        statusMessage.value = ""
    }

    // Keypad Logic
    fun handleKeypadPress(key: Char) {
        val current = loggingAmountBuffer.value
        when (key) {
            'C' -> {
                loggingAmountBuffer.value = "0"
            }
            '.' -> {
                if (!current.contains('.')) {
                    loggingAmountBuffer.value = current + "."
                }
            }
            else -> {
                if (current == "0") {
                    loggingAmountBuffer.value = key.toString()
                } else {
                    // Limit length to avoid layout overflow
                    if (current.length < 9) {
                        loggingAmountBuffer.value = current + key
                    }
                }
            }
        }
    }

    // Commit Transaction
    fun commitTransaction() {
        val amt = loggingAmountBuffer.value.toDoubleOrNull() ?: 0.0
        if (amt <= 0.0) {
            showStatus("ERR: AMOUNT MUST BE GREATER THAN 0.00")
            return
        }
        val memoText = loggingMemo.value.trim()
        val catKey = selectedLoggingCategory.value

        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    amount = amt,
                    currency = activeCurrency.value,
                    memo = if (memoText.isEmpty()) "UNNAMED TRANSACTION" else memoText,
                    categoryKey = catKey,
                    timestamp = System.currentTimeMillis()
                )
            )
            // Reset entry form
            loggingAmountBuffer.value = "0"
            loggingMemo.value = ""
            showStatus("SUCCESS: TRANSACTION COMMITTED TO MEMORY")
        }
    }

    // Reset All / Purge Database
    fun purgeDatabase() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            showStatus("REGISTRY PURGED: ALL RECORDS DESTROYED")
        }
    }

    // Delete Transaction
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            showStatus("SUCCESS: TRANSACTION REMOVED FROM REGISTER")
        }
    }

    // Update Transaction
    fun updateTransaction(id: Long, amount: Double, memo: String, categoryKey: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    id = id,
                    amount = amount,
                    currency = activeCurrency.value,
                    memo = memo,
                    categoryKey = categoryKey,
                    timestamp = timestamp
                )
            )
            showStatus("SUCCESS: REGISTER RECORD UPDATED")
        }
    }

    // Data interchange: SAF file exporter
    fun writeBackupToUri(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val coresJson = JSONArray()
                allCores.value.forEach {
                    val obj = JSONObject().apply {
                        put("systemKey", it.systemKey)
                        put("name", it.name)
                        put("iconName", it.iconName)
                        put("isSystemProtected", it.isSystemProtected)
                    }
                    coresJson.put(obj)
                }

                val txJson = JSONArray()
                allTransactions.value.forEach {
                    val obj = JSONObject().apply {
                        put("amount", it.amount)
                        put("currency", it.currency)
                        put("memo", it.memo)
                        put("categoryKey", it.categoryKey)
                        put("timestamp", it.timestamp)
                    }
                    txJson.put(obj)
                }

                val backup = JSONObject().apply {
                    put("cores", coresJson)
                    put("transactions", txJson)
                    put("system", "LEDGER.SYS")
                    put("version", 1)
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(backup.toString().toByteArray())
                }
                showStatus("BACKUP EXPORTED: FILE WRITTEN SUCCESSFULLY")
            } catch (e: Exception) {
                showStatus("ERR EXPORTING: ${e.message}")
            }
        }
    }

    // Data interchange: SAF file importer
    fun readBackupFromUri(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (content.isNullOrEmpty()) {
                    showStatus("ERR: FILE IS EMPTY")
                    return@launch
                }

                val backup = JSONObject(content)
                if (!backup.has("system") || backup.getString("system") != "LEDGER.SYS") {
                    showStatus("ERR: INVALID LEDGER.SYS BACKUP FILE")
                    return@launch
                }

                // Import cores
                if (backup.has("cores")) {
                    val coresArray = backup.getJSONArray("cores")
                    for (i in 0 until coresArray.length()) {
                        val obj = coresArray.getJSONObject(i)
                        val sysKey = obj.getString("systemKey")
                        val name = obj.getString("name")
                        val icon = obj.getString("iconName")
                        val isProtected = obj.optBoolean("isSystemProtected", false)
                        repository.insertCore(CategoryCore(sysKey, name, icon, isProtected))
                    }
                }

                // Import transactions
                if (backup.has("transactions")) {
                    val txArray = backup.getJSONArray("transactions")
                    for (i in 0 until txArray.length()) {
                        val obj = txArray.getJSONObject(i)
                        val amount = obj.getDouble("amount")
                        val currency = obj.getString("currency")
                        val memo = obj.getString("memo")
                        val categoryKey = obj.getString("categoryKey")
                        val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        repository.insertTransaction(
                            Transaction(
                                amount = amount,
                                currency = currency,
                                memo = memo,
                                categoryKey = categoryKey,
                                timestamp = timestamp
                            )
                        )
                    }
                }

                showStatus("BACKUP IMPORTED: SYSTEM STATE RESTORED")
            } catch (e: Exception) {
                showStatus("ERR IMPORTING: INVALID FILE FORMAT")
            }
        }
    }

    // Register a custom core
    fun registerCustomCore() {
        val name = newCoreName.value.trim().uppercase()
        if (name.isEmpty()) {
            showStatus("ERR: CORE IDENTIFIER IS EMPTY")
            return
        }
        if (name.length > 8) {
            showStatus("ERR: MAX LENGTH 8 CHARACTERS")
            return
        }
        val systemKey = name.lowercase()

        viewModelScope.launch {
            val currentCores = allCores.value
            if (currentCores.any { it.systemKey == systemKey }) {
                showStatus("ERR: CORE REGISTRY DUPLICATE KEY")
                return@launch
            }

            repository.insertCore(
                CategoryCore(
                    systemKey = systemKey,
                    name = name,
                    iconName = newCoreIcon.value,
                    isSystemProtected = false
                )
            )
            // Reset input form
            newCoreName.value = ""
            showStatus("SUCCESS: CUSTOM CORE REGISTERED ($name)")
        }
    }

    fun deleteCustomCore(key: String) {
        viewModelScope.launch {
            repository.deleteCoreByKey(key)
            showStatus("SUCCESS: CUSTOM CORE REMOVED ($key)")
        }
    }

    // EXPORT BACKUP TO CLIPBOARD (JSON format)
    fun exportBackup() {
        viewModelScope.launch {
            try {
                val coresJson = JSONArray()
                allCores.value.forEach {
                    val obj = JSONObject().apply {
                        put("systemKey", it.systemKey)
                        put("name", it.name)
                        put("iconName", it.iconName)
                        put("isSystemProtected", it.isSystemProtected)
                    }
                    coresJson.put(obj)
                }

                val txJson = JSONArray()
                allTransactions.value.forEach {
                    val obj = JSONObject().apply {
                        put("amount", it.amount)
                        put("currency", it.currency)
                        put("memo", it.memo)
                        put("categoryKey", it.categoryKey)
                        put("timestamp", it.timestamp)
                    }
                    txJson.put(obj)
                }

                val backup = JSONObject().apply {
                    put("cores", coresJson)
                    put("transactions", txJson)
                    put("system", "LEDGER.SYS")
                    put("version", 1)
                }

                val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ledger_sys_backup", backup.toString())
                clipboard.setPrimaryClip(clip)

                showStatus("BACKUP EXPORTED: JSON COPIED TO CLIPBOARD")
            } catch (e: Exception) {
                showStatus("ERR EXPORTING BACKUP: ${e.message}")
            }
        }
    }

    // IMPORT BACKUP FROM CLIPBOARD
    fun importBackup() {
        viewModelScope.launch {
            try {
                val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboard.primaryClip
                if (clipData == null || clipData.itemCount == 0) {
                    showStatus("ERR: CLIPBOARD IS EMPTY")
                    return@launch
                }
                val jsonStr = clipData.getItemAt(0).text?.toString()
                if (jsonStr.isNullOrEmpty()) {
                    showStatus("ERR: NO TEXT IN CLIPBOARD")
                    return@launch
                }

                val backup = JSONObject(jsonStr)
                if (!backup.has("system") || backup.getString("system") != "LEDGER.SYS") {
                    showStatus("ERR: INVALID LEDGER.SYS BACKUP DATA")
                    return@launch
                }

                // Import cores
                if (backup.has("cores")) {
                    val coresArray = backup.getJSONArray("cores")
                    for (i in 0 until coresArray.length()) {
                        val obj = coresArray.getJSONObject(i)
                        val sysKey = obj.getString("systemKey")
                        val name = obj.getString("name")
                        val icon = obj.getString("iconName")
                        val isProtected = obj.optBoolean("isSystemProtected", false)
                        repository.insertCore(CategoryCore(sysKey, name, icon, isProtected))
                    }
                }

                // Import transactions
                if (backup.has("transactions")) {
                    val txArray = backup.getJSONArray("transactions")
                    for (i in 0 until txArray.length()) {
                        val obj = txArray.getJSONObject(i)
                        val amount = obj.getDouble("amount")
                        val currency = obj.getString("currency")
                        val memo = obj.getString("memo")
                        val categoryKey = obj.getString("categoryKey")
                        val timestamp = obj.getLong("timestamp")
                        repository.insertTransaction(
                            Transaction(
                                amount = amount,
                                currency = currency,
                                memo = memo,
                                categoryKey = categoryKey,
                                timestamp = timestamp
                            )
                        )
                    }
                }

                showStatus("BACKUP IMPORTED: SYSTEM STATE RESTORED")
            } catch (e: Exception) {
                showStatus("ERR IMPORTING BACKUP: INVALID FORMAT / SYNTAX")
            }
        }
    }

    // Helper to get Google Drive Service
    private fun getDriveService(token: String): Drive {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        return Drive.Builder(httpTransport, jsonFactory, null)
            .setApplicationName("Ledger App")
            .setHttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $token"
            }
            .build()
    }

    // Google Sign-In & Sign-Out handlers
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            val credentialManager = CredentialManager.create(context)
            
            // NOTE: In a real app, this Web Client ID must be fetched from Google Cloud Console.
            // Replace this placeholder with your actual Web Client ID.
            val clientId = "673008559053-94o3dngqk5qd111o0s7gf87uiea28ot5.apps.googleusercontent.com" 

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                if (credential is GoogleIdTokenCredential) {
                    val googleIdTokenCredential = credential
                    val email = googleIdTokenCredential.id
                    val name = googleIdTokenCredential.displayName ?: "REGISTERED USER"
                    val avatar = googleIdTokenCredential.profilePictureUri?.toString() ?: ""
                    val token = googleIdTokenCredential.idToken
                    
                    googleIdToken = token
                    isGoogleSignedIn.value = true
                    googleUserEmail.value = email
                    googleUserName.value = name
                    googleUserAvatar.value = avatar
                    
                    sharedPrefs.edit()
                        .putBoolean("google_signed_in", true)
                        .putString("google_user_email", email)
                        .putString("google_user_name", name)
                        .putString("google_user_avatar", avatar)
                        .putString("google_id_token", token)
                        .apply()
                    
                    showStatus("CONNECTED: WELCOME ${name.uppercase()}")
                } else {
                    showStatus("ERR: UNEXPECTED CREDENTIAL TYPE")
                }
            } catch (e: GetCredentialException) {
                Log.e("LedgerViewModel", "Sign-in failed", e)
                val msg = if (e.message?.contains("No credentials") == true) {
                    "SIGN-IN ERR: NO ACCOUNTS OR INVALID CLIENT ID. CHECK GOOGLE CLOUD CONSOLE."
                } else {
                    "SIGN-IN FAILED: ${e.message}"
                }
                showStatus(msg)
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Unknown error", e)
                showStatus("ERR: ${e.localizedMessage}")
            }
        }
    }

    fun signOutGoogle(context: Context) {
        viewModelScope.launch {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            
            isGoogleSignedIn.value = false
            googleUserEmail.value = ""
            googleUserName.value = ""
            googleUserAvatar.value = ""
            googleIdToken = null
            cloudSyncStatus.value = "IDLE"
            
            sharedPrefs.edit()
                .putBoolean("google_signed_in", false)
                .putString("google_user_email", "")
                .putString("google_user_name", "")
                .putString("google_user_avatar", "")
                .remove("google_id_token")
                .apply()
            
            showStatus("DISCONNECTED FROM GOOGLE CORE")
        }
    }

    // Google Drive Sync - Upload current ledger JSON
    fun syncToGoogleDrive() {
        val token = googleIdToken ?: run {
            showStatus("ERR: GOOGLE CORE NOT CONNECTED")
            return
        }
        
        viewModelScope.launch {
            cloudSyncStatus.value = "SYNCING"
            try {
                val drive = getDriveService(token)
                
                // Prepare JSON
                val coresJson = JSONArray()
                allCores.value.forEach {
                    val obj = JSONObject().apply {
                        put("systemKey", it.systemKey)
                        put("name", it.name)
                        put("iconName", it.iconName)
                        put("isSystemProtected", it.isSystemProtected)
                    }
                    coresJson.put(obj)
                }

                val txJson = JSONArray()
                allTransactions.value.forEach {
                    val obj = JSONObject().apply {
                        put("amount", it.amount)
                        put("currency", it.currency)
                        put("memo", it.memo)
                        put("categoryKey", it.categoryKey)
                        put("timestamp", it.timestamp)
                    }
                    txJson.put(obj)
                }

                val backup = JSONObject().apply {
                    put("cores", coresJson)
                    put("transactions", txJson)
                    put("system", "LEDGER.SYS")
                    put("version", 1)
                }

                val content = backup.toString()
                
                // Search for existing file
                val query = "name = 'LEDGER_BACKUP.JSON' and trashed = false"
                val files = drive.files().list().setQ(query).execute().files
                
                val metadata = File().apply {
                    name = "LEDGER_BACKUP.JSON"
                    mimeType = "application/json"
                }
                
                val mediaContent = com.google.api.client.http.ByteArrayContent.fromString("application/json", content)

                if (files.isNullOrEmpty()) {
                    // Create new
                    drive.files().create(metadata, mediaContent).execute()
                    showStatus("GOOGLE DRIVE: NEW BACKUP INITIALIZED")
                } else {
                    // Update existing
                    val fileId = files[0].id
                    drive.files().update(fileId, null, mediaContent).execute()
                    showStatus("GOOGLE DRIVE: BACKUP REGISTER UPDATED")
                }

                cloudSyncStatus.value = "SUCCESS"
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Drive sync failed", e)
                cloudSyncStatus.value = "FAILED"
                showStatus("GOOGLE DRIVE ERR: ${e.localizedMessage}")
            }
        }
    }

    // Google Drive Sync - Download/Restore current ledger JSON
    fun syncFromGoogleDrive() {
        val token = googleIdToken ?: run {
            showStatus("ERR: GOOGLE CORE NOT CONNECTED")
            return
        }
        
        viewModelScope.launch {
            cloudSyncStatus.value = "SYNCING"
            try {
                val drive = getDriveService(token)
                val query = "name = 'LEDGER_BACKUP.JSON' and trashed = false"
                val files = drive.files().list().setQ(query).execute().files

                if (files.isNullOrEmpty()) {
                    cloudSyncStatus.value = "FAILED"
                    showStatus("GOOGLE DRIVE: NO BACKUP DATA DETECTED")
                    return@launch
                }

                val fileId = files[0].id
                val inputStream = drive.files().get(fileId).executeMediaAsInputStream()
                val jsonStr = inputStream.bufferedReader().use { it.readText() }

                val backup = JSONObject(jsonStr)
                if (!backup.has("system") || backup.getString("system") != "LEDGER.SYS") {
                    cloudSyncStatus.value = "FAILED"
                    showStatus("GOOGLE DRIVE: BACKUP FILE CORRUPT")
                    return@launch
                }

                // Import logic (same as local import)
                if (backup.has("cores")) {
                    val coresArray = backup.getJSONArray("cores")
                    for (i in 0 until coresArray.length()) {
                        val obj = coresArray.getJSONObject(i)
                        repository.insertCore(
                            CategoryCore(
                                systemKey = obj.getString("systemKey"),
                                name = obj.getString("name"),
                                iconName = obj.getString("iconName"),
                                isSystemProtected = obj.optBoolean("isSystemProtected", false)
                            )
                        )
                    }
                }

                if (backup.has("transactions")) {
                    val txArray = backup.getJSONArray("transactions")
                    for (i in 0 until txArray.length()) {
                        val obj = txArray.getJSONObject(i)
                        repository.insertTransaction(
                            Transaction(
                                amount = obj.getDouble("amount"),
                                currency = obj.getString("currency"),
                                memo = obj.getString("memo"),
                                categoryKey = obj.getString("categoryKey"),
                                timestamp = obj.getLong("timestamp")
                            )
                        )
                    }
                }

                cloudSyncStatus.value = "SUCCESS"
                showStatus("GOOGLE DRIVE: SYSTEM STATE RESTORED")
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Drive restore failed", e)
                cloudSyncStatus.value = "FAILED"
                showStatus("GOOGLE DRIVE ERR: ${e.localizedMessage}")
            }
        }
    }

    // Filtration computed states
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        ledgerSearchQuery,
        ledgerCategoryFilter
    ) { txList, query, catFilter ->
        txList.filter { tx ->
            val matchQuery = query.isEmpty() || tx.memo.contains(query, ignoreCase = true)
            val matchFilter = catFilter == "ALL CORES" || tx.categoryKey == catFilter
            matchQuery && matchFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Outflow metrics
    val todayOutflow: StateFlow<Double> = allTransactions.combine(activeCurrency) { txList, curr ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        txList.filter { it.timestamp >= todayStart && it.currency == curr }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val monthOutflow: StateFlow<Double> = allTransactions.combine(activeCurrency) { txList, curr ->
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        txList.filter { it.timestamp >= monthStart && it.currency == curr }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )
}
