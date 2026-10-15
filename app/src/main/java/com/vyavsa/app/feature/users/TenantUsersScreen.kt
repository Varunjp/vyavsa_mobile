package com.vyavsa.app.feature.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.AvatarInitialCircle
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaPasswordField
import com.vyavsa.app.core.designsystem.VyavsaSearchBar
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.TenantUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantUsersScreen(
    viewModel: TenantUsersViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usersState by viewModel.usersState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val isAdmin = viewModel.isAdmin

    var showCreateUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<TenantUser?>(null) }
    var userToDelete by remember { mutableStateOf<TenantUser?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Tenant Users & Roles",
                subtitle = "System logins, permissions & access control",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateUserDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add User Account", fontWeight = FontWeight.Bold) }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isAdmin) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(32.dp)
                ) {
                    EmptyStateView(
                        title = "Access Restricted",
                        description = "Only tenant administrators can manage user login accounts and access roles.",
                        icon = Icons.Default.Shield
                    )
                }
                return@Scaffold
            }

            VyavsaSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchChange(it) },
                placeholder = "Search user by name or email...",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (actionState is UiState.Error) {
                ErrorBanner(
                    message = (actionState as UiState.Error).message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            when (val state = usersState) {
                is UiState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Empty -> {
                    EmptyStateView(
                        title = "No Users Found",
                        description = "Add staff or operational team members to provide them system login credentials.",
                        icon = Icons.Default.Person,
                        actionText = "Create User Account",
                        onActionClick = { showCreateUserDialog = true }
                    )
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        ErrorBanner(message = state.message)
                    }
                }
                is UiState.Success -> {
                    val users = state.data

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(users) { user ->
                            TenantUserCard(
                                user = user,
                                onEditClick = { userToEdit = user },
                                onDeleteClick = { userToDelete = user }
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Create User Dialog
    if (showCreateUserDialog) {
        CreateTenantUserDialog(
            onDismiss = { showCreateUserDialog = false },
            onConfirm = { name, email, password, role ->
                showCreateUserDialog = false
                viewModel.createUser(name, email, password, role)
            }
        )
    }

    // Edit User Dialog
    userToEdit?.let { user ->
        EditTenantUserDialog(
            user = user,
            onDismiss = { userToEdit = null },
            onConfirm = { name, role, status, password ->
                userToEdit = null
                viewModel.updateUser(user.id, name, role, status, password)
            }
        )
    }

    // Delete User Confirmation Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Deactivate User Account") },
            text = {
                Text("Are you sure you want to remove or deactivate ${user.name} (${user.email})? This user will immediately lose access to the system.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userToDelete = null
                        viewModel.deleteUser(user.id)
                    }
                ) {
                    Text("Deactivate / Remove", color = Crimson600, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TenantUserCard(
    user: TenantUser,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            AvatarInitialCircle(
                name = user.name,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatBadge(
                        text = user.role.uppercase(),
                        backgroundColor = if (user.role.equals("admin", ignoreCase = true)) Blue50 else MaterialTheme.colorScheme.surfaceContainerHigh,
                        textColor = if (user.role.equals("admin", ignoreCase = true)) Blue600 else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                StatBadge(
                    text = user.status.uppercase(),
                    backgroundColor = if (user.status.equals("active", ignoreCase = true)) Emerald50 else Crimson50,
                    textColor = if (user.status.equals("active", ignoreCase = true)) Emerald600 else Crimson600
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit User",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete User",
                    tint = Crimson600
                )
            }
        }
    }
}

@Composable
private fun CreateTenantUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, password: String, role: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("user") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create System User Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name"
                )

                VyavsaTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Login Email",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                VyavsaPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password"
                )

                Text(
                    text = "Assign Role:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = role == "user",
                        onClick = { role = "user" },
                        label = { Text("Standard User") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = role == "admin",
                        onClick = { role = "admin" },
                        label = { Text("Tenant Admin") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue50,
                            selectedLabelColor = Blue600
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                        onConfirm(name.trim(), email.trim(), password, role)
                    }
                }
            ) {
                Text("Create Account", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditTenantUserDialog(
    user: TenantUser,
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: String, status: String, newPassword: String?) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var role by remember { mutableStateOf(user.role) }
    var status by remember { mutableStateOf(user.status) }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User: ${user.email}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name"
                )

                Text(
                    text = "System Role:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = role == "user",
                        onClick = { role = "user" },
                        label = { Text("Standard User") }
                    )

                    FilterChip(
                        selected = role == "admin",
                        onClick = { role = "admin" },
                        label = { Text("Tenant Admin") }
                    )
                }

                Text(
                    text = "Account Status:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = status == "active",
                        onClick = { status = "active" },
                        label = { Text("Active") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald50,
                            selectedLabelColor = Emerald600
                        )
                    )

                    FilterChip(
                        selected = status == "inactive",
                        onClick = { status = "inactive" },
                        label = { Text("Inactive") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Crimson50,
                            selectedLabelColor = Crimson600
                        )
                    )
                }

                VyavsaPasswordField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "Reset Password (leave empty to keep current)"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), role, status, newPassword.ifBlank { null })
                    }
                }
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
