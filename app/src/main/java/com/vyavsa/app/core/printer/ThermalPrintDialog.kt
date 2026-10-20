package com.vyavsa.app.core.printer

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs

@Composable
fun ThermalPrintDialog(
    printJobState: PrintJobState,
    pairedPrinters: List<PrinterDevice>,
    usbPrinters: List<PrinterDevice> = emptyList(),
    selectedConnectionType: PrinterConnectionType = PrinterConnectionType.BLUETOOTH,
    networkPrinterIp: String = "192.168.1.100:9100",
    selectedPrinter: PrinterDevice?,
    paperWidth: PaperWidth,
    hasCompanyLogo: Boolean = false,
    receiptPreviewText: String,
    onSelectConnectionType: (PrinterConnectionType) -> Unit = {},
    onSetNetworkPrinterIp: (String) -> Unit = {},
    onSelectPrinter: (PrinterDevice) -> Unit,
    onSelectPaperWidth: (PaperWidth) -> Unit,
    onRefreshPrinters: () -> Unit,
    onPrintClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }
    var networkInput by remember(networkPrinterIp) { mutableStateOf(networkPrinterIp) }
    val focusManager = LocalFocusManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onRefreshPrinters()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
        onRefreshPrinters()
    }

    Dialog(
        onDismissRequest = {
            if (printJobState.status != PrintJobStatus.CONNECTING && printJobState.status != PrintJobStatus.PRINTING) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Blue50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = Blue600,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Print Thermal Invoice",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ESC/POS receipt printer",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = printJobState.status != PrintJobStatus.CONNECTING && printJobState.status != PrintJobStatus.PRINTING
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // 1. Paper Width Selector & Logo Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Receipt Paper Width",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (hasCompanyLogo) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald600.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald600,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Logo Included",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Emerald600,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                VyavsaSegmentedTabs(
                    tabs = listOf(PaperWidth.WIDTH_58MM.label, PaperWidth.WIDTH_80MM.label),
                    selectedIndex = if (paperWidth == PaperWidth.WIDTH_58MM) 0 else 1,
                    onTabSelected = { idx ->
                        onSelectPaperWidth(if (idx == 0) PaperWidth.WIDTH_58MM else PaperWidth.WIDTH_80MM)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Printer Transport Selector (Bluetooth, USB OTG, Network)
                Text(
                    text = "Connection Interface",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                VyavsaSegmentedTabs(
                    tabs = listOf("Bluetooth", "USB OTG", "Network / Wi-Fi"),
                    selectedIndex = when (selectedConnectionType) {
                        PrinterConnectionType.BLUETOOTH -> 0
                        PrinterConnectionType.USB -> 1
                        PrinterConnectionType.NETWORK -> 2
                    },
                    onTabSelected = { idx ->
                        val chosen = when (idx) {
                            0 -> PrinterConnectionType.BLUETOOTH
                            1 -> PrinterConnectionType.USB
                            else -> PrinterConnectionType.NETWORK
                        }
                        onSelectConnectionType(chosen)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Printer Device Selector based on Interface
                when (selectedConnectionType) {
                    PrinterConnectionType.BLUETOOTH -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paired Bluetooth Printers",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                    }
                                    onRefreshPrinters()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh paired printers",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (pairedPrinters.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "No paired Bluetooth printers detected",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Pair your thermal printer in Android Bluetooth Settings, then tap refresh above.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        } else {
                            pairedPrinters.forEach { printer ->
                                val isSelected = selectedPrinter?.address == printer.address && selectedPrinter.connectionType == PrinterConnectionType.BLUETOOTH
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onSelectPrinter(printer) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Bluetooth,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = printer.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                Text(
                                                    text = printer.address,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
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
                            Text(
                                text = "Connected USB Printers",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = { onRefreshPrinters() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh USB printers",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (usbPrinters.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "No USB thermal printer detected",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Connect your thermal printer via USB OTG cable, power it on, and tap refresh.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        } else {
                            usbPrinters.forEach { printer ->
                                val isSelected = selectedPrinter?.address == printer.address && selectedPrinter.connectionType == PrinterConnectionType.USB
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onSelectPrinter(printer) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Usb,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = printer.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                Text(
                                                    text = printer.address,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PrinterConnectionType.NETWORK -> {
                        Text(
                            text = "Network Printer Address",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = networkInput,
                            onValueChange = {
                                networkInput = it
                                onSetNetworkPrinterIp(it)
                            },
                            placeholder = { Text("192.168.1.100:9100") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lan,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onSetNetworkPrinterIp(networkInput)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Standard ESC/POS port is 9100. Enter printer IP (e.g., 192.168.1.100 or 192.168.1.100:9100).",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Live Receipt Preview Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPreview = !showPreview }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Receipt Text Preview",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Icon(
                            imageVector = if (showPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPreview) "Hide Preview" else "Show Preview",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showPreview) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .horizontalScroll(rememberScrollState())
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = receiptPreviewText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Status & Error Messages
                if (printJobState.status == PrintJobStatus.ERROR && !printJobState.message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ErrorBanner(
                        message = printJobState.message,
                        onDismiss = {}
                    )
                }

                if (printJobState.status == PrintJobStatus.SUCCESS && !printJobState.message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald600.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = printJobState.message,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Emerald600,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                val isBusy = printJobState.status == PrintJobStatus.CONNECTING || printJobState.status == PrintJobStatus.PRINTING
                val isPrintEnabled = selectedPrinter != null && !isBusy

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VyavsaOutlinedButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    )
                    VyavsaButton(
                        text = if (isBusy) (printJobState.message ?: "Printing...") else "Print Invoice",
                        onClick = onPrintClick,
                        enabled = isPrintEnabled,
                        isLoading = isBusy,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}
