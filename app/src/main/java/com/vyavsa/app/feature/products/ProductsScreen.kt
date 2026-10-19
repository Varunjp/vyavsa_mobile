package com.vyavsa.app.feature.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.ProductItemCard
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaSearchBar
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAnalytics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val productsState by viewModel.productsState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val isAdmin = viewModel.isTenantAdmin

    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    // Close dialog when mutation succeeds
    LaunchedEffect(actionState) {
        if (actionState is UiState.Success) {
            showAddEditDialog = false
            productToEdit = null
            productToDelete = null
            viewModel.resetActionState()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Product Catalog",
                subtitle = if (isAdmin) "Inventory catalog & unit pricing" else "Catalog items & standard rates",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    if (onNavigateToAnalytics != null) {
                        IconButton(onClick = onNavigateToAnalytics) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Product Analytics",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        productToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            VyavsaSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = "Search catalog by name...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Error banner if any
            if (actionState is UiState.Error) {
                ErrorBanner(
                    message = (actionState as UiState.Error).message,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Product List
            when (val state = productsState) {
                is UiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        ErrorBanner(message = state.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        VyavsaOutlinedButton(
                            text = "Retry",
                            onClick = { viewModel.loadProducts() }
                        )
                    }
                }
                is UiState.Empty -> {
                    EmptyStateView(
                        icon = Icons.Default.Inventory2,
                        title = if (searchQuery.isNotBlank()) "No Matching Products" else "Catalog is Empty",
                        description = if (searchQuery.isNotBlank()) {
                            "No products found for \"$searchQuery\". Try a different search term."
                        } else if (isAdmin) {
                            "You haven't added any products to your catalog yet. Tap \"Add Product\" below to create your first item."
                        } else {
                            "No products have been registered yet by the administrator."
                        },
                        actionText = if (isAdmin && searchQuery.isBlank()) "Add First Product" else null,
                        onActionClick = if (isAdmin && searchQuery.isBlank()) {
                            {
                                productToEdit = null
                                showAddEditDialog = true
                            }
                        } else null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is UiState.Success -> {
                    val products = state.data
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(products, key = { it.id }) { product ->
                            ProductItemCard(
                                product = product,
                                isAdmin = isAdmin,
                                onEditClick = {
                                    productToEdit = product
                                    showAddEditDialog = true
                                },
                                onDeleteClick = {
                                    productToDelete = product
                                }
                            )
                        }

                        // Extra space at bottom for FAB
                        item {
                            Spacer(modifier = Modifier.height(84.dp))
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            productToEdit = productToEdit,
            isLoading = actionState is UiState.Loading,
            errorMessage = (actionState as? UiState.Error)?.message,
            onDismiss = {
                showAddEditDialog = false
                productToEdit = null
                viewModel.resetActionState()
            },
            onSave = { name, price ->
                if (productToEdit != null) {
                    viewModel.updateProduct(productToEdit!!.id, name, price)
                } else {
                    viewModel.createProduct(name, price)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        val target = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Crimson50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Crimson600,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Delete Product?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${target.name}\"? If this product was used in historical sales, it will be safely archived without affecting previous financial records.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            },
            confirmButton = {
                VyavsaButton(
                    text = if (actionState is UiState.Loading) "Deleting..." else "Confirm Delete",
                    onClick = {
                        viewModel.deleteProduct(target.id)
                    },
                    enabled = actionState !is UiState.Loading
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { productToDelete = null },
                    enabled = actionState !is UiState.Loading
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

/**
 * Clean modal form for adding or editing catalog products with validation and accessibility.
 */
@Composable
private fun AddEditProductDialog(
    productToEdit: Product?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, price: String) -> Unit
) {
    var name by remember(productToEdit) { mutableStateOf(productToEdit?.name ?: "") }
    var unitPrice by remember(productToEdit) { mutableStateOf(productToEdit?.unitPrice ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                .padding(24.dp)
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Title
                Text(
                    text = if (productToEdit != null) "Edit Product" else "Add New Product",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter product name and default unit price for sales autofill.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (errorMessage != null) {
                    ErrorBanner(message = errorMessage, modifier = Modifier.padding(bottom = 12.dp))
                }

                // Field 1: Product Name
                Text(
                    text = "Product Name *",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    placeholder = { Text("e.g. Full Cream Milk 500ml") },
                    isError = nameError != null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = Crimson600),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Field 2: Default Unit Price
                Text(
                    text = "Default Unit Price (₹) *",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() || it == '.' }
                        unitPrice = filtered
                        if (filtered.isNotBlank()) priceError = null
                    },
                    placeholder = { Text("0.00") },
                    prefix = {
                        Text(
                            text = "₹ ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    isError = priceError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (priceError != null) {
                    Text(
                        text = priceError!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = Crimson600),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Actions: Cancel & Save
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VyavsaOutlinedButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    )

                    VyavsaButton(
                        text = if (isLoading) "Saving..." else "Save Product",
                        onClick = {
                            var hasError = false
                            if (name.trim().isBlank()) {
                                nameError = "Product name cannot be empty"
                                hasError = true
                            }
                            val pVal = unitPrice.trim().toDoubleOrNull()
                            if (pVal == null || pVal < 0.0) {
                                priceError = "Enter a valid price"
                                hasError = true
                            }

                            if (!hasError) {
                                onSave(name.trim(), unitPrice.trim().ifBlank { "0.00" })
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}
