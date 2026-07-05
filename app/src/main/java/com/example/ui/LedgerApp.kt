package com.example.ui

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryCore
import com.example.data.Transaction
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun getCategoryIcon(name: String): ImageVector = when (name.lowercase()) {
    "restaurant", "food" -> Icons.Default.Restaurant
    "flight", "travel" -> Icons.Default.Flight
    "receipt", "bills" -> Icons.Default.Receipt
    "sports_esports", "rec" -> Icons.Default.SportsEsports
    "shopping_cart", "shop" -> Icons.Default.ShoppingCart
    "link" -> Icons.Default.Link
    "document" -> Icons.Default.Description
    "cart" -> Icons.Default.ShoppingCart
    "star" -> Icons.Default.Star
    "heart" -> Icons.Default.Favorite
    "globe" -> Icons.Default.Public
    "wrench" -> Icons.Default.Build
    else -> Icons.Default.HelpOutline
}

@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    thickness: Dp = 1.dp
) {
    val density = LocalDensity.current
    val strokeWidth = with(density) { thickness.toPx() }
    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(thickness)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
        )
    }
}

@Composable
fun DottedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    thickness: Dp = 1.dp
) {
    val density = LocalDensity.current
    val strokeWidth = with(density) { thickness.toPx() }
    Canvas(modifier = modifier
        .fillMaxHeight()
        .width(thickness)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(0f, size.height),
            strokeWidth = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
        )
    }
}

@Composable
fun RetroPanel(
    modifier: Modifier = Modifier,
    shadowOffset: Dp = 4.dp,
    borderColor: Color = MaterialTheme.colorScheme.onBackground,
    borderWidth: Dp = 2.dp,
    shadowColor: Color = MaterialTheme.colorScheme.onBackground,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentPadding: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset)) {
        // Black shadow background offset
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor)
        )
        // Content box with border and background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .border(borderWidth, borderColor)
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
fun RetroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isRed: Boolean = false,
    text: String
) {
    val onColor = MaterialTheme.colorScheme.onBackground
    val backColor = if (isRed) RetroRed else MaterialTheme.colorScheme.background
    val textColor = if (isRed) RetroWhite else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(onColor)
        )
        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backColor)
                .border(2.dp, onColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LedgerApp(viewModel: LedgerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    // Automatically dismiss status message after delay
    LaunchedEffect(statusMsg) {
        if (statusMsg.isNotEmpty()) {
            delay(4000)
            viewModel.clearStatus()
        }
    }

    MyApplicationTheme(darkTheme = isDark) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                SystemHeader(viewModel)
            },
            bottomBar = {
                SystemFooter(viewModel, currentScreen)
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Status messages banner
                    AnimatedVisibility(visible = statusMsg.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.error)
                                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusMsg,
                                color = MaterialTheme.colorScheme.onError,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.clearStatus() }
                            )
                        }
                    }

                    // Active screen selection
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        when (currentScreen) {
                            Screen.LOG -> LogScreen(viewModel)
                            Screen.LEDGER -> LedgerScreen(viewModel)
                            Screen.ANALYSE -> AnalyseScreen(viewModel)
                            Screen.CONFIG -> ConfigScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemHeader(viewModel: LedgerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(top = 24.dp) // increased margin below status bar
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[$] LEDGER.SYS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Local only status capsule
            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "[○ LOCAL ONLY]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        DashedDivider()
    }
}

@Composable
fun SystemFooter(viewModel: LedgerViewModel, activeScreen: Screen) {
    val footerTabs = listOf(
        Screen.LOG to "[+] LOG",
        Screen.LEDGER to "[*] LEDGER",
        Screen.ANALYSE to "[~] ANALYSE",
        Screen.CONFIG to "[#] CONFIG"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        DashedDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            footerTabs.forEachIndexed { index, (screen, label) ->
                val isActive = activeScreen == screen
                val tabWeight = 1f

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(tabWeight)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.background
                        )
                        .clickable { viewModel.currentScreen.value = screen }
                        .testTag("nav_${screen.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) MaterialTheme.colorScheme.background
                        else MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (index < footerTabs.size - 1) {
                    DottedDivider(modifier = Modifier.fillMaxHeight(0.6f))
                }
            }
        }
    }
}

