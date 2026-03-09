package com.pdv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.Product
import com.pdv.data.ProductDao
import com.pdv.data.UserSession
import com.pdv.data.Permission
import kotlinx.coroutines.launch

@Composable
fun ProductsScreen(snackbarHostState: SnackbarHostState) {
    val productDao = remember { ProductDao() }
    var products by remember { mutableStateOf(productDao.findAll()) }
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Permissões
    val canAddProducts = UserSession.hasPermission(Permission.ADD_PRODUCTS)
    val canEditProducts = UserSession.hasPermission(Permission.EDIT_PRODUCTS)
    val canDeleteProducts = UserSession.hasPermission(Permission.DELETE_PRODUCTS)

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.sku.contains(searchQuery, ignoreCase = true) ||
        it.category?.contains(searchQuery, ignoreCase = true) == true
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Gerenciar Produtos",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (canAddProducts) {
                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                ) {
                    Icon(Icons.Default.Add, "Adicionar")
                    Spacer(Modifier.width(4.dp))
                    Text("Novo Produto")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Busca
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar produto") },
            placeholder = { Text("Nome, SKU ou categoria...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, "Limpar")
                    }
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // Estatísticas
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                elevation = 2.dp,
                backgroundColor = Color(0xFFE3F2FD)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total de Produtos", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${products.size}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                elevation = 2.dp,
                backgroundColor = Color(0xFFFFF3E0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estoque Baixo", fontSize = 12.sp, color = Color.Gray)
                    val lowStock = products.count { it.isLowStock() }
                    Text(
                        "$lowStock",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lowStock > 0) Color(0xFFFF6F00) else Color(0xFF4CAF50)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Lista de produtos
        Card(modifier = Modifier.weight(1f).fillMaxWidth(), elevation = 2.dp) {
            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Nenhum produto encontrado", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            canEdit = canEditProducts,
                            canDelete = canDeleteProducts,
                            onEdit = {
                                // TODO: implementar edição
                                scope.launch {
                                    snackbarHostState.showSnackbar("Edição em desenvolvimento")
                                }
                            },
                            onDelete = {
                                productDao.delete(product.sku)
                                products = productDao.findAll()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Produto removido")
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showDialog) {
        ProductDialog(
            onDismiss = { showDialog = false },
            onSave = { product ->
                productDao.save(product)
                products = productDao.findAll()
                showDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Produto adicionado com sucesso!")
                }
            }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row {
                    Text("SKU: ${product.sku}", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.width(16.dp))
                    if (product.category != null) {
                        Text("Categoria: ${product.category}", fontSize = 14.sp, color = Color.Gray)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Estoque: ", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        "${product.stockQuantity} un.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.isLowStock()) Color.Red else Color(0xFF4CAF50)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "R$ %.2f".format(product.price),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    if (canEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colors.primary)
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "Remover", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Deseja realmente remover o produto '${product.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Remover", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ProductDialog(onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Produto") },
        text = {
            Column(modifier = Modifier.width(400.dp)) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU *") },
                    placeholder = { Text("Código único") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome *") },
                    placeholder = { Text("Nome do produto") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Preço *") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("R$") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { c -> c.isDigit() } },
                    label = { Text("Estoque") },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    placeholder = { Text("Ex: Bebidas, Alimentos...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val product = Product(
                        sku = sku.trim(),
                        name = name.trim(),
                        price = price.toDoubleOrNull() ?: 0.0,
                        stockQuantity = stock.toIntOrNull() ?: 0,
                        category = category.trim().ifEmpty { null }
                    )
                    onSave(product)
                },
                enabled = sku.isNotBlank() && name.isNotBlank() && price.toDoubleOrNull() != null
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

