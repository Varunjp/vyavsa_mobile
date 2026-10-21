package com.vyavsa.app.feature.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.model.Employee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordAdvanceDialog(
    employees: List<Employee>,
    initialEmployee: Employee? = null,
    onDismiss: () -> Unit,
    onConfirm: (employeeId: String, amount: String, date: String, paymentMethod: String, note: String?) -> Unit
) {
    var selectedEmployee by remember { mutableStateOf(initialEmployee ?: employees.firstOrNull()) }
    var employeeDropdownExpanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("cash") }
    var date by remember { mutableStateOf(FormatUtils.todayDateString()) }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Add Employee Advance", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = Crimson600,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Employee Selection
                ExposedDropdownMenuBox(
                    expanded = employeeDropdownExpanded,
                    onExpandedChange = { employeeDropdownExpanded = !employeeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmployee?.name ?: "Select Employee",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Employee") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeDropdownExpanded) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = employeeDropdownExpanded,
                        onDismissRequest = { employeeDropdownExpanded = false }
                    ) {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text(emp.name, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedEmployee = emp
                                    employeeDropdownExpanded = false
                                    validationError = null
                                }
                            )
                        }
                    }
                }

                // Amount
                VyavsaTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validationError = null
                    },
                    label = "Advance Amount (₹)",
                    placeholder = "e.g. 500.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Payment Method
                Text(
                    text = "Disbursement Method",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                VyavsaSegmentedTabs(
                    tabs = listOf("Cash Drawer", "Bank Transfer"),
                    selectedIndex = if (paymentMethod == "cash") 0 else 1,
                    onTabSelected = { idx ->
                        paymentMethod = if (idx == 0) "cash" else "bank"
                    }
                )

                // Optional Note
                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Notes / Reason (Optional)",
                    placeholder = "e.g. Emergency advance"
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    val emp = selectedEmployee
                    if (emp == null) {
                        validationError = "Please select an employee"
                        return@TextButton
                    }
                    val amtVal = amount.trim().toDoubleOrNull()
                    if (amtVal == null || amtVal <= 0.0) {
                        validationError = "Please enter a valid amount greater than zero"
                        return@TextButton
                    }
                    isSubmitting = true
                    onConfirm(emp.id, amount.trim(), date, paymentMethod, note.trim().ifBlank { null })
                }
            ) {
                Text(
                    text = if (isSubmitting) "Saving..." else "Confirm Advance",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordOvertimeDialog(
    employees: List<Employee>,
    initialEmployee: Employee? = null,
    onDismiss: () -> Unit,
    onConfirm: (employeeId: String, amount: String, date: String, note: String?) -> Unit
) {
    var selectedEmployee by remember { mutableStateOf(initialEmployee ?: employees.firstOrNull()) }
    var employeeDropdownExpanded by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(FormatUtils.todayDateString()) }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Record Overtime", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = Crimson600,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Employee Selection
                ExposedDropdownMenuBox(
                    expanded = employeeDropdownExpanded,
                    onExpandedChange = { employeeDropdownExpanded = !employeeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmployee?.name ?: "Select Employee",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Employee") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeDropdownExpanded) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = employeeDropdownExpanded,
                        onDismissRequest = { employeeDropdownExpanded = false }
                    ) {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text(emp.name, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedEmployee = emp
                                    employeeDropdownExpanded = false
                                    validationError = null
                                }
                            )
                        }
                    }
                }

                // Amount
                VyavsaTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validationError = null
                    },
                    label = "Overtime Pay Amount (₹)",
                    placeholder = "e.g. 150.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Optional Note
                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Reason / OT Hours",
                    placeholder = "e.g. 3 hours evening overtime"
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmitting,
                onClick = {
                    val emp = selectedEmployee
                    if (emp == null) {
                        validationError = "Please select an employee"
                        return@TextButton
                    }
                    val amtVal = amount.trim().toDoubleOrNull()
                    if (amtVal == null || amtVal <= 0.0) {
                        validationError = "Please enter a valid amount greater than zero"
                        return@TextButton
                    }
                    isSubmitting = true
                    onConfirm(emp.id, amount.trim(), date, note.trim().ifBlank { null })
                }
            ) {
                Text(
                    text = if (isSubmitting) "Saving..." else "Record Overtime",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}
