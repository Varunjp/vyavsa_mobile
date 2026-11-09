package com.vyavsa.app.feature.party

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.model.Party

@Composable
fun CreatePartyDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        phone: String?,
        email: String?,
        address: String?,
        role: String,
        openingReceivable: Double,
        openingPayable: Double
    ) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("both") } // "both", "customer", "supplier"
    var openingReceivable by rememberSaveable { mutableStateOf("0.00") }
    var openingPayable by rememberSaveable { mutableStateOf("0.00") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val emailRegex = remember { "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex() }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "Register Business Party",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Single business entity with customer, supplier, or dual ledgers",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Field
                VyavsaTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.trim().length >= 2) nameError = null
                    },
                    label = "Party / Company Name *",
                    placeholder = "e.g. ABC Traders Pvt Ltd",
                    leadingIcon = Icons.Default.Business,
                    isError = nameError != null,
                    errorMessage = nameError,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Phone & Email Row
                VyavsaTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number",
                    placeholder = "+91 9876543210",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                VyavsaTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (it.isBlank() || emailRegex.matches(it.trim())) emailError = null
                    },
                    label = "Email Address",
                    placeholder = "contact@abctraders.com",
                    leadingIcon = Icons.Default.Email,
                    isError = emailError != null,
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                // Address Field
                VyavsaTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Physical / Billing Address",
                    placeholder = "Street, City, State, PIN",
                    leadingIcon = Icons.Default.LocationOn,
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Business Entity Role *",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = role == "both",
                            onClick = { role = "both" },
                            label = { Text("Both", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = role == "customer",
                            onClick = { role = "customer" },
                            label = { Text("Customer", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = role == "supplier",
                            onClick = { role = "supplier" },
                            label = { Text("Supplier", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (role) {
                            "customer" -> "Maintains customer credit ledger, sales invoices, and collections."
                            "supplier" -> "Maintains supplier payables ledger, purchase bills, and payments."
                            else -> "Dual role maintains independent Accounts Receivable & Payable with dynamic Net Balance."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Opening Balances Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Auditable Opening Balances (₹)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        // Opening Receivable (Visible if Customer or Both)
                        AnimatedVisibility(
                            visible = role == "customer" || role == "both",
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            VyavsaTextField(
                                value = openingReceivable,
                                onValueChange = { openingReceivable = it },
                                label = "Opening Receivable (₹)",
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Opening Payable (Visible if Supplier or Both)
                        AnimatedVisibility(
                            visible = role == "supplier" || role == "both",
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            VyavsaTextField(
                                value = openingPayable,
                                onValueChange = { openingPayable = it },
                                label = "Opening Payable (₹)",
                                placeholder = "0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            VyavsaButton(
                text = if (isLoading) "Saving..." else "Register Party",
                isLoading = isLoading,
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.length < 2) {
                        nameError = "Name must be at least 2 characters"
                        return@VyavsaButton
                    }
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isNotEmpty() && !emailRegex.matches(trimmedEmail)) {
                        emailError = "Please enter a valid email address"
                        return@VyavsaButton
                    }
                    val recv = openingReceivable.toDoubleOrNull() ?: 0.0
                    val pay = openingPayable.toDoubleOrNull() ?: 0.0
                    onSubmit(
                        trimmedName,
                        phone.trim().ifBlank { null },
                        trimmedEmail.ifBlank { null },
                        address.trim().ifBlank { null },
                        role,
                        recv,
                        pay
                    )
                }
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditPartyDialog(
    party: Party,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        phone: String?,
        email: String?,
        address: String?,
        role: String,
        status: String
    ) -> Unit
) {
    var name by rememberSaveable(party.id) { mutableStateOf(party.displayName) }
    var phone by rememberSaveable(party.id) { mutableStateOf(party.phone ?: "") }
    var email by rememberSaveable(party.id) { mutableStateOf(party.email ?: "") }
    var address by rememberSaveable(party.id) { mutableStateOf(party.address ?: "") }
    var role by rememberSaveable(party.id) {
        mutableStateOf(if (party.isDualRole) "both" else if (party.isSupplierOnly) "supplier" else "customer")
    }
    var status by rememberSaveable(party.id) { mutableStateOf(party.status.ifBlank { "active" }) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val emailRegex = remember { "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex() }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Edit Business Party",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VyavsaTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.trim().length >= 2) nameError = null
                    },
                    label = "Party Name *",
                    placeholder = "Enter party name",
                    leadingIcon = Icons.Default.Business,
                    isError = nameError != null,
                    errorMessage = nameError,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                VyavsaTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number",
                    placeholder = "+91 9876543210",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                VyavsaTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (it.isBlank() || emailRegex.matches(it.trim())) emailError = null
                    },
                    label = "Email Address",
                    placeholder = "contact@abctraders.com",
                    leadingIcon = Icons.Default.Email,
                    isError = emailError != null,
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                VyavsaTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Address",
                    placeholder = "Street, City, State, PIN",
                    leadingIcon = Icons.Default.LocationOn,
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Business Role",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = role == "both",
                            onClick = { role = "both" },
                            label = { Text("Both", fontWeight = FontWeight.SemiBold) }
                        )
                        FilterChip(
                            selected = role == "customer",
                            onClick = { role = "customer" },
                            label = { Text("Customer", fontWeight = FontWeight.SemiBold) }
                        )
                        FilterChip(
                            selected = role == "supplier",
                            onClick = { role = "supplier" },
                            label = { Text("Supplier", fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Status
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = status == "active",
                            onClick = { status = "active" },
                            label = { Text("Active", fontWeight = FontWeight.SemiBold) }
                        )
                        FilterChip(
                            selected = status == "inactive",
                            onClick = { status = "inactive" },
                            label = { Text("Inactive", fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            VyavsaButton(
                text = if (isLoading) "Saving..." else "Save Changes",
                isLoading = isLoading,
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.length < 2) {
                        nameError = "Name must be at least 2 characters"
                        return@VyavsaButton
                    }
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isNotEmpty() && !emailRegex.matches(trimmedEmail)) {
                        emailError = "Please enter a valid email address"
                        return@VyavsaButton
                    }
                    onSubmit(
                        trimmedName,
                        phone.trim().ifBlank { null },
                        trimmedEmail.ifBlank { null },
                        address.trim().ifBlank { null },
                        role,
                        status
                    )
                }
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