@Composable
fun LogScreen(viewModel: LedgerViewModel) {
    val amount by viewModel.loggingAmountBuffer.collectAsState()
    val memo by viewModel.loggingMemo.collectAsState()
    val selectedCategoryKey by viewModel.selectedLoggingCategory.collectAsState()
    val cores by viewModel.allCores.collectAsState()
    val currency by viewModel.activeCurrency.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    val activeCategory = cores.find { it.systemKey == selectedCategoryKey }
    val activeCategoryName = activeCategory?.name ?: "UNKNOWN"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Display Banner
        RetroPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[$ REGISTER]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "MODULE: [$activeCategoryName]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                DashedDivider(modifier = Modifier.padding(vertical = 4.dp))
                // Large digital numeric readout
                val displayAmount = if (amount == "0" || amount.isEmpty()) "0.00" else amount
                Text(
                    text = "$currency$displayAmount",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // [MEMO / TRANSACTION NOTE] Input
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "MEMO / TRANSACTION NOTE:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            BasicTextField(
                value = memo,
                onValueChange = { viewModel.loggingMemo.value = it },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, MaterialTheme.colorScheme.onBackground)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(12.dp)
                    ) {
                        if (memo.isEmpty()) {
                            Text(
                                text = "e.g. Organic sourdough bread, Subway ride...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("memo_input")
            )
        }

        // [SELECT EXPENSE CATEGORY] Horizontal Carousel
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "SELECT EXPENSE CATEGORY:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cores.forEach { core ->
                    val isSelected = core.systemKey == selectedCategoryKey
                    Box(
                        modifier = Modifier
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.background
                            )
                            .clickable { viewModel.selectedLoggingCategory.value = core.systemKey }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("category_tab_${core.systemKey}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(core.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.background
                                else MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "[${core.name}]",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.background
                                else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        // Numeric Keypad Matrix
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val keys = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('C', '0', '.')
            )

            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isClear = key == 'C'
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .border(
                                    2.dp,
                                    if (isClear) RetroRed else MaterialTheme.colorScheme.onBackground
                                )
                                .background(MaterialTheme.colorScheme.background)
                                .clickable { viewModel.handleKeypadPress(key) }
                                .testTag("keypad_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isClear) RetroRed else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        // Execution Command
        RetroButton(
            onClick = {
                keyboardController?.hide()
                viewModel.commitTransaction()
            },
            text = "[COMMIT TRANSACTION]",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("commit_button")
        )
    }
}

