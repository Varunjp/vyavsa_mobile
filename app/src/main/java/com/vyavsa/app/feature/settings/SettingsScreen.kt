package com.vyavsa.app.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.CompanyLogoImage
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.VyavsaSwitchRow
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.printer.BluetoothPermissionHelper
import com.vyavsa.app.core.printer.InvoiceContentSettings
import com.vyavsa.app.core.printer.InvoiceTheme
import com.vyavsa.app.core.printer.PaperWidth
import com.vyavsa.app.core.printer.PrintJobStatus
import com.vyavsa.app.core.printer.PrintTypography
import com.vyavsa.app.core.printer.PrinterConnectionType
import com.vyavsa.app.core.printer.PrinterDevice
import com.vyavsa.app.core.settings.AppFontSize
import com.vyavsa.app.core.settings.AppLanguage
import com.vyavsa.app.core.settings.QuickActionItem
import com.vyavsa.app.core.settings.ThemeMode
import com.vyavsa.app.feature.profile.EditInvoiceFooterDialog

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            viewModel.refreshPrinters()
            viewModel.runTestPrint()
        } else {
            Toast.makeText(context, "Bluetooth permissions are required for printing.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Settings",
                subtitle = "App preferences & printing setup",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("General", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Invoice & Print", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Printer Setup", fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTab) {
                0 -> GeneralSettingsTab(viewModel = viewModel)
                1 -> InvoicePrintingSettingsTab(viewModel = viewModel)
                2 -> PrinterConfigurationTab(
                    viewModel = viewModel,
                    onRequestPermissions = { perms -> permissionLauncher.launch(perms) }
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("This will reset your general preferences, font scale, dock order, and invoice theme settings to system defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllSettingsToDefaults()
                        showResetDialog = false
                        Toast.makeText(context, "Settings restored to defaults", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================================================================
// TAB 1: GENERAL SETTINGS
// ============================================================================

@Composable
private fun GeneralSettingsTab(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val language by viewModel.language.collectAsState()
    val enabledQuickActions by viewModel.enabledQuickActions.collectAsState()
    val dockOrder = viewModel.getEffectiveDockOrder()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Mode Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Appearance & Color Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            val isSelected = themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setThemeMode(mode) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            ThemeMode.SYSTEM -> Icons.Default.Settings
                                            ThemeMode.LIGHT -> Icons.Default.LightMode
                                            ThemeMode.DARK -> Icons.Default.DarkMode
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Font Size Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Font Scale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Adjust typography size throughout the application for easier reading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFontSize.values().forEach { size ->
                            val isSelected = fontSize == size
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setFontSize(size) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = when (size) {
                                            AppFontSize.SMALL -> "90%"
                                            AppFontSize.MEDIUM -> "100%"
                                            AppFontSize.LARGE -> "115%"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = size.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // App Language Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AppLanguage.values().forEach { lang ->
                        val isSelected = language == lang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setLanguage(lang) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(lang.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Text(lang.nativeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setLanguage(lang) }
                            )
                        }
                        if (lang != AppLanguage.values().last()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        // Quick Actions Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dashboard Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Choose shortcuts displayed on your main dashboard.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    QuickActionItem.values().forEach { action ->
                        val isEnabled = enabledQuickActions.contains(action.id)
                        VyavsaSwitchRow(
                            title = action.title,
                            subtitle = action.description,
                            checked = isEnabled,
                            onCheckedChange = { viewModel.toggleQuickAction(action.id, it) }
                        )
                    }
                }
            }
        }

        // Dock Customization Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dock Customization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Reorder bottom navigation tabs to fit your workflow.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { viewModel.resetDockOrder() }) {
                            Text("Reset")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    dockOrder.forEachIndexed { index, destination ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(destination.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Row {
                                IconButton(
                                    onClick = { viewModel.moveDockItemUp(index) },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.moveDockItemDown(index) },
                                    enabled = index < dockOrder.size - 1
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: INVOICE & PRINTING SETTINGS
// ============================================================================

@Composable
private fun InvoicePrintingSettingsTab(viewModel: SettingsViewModel) {
    val invoiceTheme by viewModel.invoiceTheme.collectAsState()
    val paperWidth by viewModel.paperWidth.collectAsState()
    val typography by viewModel.typography.collectAsState()
    val contentSettings by viewModel.contentSettings.collectAsState()
    val previewText = viewModel.getReceiptPreviewText()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Invoice Theme Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Invoice Themes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Select from 3 professionally styled layout templates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    InvoiceTheme.values().forEach { theme ->
                        val isSelected = invoiceTheme == theme
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.setInvoiceTheme(theme) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setInvoiceTheme(theme) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(theme.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(theme.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Paper Size Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paper Roll Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Format receipts precisely for your printer's physical width.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaperWidth.values().forEach { width ->
                            val isSelected = paperWidth == width
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setPaperWidth(width) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${width.widthMm} mm",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${width.charsPerLine} cols",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Print Typography Density
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Print Typography Density", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Adjust font sizing and line density on physical receipts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrintTypography.values().forEach { typo ->
                            val isSelected = typography == typo
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setTypography(typo) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = typo.label.split(" ").first(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${(typo.scaleFactor * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Invoice Footer & Closing Terms Card
        item {
            val companyProfile by viewModel.companyProfile.collectAsState()
            val footerHeading = companyProfile?.invoiceFooterHeading?.ifBlank { null } ?: "Thank you for your business!"
            val footerParagraph = companyProfile?.invoiceFooterParagraph?.ifBlank { null } ?: "Powered by Vyavsa Small Business Solutions"
            var showEditFooterDialog by remember { mutableStateOf(false) }
            val footerUpdateState by viewModel.footerUpdateState.collectAsState()

            LaunchedEffect(footerUpdateState) {
                if (footerUpdateState is UiState.Success) {
                    showEditFooterDialog = false
                    viewModel.resetFooterUpdateState()
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Invoice Footer & Terms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Closing greeting & declaration printed at bottom of bills.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { showEditFooterDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Invoice Footer", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(footerHeading, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(footerParagraph, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (showEditFooterDialog) {
                EditInvoiceFooterDialog(
                    initialHeading = companyProfile?.invoiceFooterHeading ?: "",
                    initialParagraph = companyProfile?.invoiceFooterParagraph ?: "",
                    isSaving = footerUpdateState is UiState.Loading,
                    errorMessage = (footerUpdateState as? UiState.Error)?.message,
                    onDismiss = {
                        showEditFooterDialog = false
                        viewModel.resetFooterUpdateState()
                    },
                    onConfirm = { heading, paragraph ->
                        viewModel.updateInvoiceFooter(heading, paragraph)
                    }
                )
            }
        }

        // Content Visibility Switches
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Invoice Content Toggles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Show or hide optional fields on printed bills and share previews.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    VyavsaSwitchRow(
                        title = "Business Address",
                        subtitle = "Show company street address & city on receipts",
                        checked = contentSettings.showBusinessAddress,
                        onCheckedChange = { viewModel.toggleShowBusinessAddress(it) }
                    )
                    VyavsaSwitchRow(
                        title = "Business Phone Number",
                        subtitle = "Include primary contact number for customer support",
                        checked = contentSettings.showBusinessPhone,
                        onCheckedChange = { viewModel.toggleShowBusinessPhone(it) }
                    )
                    VyavsaSwitchRow(
                        title = "Business Email",
                        subtitle = "Include official business email address",
                        checked = contentSettings.showBusinessEmail,
                        onCheckedChange = { viewModel.toggleShowBusinessEmail(it) }
                    )
                    VyavsaSwitchRow(
                        title = "GSTIN / Tax ID Box",
                        subtitle = "Display tax registration identification",
                        checked = contentSettings.showTaxId,
                        onCheckedChange = { viewModel.toggleShowTaxId(it) }
                    )
                    VyavsaSwitchRow(
                        title = "Customer Details",
                        subtitle = "Show customer name, phone, and delivery route",
                        checked = contentSettings.showCustomerDetails,
                        onCheckedChange = { viewModel.toggleShowCustomerDetails(it) }
                    )
                    VyavsaSwitchRow(
                        title = "Payment Mode & Summary",
                        subtitle = "Display payment terms and settlement mode",
                        checked = contentSettings.showPaymentInfo,
                        onCheckedChange = { viewModel.toggleShowPaymentInfo(it) }
                    )
                    VyavsaSwitchRow(
                        title = "Invoice Notes & Remarks",
                        subtitle = "Show transaction remarks or special instructions",
                        checked = contentSettings.showNotes,
                        onCheckedChange = { viewModel.toggleShowNotes(it) }
                    )
                    VyavsaSwitchRow(
                        title = "Footer Note & Declaration",
                        subtitle = "Print thank you greeting and terms & conditions at bottom",
                        checked = contentSettings.showFooterNote,
                        onCheckedChange = { viewModel.toggleShowFooterNote(it) }
                    )
                    VyavsaSwitchRow(
                        title = "Company Logo",
                        subtitle = "Render graphic business logo at top of receipts",
                        checked = contentSettings.showLogo,
                        onCheckedChange = { viewModel.toggleShowLogo(it) }
                    )
                }
            }
        }

        // Live Interactive Invoice Preview
        item {
            val companyProfile by viewModel.companyProfile.collectAsState()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ViewCarousel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Live Thermal Receipt Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Live layout for '${invoiceTheme.title}' on ${paperWidth.widthMm} mm paper.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            if (contentSettings.showLogo) {
                                val logoBmp = viewModel.getCachedLogoBitmap()
                                val logoUrl = companyProfile?.logoUrl
                                if (logoBmp != null || !logoUrl.isNullOrBlank()) {
                                    val logoAlignment = when (invoiceTheme) {
                                        InvoiceTheme.MODERN_MINIMAL -> Alignment.Start
                                        InvoiceTheme.CLASSIC, InvoiceTheme.DETAILED_GST -> Alignment.CenterHorizontally
                                    }
                                    val logoSize = when (paperWidth) {
                                        PaperWidth.WIDTH_58MM -> 48.dp
                                        PaperWidth.WIDTH_80MM -> 64.dp
                                        PaperWidth.WIDTH_A4 -> 76.dp
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = logoAlignment
                                    ) {
                                        CompanyLogoImage(
                                            logoUrl = logoUrl,
                                            previewBitmap = logoBmp,
                                            companyName = companyProfile?.name ?: "Vyavsa Business",
                                            size = logoSize,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = previewText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 3: PRINTER CONFIGURATION
// ============================================================================

@Composable
private fun PrinterConfigurationTab(
    viewModel: SettingsViewModel,
    onRequestPermissions: (Array<String>) -> Unit
) {
    val context = LocalContext.current
    val connectionType by viewModel.selectedConnectionType.collectAsState()
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val pairedPrinters by viewModel.pairedPrinters.collectAsState()
    val usbPrinters by viewModel.usbPrinters.collectAsState()
    val networkIp by viewModel.networkPrinterIp.collectAsState()
    val testPrintState by viewModel.testPrintState.collectAsState()

    var inputIp by remember(networkIp) { mutableStateOf(networkIp) }
    var showTroubleshooting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection Mode Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connection Interface", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Choose how the mobile terminal communicates with your thermal printer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrinterConnectionType.values().forEach { type ->
                            val isSelected = connectionType == type
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setConnectionType(type) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = type.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Currently Configured Printer Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedPrinter != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedPrinter != null) Icons.Default.CheckCircle else Icons.Default.Print,
                        contentDescription = null,
                        tint = if (selectedPrinter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedPrinter != null) "Default Printer Configured" else "No Printer Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedPrinter != null) "${selectedPrinter?.name} (${selectedPrinter?.address})" else "Select a printer below to enable automatic printing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Discovery / Device Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (connectionType) {
                        PrinterConnectionType.BLUETOOTH -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Paired Bluetooth Printers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.refreshPrinters() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Printers")
                                }
                            }
                            Text(
                                "Ensure printer is paired in Android Settings > Bluetooth before selecting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (pairedPrinters.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("No paired Bluetooth devices detected.", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Text("Open Bluetooth Settings")
                                        }
                                    }
                                }
                            } else {
                                pairedPrinters.forEach { printer ->
                                    val isSelected = selectedPrinter?.address == printer.address
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.selectPrinter(printer) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectPrinter(printer) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(printer.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                            Text(printer.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        PrinterConnectionType.USB -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Connected USB Printers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.refreshPrinters() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh USB")
                                }
                            }
                            if (usbPrinters.isEmpty()) {
                                Text("No USB thermal printers found. Connect via USB OTG adapter.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                usbPrinters.forEach { printer ->
                                    val isSelected = selectedPrinter?.address == printer.address
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.selectPrinter(printer) }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectPrinter(printer) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(printer.name, fontWeight = FontWeight.Bold)
                                            Text(printer.address, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }

                        PrinterConnectionType.NETWORK -> {
                            Text("Network Thermal Printer (ESC/POS over TCP/IP)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = inputIp,
                                onValueChange = { inputIp = it },
                                label = { Text("Printer IP Address & Port") },
                                placeholder = { Text("192.168.1.100:9100") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.setNetworkPrinterIp(inputIp)
                                    Toast.makeText(context, "Network printer saved", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Save Network IP")
                            }
                        }
                    }
                }
            }
        }

        // Test Print & Feedback
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Test Printing & Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Verify printer readiness, alignment, and formatting before regular sales.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (testPrintState.status == PrintJobStatus.ERROR) {
                        ErrorBanner(message = testPrintState.message ?: "Test print failed")
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (testPrintState.status == PrintJobStatus.SUCCESS) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(testPrintState.message ?: "Print succeeded!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            viewModel.runTestPrint(
                                onPermissionRequired = { perms -> onRequestPermissions(perms) }
                            )
                        },
                        enabled = selectedPrinter != null && testPrintState.status != PrintJobStatus.CONNECTING && testPrintState.status != PrintJobStatus.PRINTING,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (testPrintState.status == PrintJobStatus.CONNECTING || testPrintState.status == PrintJobStatus.PRINTING) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(testPrintState.message ?: "Printing...")
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Test Print")
                        }
                    }
                }
            }
        }

        // Troubleshooting Guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTroubleshooting = !showTroubleshooting },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Printer Troubleshooting Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(if (showTroubleshooting) "Hide" else "Show", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    AnimatedVisibility(visible = showTroubleshooting) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text("1. Bluetooth Permission Denied:", fontWeight = FontWeight.Bold)
                            Text("On Android 12+, enable 'Nearby Devices' permission in Android Settings > Apps > Vyavsa > Permissions.\n")

                            Text("2. Printer Not Listed:", fontWeight = FontWeight.Bold)
                            Text("Go to Android Settings > Bluetooth, pair the printer first (common PINs are 0000 or 1234), then tap the Refresh button above.\n")

                            Text("3. Blank Paper Output:", fontWeight = FontWeight.Bold)
                            Text("Thermal paper is heat-sensitive on one side only. If the paper comes out blank, ensure the paper roll is inserted with the active side facing the print head.\n")

                            Text("4. Connection Timeouts:", fontWeight = FontWeight.Bold)
                            Text("Ensure the printer is fully charged, powered on, within 5 meters, and not already connected to another smartphone or POS terminal.")
                        }
                    }
                }
            }
        }
    }
}
