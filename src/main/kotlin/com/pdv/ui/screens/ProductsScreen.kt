package com.pdv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.Product
import com.pdv.data.ProductDao
import com.pdv.data.UserSession
import com.pdv.data.Permission
import com.pdv.util.CurrencyUtils
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter

@Composable
fun ProductsScreen(snackbarHostState: SnackbarHostState) {
    val productDao = remember { ProductDao() }
    var products by remember { mutableStateOf(productDao.findAll()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
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

    val outerScroll = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthContent: Dp = if (this.maxWidth < 1000.dp) this.maxWidth - 32.dp else 980.dp
        Column(modifier = Modifier.fillMaxSize().verticalScroll(outerScroll).padding(16.dp)) {
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
                        onClick = { editingProduct = null; showDialog = true },
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
            Card(modifier = Modifier.fillMaxWidth().weight(1f).widthIn(max = maxWidthContent), elevation = 2.dp) {
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
                                    editingProduct = product
                                    showDialog = true
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

        if (outerScroll.maxValue > 0) {
            VerticalScrollbar(adapter = rememberScrollbarAdapter(outerScroll), modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp))
        }

        if (showDialog) {
            ProductDialog(
                existing = editingProduct,
                onDismiss = { showDialog = false },
                onSave = { newProduct ->
                    scope.launch {
                        // se estamos editando
                        if (editingProduct != null) {
                            val ok = productDao.update(newProduct)
                            if (ok) {
                                snackbarHostState.showSnackbar("Produto atualizado com sucesso")
                            } else {
                                snackbarHostState.showSnackbar("Erro ao atualizar produto")
                            }
                        } else {
                            // criação: validar SKU único
                            val existing = productDao.findBySku(newProduct.sku)
                            if (existing != null) {
                                snackbarHostState.showSnackbar("SKU já existe: use outro código")
                                return@launch
                            }
                            val id = productDao.save(newProduct)
                            if (id > 0) {
                                snackbarHostState.showSnackbar("Produto criado com sucesso")
                            } else {
                                snackbarHostState.showSnackbar("Erro ao salvar produto")
                            }
                        }
                        products = productDao.findAll()
                        showDialog = false
                        editingProduct = null
                    }
                }
            )
        }
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
                    val stockText = if (product.unit != "un") String.format("%.2f %s", product.stockQuantity, product.unit) else "${product.stockQuantity.toInt()} ${product.unit}."
                    Text(
                        stockText,
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
fun ProductDialog(existing: Product? = null, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    // armazenar entrada bruta para melhor máscara
    var priceRaw by remember { mutableStateOf("") }
    var stockRaw by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("un") }
    var category by remember { mutableStateOf("") }
    val units = listOf("un", "kg", "g", "L", "ml", "cx")
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    // validation state
    var skuError by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf("") }
    var priceError by remember { mutableStateOf("") }

    val productDao = remember { ProductDao() }

    // Inicializar estados com valores existentes (modo edição)
    LaunchedEffect(existing) {
        existing?.let { p ->
            sku = p.sku
            name = p.name
            // store formatted strings so display shows proper separators
            priceRaw = CurrencyUtils.formatPlain(p.price)
            stockRaw = CurrencyUtils.formatPlain(p.stockQuantity)
            unit = p.unit
            category = p.category ?: ""
        } ?: run {
            // ensure defaults when creating
            sku = ""
            name = ""
            priceRaw = ""
            stockRaw = ""
            unit = "un"
            category = ""
        }
        skuError = ""
        nameError = ""
        priceError = ""
    }

    AlertDialog(
         onDismissRequest = onDismiss,
         title = { Text("Novo Produto") },
         text = {
             Column(modifier = Modifier.width(420.dp)) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { if (existing == null) { sku = it; skuError = "" } },
                    label = { Text("SKU *") },
                    placeholder = { Text("Código único") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = existing != null,
                    isError = skuError.isNotBlank()
                )
                if (skuError.isNotBlank()) {
                    Text(skuError, color = MaterialTheme.colors.error, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }
                 Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = "" },
                    label = { Text("Nome *") },
                    placeholder = { Text("Nome do produto") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError.isNotBlank()
                )
                if (nameError.isNotBlank()) {
                    Text(nameError, color = MaterialTheme.colors.error, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }
                 Spacer(Modifier.height(8.dp))

                // Preço com máscara enquanto digita
                val displayPrice = if (priceRaw.isBlank()) "" else CurrencyUtils.formatFromInput(priceRaw)
                OutlinedTextField(
                    value = displayPrice,
                    onValueChange = { v -> priceRaw = v.filter { c -> c.isDigit() || c == ',' || c == '.' }; priceError = "" },
                    label = { Text("Preço *") },
                    placeholder = { Text("0,00") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("R$") },
                    singleLine = true,
                    isError = priceError.isNotBlank()
                )
                if (priceError.isNotBlank()) {
                    Text(priceError, color = MaterialTheme.colors.error, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }
                Spacer(Modifier.height(8.dp))

                // Estoque (mascara simples usando mesma utilidade)
                val displayStock = if (stockRaw.isBlank()) "" else CurrencyUtils.formatFromInput(stockRaw)
                OutlinedTextField(
                    value = displayStock,
                    onValueChange = { v -> stockRaw = v.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Estoque") },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("Qtd") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                // Select de unidade estilizado
                val unitLabels = mapOf(
                    "un" to "Unidade (un)",
                    "kg" to "Quilograma (kg)",
                    "g" to "Grama (g)",
                    "L" to "Litro (L)",
                    "ml" to "Mililitro (ml)",
                    "cx" to "Caixa (cx)"
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Unidade",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { unitDropdownExpanded = !unitDropdownExpanded },
                            shape = MaterialTheme.shapes.small,
                            border = ButtonDefaults.outlinedBorder,
                            color = MaterialTheme.colors.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Straighten,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        unitLabels[unit] ?: unit,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                }
                                Icon(
                                    if (unitDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Selecionar",
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false },
                            modifier = Modifier.width(380.dp)
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    onClick = {
                                        unit = u
                                        unitDropdownExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            if (u == unit) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (u == unit) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                unitLabels[u] ?: u,
                                                fontWeight = if (u == unit) FontWeight.Bold else FontWeight.Normal,
                                                color = if (u == unit) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                                            )
                                        }
                                    }
                                }
                                if (u != units.last()) {
                                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Categoria (opcional)
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    placeholder = { Text("Categoria do produto") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
         },
         confirmButton = {
            Button(
                onClick = {
                    // validations
                    var ok = true
                    if (sku.trim().isEmpty()) {
                        skuError = "SKU obrigatório"
                        ok = false
                    }
                    if (name.trim().isEmpty()) {
                        nameError = "Nome obrigatório"
                        ok = false
                    }
                    val priceVal = CurrencyUtils.parse(priceRaw)
                    if (priceVal <= 0.0) {
                        priceError = "Preço inválido"
                        ok = false
                    }

                    // if creating, check SKU uniqueness
                    if (existing == null && ok) {
                        val exists = productDao.findBySku(sku.trim())
                        if (exists != null) {
                            skuError = "SKU já cadastrado"
                            ok = false
                        }
                    }

                    if (!ok) return@Button

                    val price = CurrencyUtils.parse(priceRaw)
                    val stock = CurrencyUtils.parse(stockRaw)
                    val product = Product(
                        sku = sku.trim(),
                        name = name.trim(),
                        price = price,
                        stockQuantity = stock,
                        unit = unit.ifBlank { "un" },
                        category = category.trim().ifEmpty { null }
                    )
                    onSave(product)
                },
                enabled = true
            ) {
                Text(if (existing == null) "Salvar" else "Atualizar")
            }
         },
         dismissButton = {
             OutlinedButton(onClick = onDismiss) {
                 Text("Cancelar")
             }
         }
     )
}