@Composable
fun LedgerScreen(viewModel: LedgerViewModel) {
    val todayOutflow by viewModel.todayOutflow.collectAsState()
    val monthOutflow by viewModel.monthOutflow.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val cores by viewModel.allCores.collectAsState()
    val currency by viewModel.activeCurrency.collectAsState()
    val searchQuery by viewModel.ledgerSearchQuery.collectAsState()
    val selectedFilter by viewModel.ledgerCategoryFilter.collectAsState()

    var showCoreDropdown by remember { mutableStateOf(false) }
    var selectedTxToEdit by remember { mutableStateOf<Transaction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Aggregate Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // TODAY OUTFLOW
            RetroPanel(
                modifier = Modifier.weight(1f),
                shadowOffset = 3.dp,
                contentPadding = 8.dp
            ) {
                Column {
                    Text(
                        text = "TODAY OUTFLOW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%s%.2f", currency, todayOutflow),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // MONTH TOTAL
            RetroPanel(
                modifier = Modifier.weight(1f),
                shadowOffset = 3.dp,
                contentPadding = 8.dp
            ) {
                Column {
                    Text(
                        text = "MONTH TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%s%.2f", currency, monthOutflow),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Section Header (Reset button removed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRANSACTION LEDGER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Filter Matrix (Search bar and Core selection)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input
            BasicTextField(
                value = searchQuery,
                onValueChange = { viewModel.ledgerSearchQuery.value = it },
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp)
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search notes...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("ledger_search_input")
            )

            // CORE DROPDOWN
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { showCoreDropdown = true }
                    .padding(8.dp)
                    .testTag("ledger_core_filter_dropdown"),
                contentAlignment = Alignment.Center
            ) {
                val filterName = if (selectedFilter == "ALL CORES") "ALL CORES" else {
                    cores.find { it.systemKey == selectedFilter }?.name ?: selectedFilter
                }
                Text(
                    text = "[$filterName]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                DropdownMenu(
                    expanded = showCoreDropdown,
                    onDismissRequest = { showCoreDropdown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.background).border(1.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    DropdownMenuItem(
                        text = { Text("ALL CORES", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground) },
                        onClick = {
                            viewModel.ledgerCategoryFilter.value = "ALL CORES"
                            showCoreDropdown = false
                        }
                    )
                    cores.forEach { core ->
                        DropdownMenuItem(
                            text = { Text(core.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground) },
                            onClick = {
                                viewModel.ledgerCategoryFilter.value = core.systemKey
                                showCoreDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Data Table Workspace
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, MaterialTheme.colorScheme.onBackground)
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp)
        ) {
            if (transactions.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NO LOGS REGISTERED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        DashedDivider(modifier = Modifier.width(180.dp))
                        Text(
                            text = "system state: ready.\nwaiting for entries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // List of logs
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { tx ->
                        val cat = cores.find { it.systemKey == tx.categoryKey }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                                .background(MaterialTheme.colorScheme.background)
                                .clickable { selectedTxToEdit = tx }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat?.iconName ?: "help"),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = cat?.name ?: "UNKNOWN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(tx.timestamp))
                                    Text(
                                        text = "($dateStr)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                                Text(
                                    text = tx.memo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "%s%.2f", tx.currency, tx.amount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Dialog for Editing / Deleting a specific register record
    if (selectedTxToEdit != null) {
        val editingTx = selectedTxToEdit!!
        var editAmount by remember(editingTx) { mutableStateOf(editingTx.amount.toString()) }
        var editMemo by remember(editingTx) { mutableStateOf(editingTx.memo) }
        var editCategoryKey by remember(editingTx) { mutableStateOf(editingTx.categoryKey) }
        var showCatDropdown by remember { mutableStateOf(false) }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedTxToEdit = null }
        ) {
            RetroPanel(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shadowOffset = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EDIT TRANSACTION REGISTER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { selectedTxToEdit = null }
                        )
                    }

                    DashedDivider()

                    // Amount input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "AMOUNT ($currency):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        BasicTextField(
                            value = editAmount,
                            onValueChange = { editAmount = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            decorationBox = { inner ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(8.dp)
                                ) {
                                    inner()
                                }
                            },
                            modifier = Modifier.testTag("edit_amount_input")
                        )
                    }

                    // Category dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CATEGORY:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                .background(MaterialTheme.colorScheme.background)
                                .clickable { showCatDropdown = true }
                                .padding(8.dp)
                                .testTag("edit_category_dropdown"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val catName = cores.find { it.systemKey == editCategoryKey }?.name ?: editCategoryKey
                            Text(
                                text = "[$catName]",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )

                            DropdownMenu(
                                expanded = showCatDropdown,
                                onDismissRequest = { showCatDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.background).border(1.dp, MaterialTheme.colorScheme.onBackground)
                            ) {
                                cores.forEach { core ->
                                    DropdownMenuItem(
                                        text = { Text(core.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground) },
                                        onClick = {
                                            editCategoryKey = core.systemKey
                                            showCatDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Memo input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MEMO:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        BasicTextField(
                            value = editMemo,
                            onValueChange = { editMemo = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            decorationBox = { inner ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(8.dp)
                                ) {
                                    inner()
                                }
                            },
                            modifier = Modifier.testTag("edit_memo_input")
                        )
                    }

                    DashedDivider()

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Delete (Red Button)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(2.dp, RetroRed)
                                .background(MaterialTheme.colorScheme.background)
                                .clickable {
                                    viewModel.deleteTransaction(editingTx.id)
                                    selectedTxToEdit = null
                                }
                                .padding(vertical = 10.dp)
                                .testTag("dialog_delete_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "[DELETE RECORD]",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = RetroRed
                            )
                        }

                        // Update (Filled black/white button)
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .border(2.dp, MaterialTheme.colorScheme.onBackground)
                                .background(MaterialTheme.colorScheme.onBackground)
                                .clickable {
                                    val finalAmt = editAmount.toDoubleOrNull() ?: editingTx.amount
                                    viewModel.updateTransaction(
                                        id = editingTx.id,
                                        amount = finalAmt,
                                        memo = editMemo,
                                        categoryKey = editCategoryKey,
                                        timestamp = editingTx.timestamp
                                    )
                                    selectedTxToEdit = null
                                }
                                .padding(vertical = 10.dp)
                                .testTag("dialog_update_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "[UPDATE REGISTER]",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyseScreen(viewModel: LedgerViewModel) {
    val interval by viewModel.analyticsInterval.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val cores by viewModel.allCores.collectAsState()
    val currency by viewModel.activeCurrency.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Granularity Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val intervals = listOf("DAILY", "WEEKLY", "MONTHLY")
            intervals.forEach { i ->
                val isActive = interval == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, MaterialTheme.colorScheme.onBackground)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.background
                        )
                        .clickable { viewModel.analyticsInterval.value = i }
                        .padding(vertical = 8.dp)
                        .testTag("granularity_$i"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[$i]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.background
                        else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // [1-BIT TIMELINE BAR GRAPH] Panel
        RetroPanel(
            modifier = Modifier.fillMaxWidth(),
            shadowOffset = 4.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "1-BIT TIMELINE BAR GRAPH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                DashedDivider()

                // Calculate dynamic aggregation values and labels based on interval
                val labels = ArrayList<String>()
                val totals = FloatArray(7)

                when (interval) {
                    "DAILY" -> {
                        for (i in 6 downTo 0) {
                            val dayCal = Calendar.getInstance()
                            dayCal.add(Calendar.DAY_OF_YEAR, -i)
                            val dateStr = SimpleDateFormat("MM/dd", Locale.US).format(dayCal.time)
                            labels.add(dateStr)

                            val startOfDay = Calendar.getInstance().apply {
                                timeInMillis = dayCal.timeInMillis
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                            val endOfDay = Calendar.getInstance().apply {
                                timeInMillis = dayCal.timeInMillis
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis

                            totals[6 - i] = transactions.filter { it.timestamp in startOfDay..endOfDay }.sumOf { it.amount }.toFloat()
                        }
                    }
                    "WEEKLY" -> {
                        for (i in 6 downTo 0) {
                            val weekCal = Calendar.getInstance()
                            weekCal.add(Calendar.WEEK_OF_YEAR, -i)
                            val weekNum = weekCal.get(Calendar.WEEK_OF_YEAR)
                            labels.add("W$weekNum")

                            val startOfWeek = Calendar.getInstance().apply {
                                timeInMillis = weekCal.timeInMillis
                                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                            val endOfWeek = Calendar.getInstance().apply {
                                timeInMillis = weekCal.timeInMillis
                                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis

                            totals[6 - i] = transactions.filter { it.timestamp in startOfWeek..endOfWeek }.sumOf { it.amount }.toFloat()
                        }
                    }
                    "MONTHLY" -> {
                        for (i in 6 downTo 0) {
                            val monthCal = Calendar.getInstance()
                            monthCal.add(Calendar.MONTH, -i)
                            val monthName = SimpleDateFormat("MMM", Locale.US).format(monthCal.time)
                            labels.add(monthName)

                            val startOfMonth = Calendar.getInstance().apply {
                                timeInMillis = monthCal.timeInMillis
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                            val endOfMonth = Calendar.getInstance().apply {
                                timeInMillis = monthCal.timeInMillis
                                set(Calendar.DAY_OF_MONTH, monthCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis

                            totals[6 - i] = transactions.filter { it.timestamp in startOfMonth..endOfMonth }.sumOf { it.amount }.toFloat()
                        }
                    }
                }

                val maxAmount = totals.maxOrNull() ?: 0f
                val finalMax = if (maxAmount == 0f) 100f else maxAmount

                // Timeline Plot Canvas
                val gridColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                val barColor = MaterialTheme.colorScheme.onBackground
                val textOnColor = MaterialTheme.colorScheme.onBackground

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val paddingLeft = 40f
                        val paddingBottom = 40f
                        val plotWidth = canvasWidth - paddingLeft
                        val plotHeight = canvasHeight - paddingBottom

                        // Draw Axes
                        drawLine(
                            color = textOnColor,
                            start = Offset(paddingLeft, 0f),
                            end = Offset(paddingLeft, plotHeight),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = textOnColor,
                            start = Offset(paddingLeft, plotHeight),
                            end = Offset(canvasWidth, plotHeight),
                            strokeWidth = 2f
                        )

                        // Draw horizontal grid guidelines
                        val divisions = 4
                        for (i in 0..divisions) {
                            val y = (plotHeight / divisions) * i
                            drawLine(
                                color = gridColor,
                                start = Offset(paddingLeft, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // Plot Bars
                        val barSpacing = plotWidth / 7
                        for (i in 0..6) {
                            val barVal = totals[i]
                            val barHeightRatio = barVal / finalMax
                            val barHeight = plotHeight * barHeightRatio

                            val leftX = paddingLeft + (i * barSpacing) + (barSpacing * 0.2f)
                            val rightX = paddingLeft + (i * barSpacing) + (barSpacing * 0.8f)
                            val topY = plotHeight - barHeight

                            // Draw hard solid retro bar if amount > 0
                            if (barVal > 0) {
                                drawRect(
                                    color = barColor,
                                    topLeft = Offset(leftX, topY),
                                    size = androidx.compose.ui.geometry.Size(rightX - leftX, barHeight)
                                )
                            }
                        }
                    }

                    // Add text overlays for days and grid labels using native Composable positions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp), // offset by y-axis padding
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        labels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Max Indicator
                    Text(
                        text = String.format(Locale.US, "%s%.0f", currency, finalMax),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(2.dp)
                    )
                }
            }
        }

        // [CATEGORY APPORTIONMENT METER] Panel
        RetroPanel(
            modifier = Modifier.fillMaxWidth(),
            shadowOffset = 4.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CATEGORY APPORTIONMENT METER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                DashedDivider()

                if (transactions.isEmpty()) {
                    Text(
                        text = "[NO STATS REGISTERED YET]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    // Aggregate category amounts
                    val categorySums = mutableMapOf<String, Double>()
                    transactions.forEach { tx ->
                        categorySums[tx.categoryKey] = (categorySums[tx.categoryKey] ?: 0.0) + tx.amount
                    }
                    val grandTotal = categorySums.values.sum()

                    categorySums.forEach { (catKey, sum) ->
                        val core = cores.find { it.systemKey == catKey }
                        val name = core?.name ?: catKey.uppercase()
                        val percentage = (sum / grandTotal * 100).toInt()

                        // ASCII meter string representation e.g. [████░░░░░░]
                        val filledBlocks = percentage / 10
                        val asciiMeter = StringBuilder("[")
                        for (i in 0 until 10) {
                            if (i < filledBlocks) asciiMeter.append("█") else asciiMeter.append("░")
                        }
                        asciiMeter.append("]")

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(core?.iconName ?: "help"),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Text(
                                    text = String.format(Locale.US, "%s%.2f (%d%%)", currency, sum, percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Output the gorgeous ASCII meter
                            Text(
                                text = asciiMeter.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigScreen(viewModel: LedgerViewModel) {
    val context = LocalContext.current
    val isDark by viewModel.isDarkTheme.collectAsState()
    val activeCurrencySymbol by viewModel.activeCurrency.collectAsState()
    val isReminderEnabled by viewModel.isReminderEnabled.collectAsState()
    val reminderTimeStr by viewModel.reminderTime.collectAsState()
    val cores by viewModel.allCores.collectAsState()

    // Google Sync states
    val isGoogleSignedIn by viewModel.isGoogleSignedIn.collectAsState()
    val googleEmail by viewModel.googleUserEmail.collectAsState()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()
    val customCurrencySymbol by viewModel.customCurrencySymbol.collectAsState()

    // Add Core states
    val newName by viewModel.newCoreName.collectAsState()
    val selectedIcon by viewModel.newCoreIcon.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val availableIcons = listOf("link", "document", "cart", "star", "heart", "globe", "wrench")

    // File Interops
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.writeBackupToUri(context, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.readBackupFromUri(context, it) }
    }

    // Time Picker
    val timeParts = reminderTimeStr.split(":")
    val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 20
    val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            viewModel.setReminderTimeValue(String.format(Locale.US, "%02d:%02d", hourOfDay, minute))
        },
        initialHour,
        initialMinute,
        true
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // [SYSTEM THEME CONTROLLER]
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "SYSTEM THEME CONTROLLER:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            RetroButton(
                onClick = { viewModel.toggleTheme() },
                text = if (isDark) "SWAP TO LIGHT MONOCHROME" else "SWAP TO DARK MONOCHROME",
                modifier = Modifier.fillMaxWidth().testTag("swap_theme_button")
            )
        }

        DashedDivider()

        // [ACTIVE CURRENCY CORES]
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "ACTIVE CURRENCY CORES:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val currencies = listOf("$", "€", "£", "¥", "OTHER")
                currencies.forEach { curr ->
                    val isStandardSymbol = activeCurrencySymbol in listOf("$", "€", "£", "¥")
                    val isActive = if (curr == "OTHER") !isStandardSymbol else activeCurrencySymbol == curr
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, MaterialTheme.colorScheme.onBackground)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.background
                            )
                            .clickable {
                                if (curr == "OTHER") {
                                    if (customCurrencySymbol.isEmpty()) {
                                        viewModel.setCustomCurrencySymbol("¤")
                                        viewModel.setCurrency("¤")
                                    } else {
                                        viewModel.setCurrency(customCurrencySymbol)
                                    }
                                } else {
                                    viewModel.setCurrency(curr)
                                }
                            }
                            .padding(vertical = 10.dp)
                            .testTag("currency_tab_$curr"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[$curr]",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Custom Currency Input
            if (activeCurrencySymbol !in listOf("$", "€", "£", "¥")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CUSTOM SYMBOL:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    BasicTextField(
                        value = customCurrencySymbol,
                        onValueChange = {
                            viewModel.setCustomCurrencySymbol(it)
                            viewModel.setCurrency(it)
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 6.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                innerTextField()
                            }
                        },
                        modifier = Modifier.testTag("custom_currency_input")
                    )
                }
            }
        }

        DashedDivider()

        // [DAILY PWA REMINDER CONTROLLER]
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DAILY PWA REMINDER CONTROLLER:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOGGING REMINDER:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Enabled/disabled switch
                Box(
                    modifier = Modifier
                        .border(2.dp, MaterialTheme.colorScheme.onBackground)
                        .background(
                            if (isReminderEnabled) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.background
                        )
                        .clickable { viewModel.toggleReminder() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("reminder_toggle_button")
                ) {
                    Text(
                        text = if (isReminderEnabled) "[ENABLED]" else "[DISABLED]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isReminderEnabled) MaterialTheme.colorScheme.background
                        else MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REMINDER TIME (24H):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )

                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .background(MaterialTheme.colorScheme.background)
                            .clickable { timePickerDialog.show() }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .testTag("reminder_time_select"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reminderTimeStr,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        DashedDivider()

        // [CLOUD SYNCHRONIZATION CONTROLLER]
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CLOUD SYNCHRONIZATION CONTROLLER:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (!isGoogleSignedIn) {
                Text(
                    text = "Sync your ledger via Google Drive. Note: You must register this app's SHA-1 and Package Name in the Google Cloud Console to enable this feature.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                RetroButton(
                    onClick = { viewModel.signInWithGoogle(context) },
                    text = "[AUTHORIZE GOOGLE DRIVE]",
                    modifier = Modifier.fillMaxWidth().testTag("connect_google_button")
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONNECTED AS:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = googleEmail.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "STATUS: $cloudSyncStatus",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cloudSyncStatus == "SUCCESS") Color(0xFF008800) else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .border(1.dp, RetroRed)
                            .clickable { viewModel.signOutGoogle(context) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DISCONNECT",
                            style = MaterialTheme.typography.labelSmall,
                            color = RetroRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RetroButton(
                        onClick = { viewModel.syncToGoogleDrive() },
                        text = "SYNC TO DRIVE",
                        modifier = Modifier.weight(1f)
                    )
                    RetroButton(
                        onClick = { viewModel.syncFromGoogleDrive() },
                        text = "RESTORE FROM DRIVE",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        DashedDivider()

        // [CORES REGISTRY DICTIONARY]
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CORES REGISTRY DICTIONARY:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Custom Registration field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = newName,
                    onValueChange = { viewModel.newCoreName.value = it },
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(10.dp)
                        ) {
                            if (newName.isEmpty()) {
                                Text(
                                    text = "E.G. TAXES, SUBS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("new_core_name_input")
                )

                // ADD CORE button
                Box(
                    modifier = Modifier
                        .border(2.dp, MaterialTheme.colorScheme.onBackground)
                        .background(MaterialTheme.colorScheme.onBackground)
                        .clickable {
                            keyboardController?.hide()
                            viewModel.registerCustomCore()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("add_core_submit_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[ADD CORE]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.background
                    )
                }
            }

            // Asset selector row
            Text(
                text = "SELECT ICON ASSET FOR NEW CORE:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableIcons.forEach { iconName ->
                    val isSel = selectedIcon == iconName
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(
                                1.dp,
                                if (isSel) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                            .background(
                                if (isSel) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.background
                            )
                            .clickable { viewModel.newCoreIcon.value = iconName }
                            .testTag("asset_icon_$iconName"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(iconName),
                            contentDescription = iconName,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSel) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dictionary List Tree display
            Text(
                text = "ACTIVE MODULES TREE:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cores.forEach { core ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(core.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                            Column {
                                Text(
                                    text = core.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "(${core.systemKey})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }

                        if (core.isSystemProtected) {
                            Text(
                                text = "[SYSTEM_PROTECTED]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            // Custom core can be deleted
                            Box(
                                modifier = Modifier
                                    .border(1.dp, RetroRed)
                                    .background(MaterialTheme.colorScheme.background)
                                    .clickable { viewModel.deleteCustomCore(core.systemKey) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("delete_core_${core.systemKey}")
                            ) {
                                Text(
                                    text = "[DELETE]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RetroRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        DashedDivider()

        // [DATA INTERCHANGE PORTABILITY]
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "DATA INTERCHANGE PORTABILITY:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // EXPORT BACKUP
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { exportLauncher.launch("ledger_backup_${System.currentTimeMillis()}.json") }
                        .testTag("export_backup_button")
                ) {
                    RetroPanel(
                        shadowOffset = 3.dp,
                        contentPadding = 8.dp
                    ) {
                        Text(
                            text = "SAVE TO FILE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // IMPORT BACKUP
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { importLauncher.launch(arrayOf("application/json")) }
                        .testTag("import_backup_button")
                ) {
                    RetroPanel(
                        shadowOffset = 3.dp,
                        contentPadding = 8.dp
                    ) {
                        Text(
                            text = "LOAD FROM FILE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
