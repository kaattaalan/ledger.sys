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
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val isGoogleSignedIn = MutableStateFlow(false)
    val googleUserEmail = MutableStateFlow("")
    val googleUserName = MutableStateFlow("")
    val googleUserAvatar = MutableStateFlow("")
    val cloudSyncStatus = MutableStateFlow("IDLE")

    val syncConsoleLogs = MutableStateFlow<List<String>>(emptyList())
    val currentSyncPayload = MutableStateFlow("")

    private val _recoverableAuthIntent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val recoverableAuthIntent: SharedFlow<Intent> = _recoverableAuthIntent.asSharedFlow()

    var pendingActionAfterConsent: String? = null

    // Navigation state
    val currentScreen = MutableStateFlow(Screen.LOG)

    init {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(application)
        if (lastAccount != null) {
            val email = lastAccount.email ?: ""
            val name = lastAccount.displayName ?: "REGISTERED USER"
            val avatar = lastAccount.photoUrl?.toString() ?: ""
            isGoogleSignedIn.value = true
            googleUserEmail.value = email
            googleUserName.value = name
            googleUserAvatar.value = avatar
            
            sharedPrefs.edit()
                .putBoolean("google_signed_in", true)
                .putString("google_user_email", email)
                .putString("google_user_name", name)
                .putString("google_user_avatar", avatar)
                .apply()
        }

        // Keep currentSyncPayload automatically up-to-date with any DB change reactively!
        viewModelScope.launch {
            combine(repository.allCores, repository.allTransactions) { cores, transactions ->
                cores to transactions
            }.collect { (cores, transactions) ->
                try {
                    val coresJson = JSONArray()
                    cores.forEach {
                        val obj = JSONObject().apply {
                            put("systemKey", it.systemKey)
                            put("name", it.name)
                            put("iconName", it.iconName)
                            put("isSystemProtected", it.isSystemProtected)
                        }
                        coresJson.put(obj)
                    }

                    val txJson = JSONArray()
                    transactions.forEach {
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
                    currentSyncPayload.value = backup.toString(2)
                } catch (e: Exception) {
                    currentSyncPayload.value = "{ \"error\": \"Failed to build: ${e.message}\" }"
                }
            }
        }
    }

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
    val loggingTimestamp = MutableStateFlow(System.currentTimeMillis())

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
        val tStamp = loggingTimestamp.value

        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    amount = amt,
                    currency = activeCurrency.value,
                    memo = if (memoText.isEmpty()) "UNNAMED TRANSACTION" else memoText,
                    categoryKey = catKey,
                    timestamp = tStamp
                )
            )
            // Reset entry form
            loggingAmountBuffer.value = "0"
            loggingMemo.value = ""
            loggingTimestamp.value = System.currentTimeMillis() // default back to current date
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
                val payload = buildSyncPayload()

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(payload.toByteArray())
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

    // Console logger helper
    fun logConsole(message: String) {
        val currentLogs = syncConsoleLogs.value.toMutableList()
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        currentLogs.add("[$timestamp] $message")
        syncConsoleLogs.value = currentLogs
        showStatus(message)
    }

    fun clearConsoleLogs() {
        syncConsoleLogs.value = emptyList()
    }

    // Google Sign-In & Sign-Out handlers
    fun handleGoogleSignInResult(account: GoogleSignInAccount) {
        val email = account.email ?: ""
        val name = account.displayName ?: "REGISTERED USER"
        val avatar = account.photoUrl?.toString() ?: ""
        
        isGoogleSignedIn.value = true
        googleUserEmail.value = email
        googleUserName.value = name
        googleUserAvatar.value = avatar
        
        sharedPrefs.edit()
            .putBoolean("google_signed_in", true)
            .putString("google_user_email", email)
            .putString("google_user_name", name)
            .putString("google_user_avatar", avatar)
            .apply()
        
        logConsole("CONNECTED: WELCOME ${name.uppercase()}")
        prepareCurrentPayload()
    }

    fun signOutGoogle(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val client = GoogleSignIn.getClient(context, gso)
        client.signOut().addOnCompleteListener {
            isGoogleSignedIn.value = false
            googleUserEmail.value = ""
            googleUserName.value = ""
            googleUserAvatar.value = ""
            cloudSyncStatus.value = "IDLE"
            currentSyncPayload.value = ""
            syncConsoleLogs.value = emptyList()
            
            sharedPrefs.edit()
                .putBoolean("google_signed_in", false)
                .putString("google_user_email", "")
                .putString("google_user_name", "")
                .putString("google_user_avatar", "")
                .apply()
            
            logConsole("DISCONNECTED FROM GOOGLE CORE")
        }
    }

    suspend fun buildSyncPayload(): String = withContext(Dispatchers.IO) {
        val cores = repository.allCores.first()
        val transactions = repository.allTransactions.first()

        val coresJson = JSONArray()
        cores.forEach {
            val obj = JSONObject().apply {
                put("systemKey", it.systemKey)
                put("name", it.name)
                put("iconName", it.iconName)
                put("isSystemProtected", it.isSystemProtected)
            }
            coresJson.put(obj)
        }

        val txJson = JSONArray()
        transactions.forEach {
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
        return@withContext backup.toString(2)
    }

    fun prepareCurrentPayload() {
        viewModelScope.launch {
            try {
                currentSyncPayload.value = buildSyncPayload()
            } catch (e: Exception) {
                currentSyncPayload.value = "{ \"error\": \"Failed to build: ${e.message}\" }"
            }
        }
    }

    // Access Token Acquisition
    private suspend fun fetchAccessToken(context: Context, email: String): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val scope = "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file"
                GoogleAuthUtil.getToken(context, email, scope)
            } catch (recoverable: UserRecoverableAuthException) {
                Log.w("LedgerViewModel", "UserRecoverableAuthException thrown", recoverable)
                val intent = recoverable.intent
                if (intent != null) {
                    _recoverableAuthIntent.emit(intent)
                } else {
                    logConsole("AUTH ERROR: Recoverable intent is null")
                }
                null
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Error acquiring access token", e)
                logConsole("AUTH ERROR: ${e.localizedMessage}")
                null
            }
        }
    }

    // Google Drive Sync - HTTP REST Helper
    private suspend fun searchBackupFile(accessToken: String): String? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/files")
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name = 'ledger_backup.json'")
            .addQueryParameter("fields", "files(id,name)")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e("LedgerViewModel", "Search failed with code ${response.code}: $errorBody")
                val cleanMsg = errorBody.ifEmpty { response.message ?: "Unknown" }
                throw Exception("Drive search error (${response.code}): $cleanMsg")
            }
            val bodyStr = response.body?.string() ?: return@withContext null
            val json = JSONObject(bodyStr)
            val files = json.optJSONArray("files")
            if (files != null && files.length() > 0) {
                return@withContext files.getJSONObject(0).getString("id")
            }
        }
        return@withContext null
    }

    private suspend fun uploadBackupFile(accessToken: String, fileId: String?, serializedData: String) = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val boundary = "ledger_backup_boundary"
        val isNew = fileId == null
        
        val metadata = if (isNew) {
            JSONObject().apply {
                put("name", "ledger_backup.json")
                put("parents", JSONArray().put("appDataFolder"))
            }.toString()
        } else {
            JSONObject().apply {
                put("name", "ledger_backup.json")
            }.toString()
        }

        val bodyBuilder = StringBuilder()
        bodyBuilder.append("--").append(boundary).append("\r\n")
        bodyBuilder.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        bodyBuilder.append(metadata).append("\r\n")
        bodyBuilder.append("--").append(boundary).append("\r\n")
        bodyBuilder.append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
        bodyBuilder.append(serializedData).append("\r\n")
        bodyBuilder.append("--").append(boundary).append("--\r\n")

        val url = if (isNew) {
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        } else {
            "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=multipart"
        }

        val mediaType = "multipart/related; boundary=$boundary".toMediaTypeOrNull()
        val requestBody = bodyBuilder.toString().toByteArray(Charsets.UTF_8).toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "multipart/related; boundary=$boundary")
            .method(if (isNew) "POST" else "PATCH", requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e("LedgerViewModel", "Upload failed with code ${response.code}: $errorBody")
                val cleanMsg = errorBody.ifEmpty { response.message ?: "Unknown" }
                throw Exception("Drive upload error (${response.code}): $cleanMsg")
            }
        }
    }

    private suspend fun downloadBackupFile(accessToken: String, fileId: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e("LedgerViewModel", "Download failed with code ${response.code}: $errorBody")
                val cleanMsg = errorBody.ifEmpty { response.message ?: "Unknown" }
                throw Exception("Drive download error (${response.code}): $cleanMsg")
            }
            return@withContext response.body?.string() ?: throw Exception("Empty response body")
        }
    }

    fun retryGoogleDriveAction() {
        val action = pendingActionAfterConsent
        pendingActionAfterConsent = null
        if (action == "BACKUP") {
            syncToGoogleDrive(getApplication())
        } else if (action == "RESTORE") {
            syncFromGoogleDrive(getApplication())
        }
    }

    // Google Drive Sync - Upload current ledger JSON
    fun syncToGoogleDrive(context: Context) {
        val email = googleUserEmail.value
        if (email.isEmpty()) {
            logConsole("ERR: GOOGLE ACCOUNT NOT CONNECTED")
            return
        }
        
        viewModelScope.launch {
            cloudSyncStatus.value = "SYNCING"
            logConsole("Starting Drive backup...")
            
            val token = fetchAccessToken(context, email)
            if (token == null) {
                pendingActionAfterConsent = "BACKUP"
                logConsole("AWAITING CONSENT / RESOLVING AUTH...")
                return@launch
            }
            
            try {
                logConsole("Auth token acquired.")
                val payload = buildSyncPayload()
                
                logConsole("Searching for existing backup file...")
                val fileId = searchBackupFile(token)
                if (fileId != null) {
                    logConsole("Existing backup found. Updating...")
                } else {
                    logConsole("No existing backup found. Creating new...")
                }
                
                uploadBackupFile(token, fileId, payload)
                
                logConsole("Drive backup completed successfully!")
                cloudSyncStatus.value = "SUCCESS"
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Backup failed", e)
                val errMsg = e.message ?: e.localizedMessage ?: e.toString()
                logConsole("BACKUP FAILED: $errMsg")
                cloudSyncStatus.value = "FAILED"
            }
        }
    }

    // Google Drive Sync - Download/Restore current ledger JSON
    fun syncFromGoogleDrive(context: Context) {
        val email = googleUserEmail.value
        if (email.isEmpty()) {
            logConsole("ERR: GOOGLE ACCOUNT NOT CONNECTED")
            return
        }
        
        viewModelScope.launch {
            cloudSyncStatus.value = "SYNCING"
            logConsole("Starting Drive restore...")
            
            val token = fetchAccessToken(context, email)
            if (token == null) {
                pendingActionAfterConsent = "RESTORE"
                logConsole("AWAITING CONSENT / RESOLVING AUTH...")
                return@launch
            }
            
            try {
                logConsole("Auth token acquired.")
                logConsole("Searching for backup file...")
                val fileId = searchBackupFile(token)
                if (fileId == null) {
                    logConsole("RESTORE ERROR: No backup file found in AppDataFolder.")
                    cloudSyncStatus.value = "FAILED"
                    return@launch
                }
                
                logConsole("Backup file found. Downloading...")
                val jsonStr = downloadBackupFile(token, fileId)
                
                logConsole("Data downloaded. Parsing...")
                val backup = JSONObject(jsonStr)
                if (!backup.has("system") || backup.getString("system") != "LEDGER.SYS") {
                    logConsole("RESTORE ERROR: Invalid backup header format.")
                    cloudSyncStatus.value = "FAILED"
                    return@launch
                }
                
                currentSyncPayload.value = backup.toString(2)
                
                // Import cores
                logConsole("Importing systems & category cores...")
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

                // Import transactions
                logConsole("Importing transaction logs...")
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

                logConsole("Restore completed successfully! System synchronized.")
                cloudSyncStatus.value = "SUCCESS"
            } catch (e: Exception) {
                Log.e("LedgerViewModel", "Restore failed", e)
                val errMsg = e.message ?: e.localizedMessage ?: e.toString()
                logConsole("RESTORE FAILED: $errMsg")
                cloudSyncStatus.value = "FAILED"
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
