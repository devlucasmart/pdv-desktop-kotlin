package com.pdv.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pdv.data.*
import com.pdv.ui.components.PaymentDialog
import com.pdv.ui.components.PaymentDialogSplit
import com.pdv.util.CurrencyUtils
import com.pdv.util.NumberUtils
import com.pdv.util.PdfUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SalesScreen(snackbarHostState: SnackbarHostState) {
    // Verificar permissão para fazer vendas
    val canMakeSales = UserSession.hasPermission(Permission.MAKE_SALES)

    var skuInput by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<SaleItem>() }
    var discount by remember { mutableStateOf(0.0) }
    var clientAppliedDiscount by remember { mutableStateOf(0.0) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var lastAddedProduct by remember { mutableStateOf<String?>(null) }
    var lastSaleForReceipt by remember { mutableStateOf<Sale?>(null) }

    val productDao = remember { ProductDao() }
    val saleDao = remember { SaleDao() }
    val paymentPartDao = remember { com.pdv.data.PaymentPartDao() }
    val cashDao = remember { CashRegisterDao() }
    val clientDao = remember { ClientDao() }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    // FocusRequester para capturar atalhos no diálogo de sucesso
    val dialogFocusRequester = remember { FocusRequester() }

    // helper functions for export/print/view (local so they can use snackbarHostState)
    suspend fun exportReceiptAction(sale: Sale) {
        try {
            val folder = java.io.File(System.getProperty("user.home"), "pdv_receipts/${java.time.LocalDate.now()}")
            if (!folder.exists()) folder.mkdirs()
            val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val out = java.io.File(folder, "sale_${sale.id}_$timestamp.pdf")
            com.pdv.util.PdfUtils.generateReceiptPdf(sale, out)
            snackbarHostState.showSnackbar("✓ PDF salvo: ${out.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            snackbarHostState.showSnackbar("✗ Erro ao gerar PDF: ${e.message}")
        }
    }

    suspend fun printReceiptAction(sale: Sale) {
        try {
            val folder = java.io.File(System.getProperty("java.io.tmpdir"))
            val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val out = java.io.File(folder, "sale_${sale.id}_$timestamp.pdf")
            com.pdv.util.PdfUtils.generateReceiptPdf(sale, out)
            val ok = com.pdv.util.PdfUtils.printPdf(out)
            if (ok) snackbarHostState.showSnackbar("✓ Enviado para impressora") else snackbarHostState.showSnackbar("✗ Erro ao imprimir")
        } catch (e: Exception) {
            e.printStackTrace()
            snackbarHostState.showSnackbar("✗ Erro na impressão: ${e.message}")
        }
    }

    suspend fun viewReceiptAction(sale: Sale) {
        try {
            val folder = java.io.File(System.getProperty("java.io.tmpdir"))
            val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val out = java.io.File(folder, "sale_preview_${sale.id}_$timestamp.pdf")
            com.pdv.util.PdfUtils.generateReceiptPdf(sale, out)
            try {
                val desktop = java.awt.Desktop.getDesktop()
                desktop.open(out)
                snackbarHostState.showSnackbar("✓ Abrindo visualizador: ${out.name}")
            } catch (inner: Exception) {
                snackbarHostState.showSnackbar("✓ PDF gerado: ${out.absolutePath}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            snackbarHostState.showSnackbar("✗ Erro ao gerar/abrir PDF: ${e.message}")
        }
    }

    // Request focus on dialog to capture key events when it opens
    LaunchedEffect(showSuccessDialog, lastSaleForReceipt) {
        if (showSuccessDialog && lastSaleForReceipt != null) {
            kotlinx.coroutines.delay(60)
            try { dialogFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    var currentSession by remember { mutableStateOf<CashSession?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var showClientPicker by remember { mutableStateOf(false) }
    var chargeToAccount by remember { mutableStateOf(false) }

    // Carregar sessão atual
    LaunchedEffect(Unit) {
        currentSession = cashDao.getCurrentOpenSession()
    }

    // Atualizar sessão após fechar diálogo de sucesso
    LaunchedEffect(showSuccessDialog) {
        if (!showSuccessDialog) {
            currentSession = cashDao.getCurrentOpenSession()
        }
    }

    // Auto-focus no campo de SKU
    LaunchedEffect(Unit) {
        delay(300)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val subtotal = items.sumOf { it.total }
    val total = (subtotal - discount - clientAppliedDiscount).coerceAtLeast(0.0)
    // number of distinct line items
    val itemCount = items.size
    // total quantity (sum of quantities) for informational display
    val totalQuantity = items.sumOf { it.quantity }

    // Função para adicionar produto
    // Selected product from fuzzy search
    var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showQuantityDialog by remember { mutableStateOf<Pair<Product, Double>?>(null) }

    // When quantity dialog confirms, add or update item
    fun confirmAddQuantity(product: Product, qty: Double) {
        println("→ confirmAddQuantity: ${product.sku} qty=$qty")
        val existingIndex = items.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            items[existingIndex] = SaleItem(product, qty, items[existingIndex].discount)
            println("→ updated existing item at index $existingIndex")
        } else {
            items.add(SaleItem(product, qty))
            println("→ added new item, total items=${items.size}")
        }
        scope.launch {
            snackbarHostState.showSnackbar("✓ ${product.name} adicionado x${NumberUtils.formatQuantity(qty, product.unit)}")
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    fun addProduct() {
        val rawInput = skuInput
        val normalized = rawInput.trim().uppercase()
        if (normalized.isBlank() || isProcessing) return

        println("→ addProduct: input='$rawInput' normalized='$normalized'")
        isProcessing = true
        // tolerant SKU lookup: normalized, raw, stripped leading zeros
        var product = productDao.findBySku(normalized)
        if (product == null && rawInput.isNotBlank()) {
            product = productDao.findBySku(rawInput.trim())
        }
        if (product == null && normalized.startsWith("0")) {
            val stripped = normalized.trimStart('0')
            if (stripped.isNotBlank()) product = productDao.findBySku(stripped)
        }

        if (product == null) {
            // try fuzzy search (name or SKU contains term)
            val results = productDao.findByQuery(normalized)
            println("→ findByQuery returned ${results.size} results for '$normalized'")
            if (results.isEmpty()) {
                scope.launch { snackbarHostState.showSnackbar("✗ Produto não encontrado: $rawInput") }
                skuInput = ""
                isProcessing = false
                return
            } else if (results.size == 1) {
                product = results.first()
            } else {
                // multiple matches -> open picker
                searchResults = results
                showProductPicker = true
                isProcessing = false
                return
            }
        }

        if (product != null) {
             println("→ Produto encontrado: ${product.name} (SKU=${product.sku}) unit=${product.unit} stock=${product.stockQuantity}")
             // Verificar se o produto está ativo
             if (!product.active) {
                scope.launch {
                    snackbarHostState.showSnackbar("⚠ Produto '${product.name}' está inativo!")
                }
                skuInput = ""
                isProcessing = false
                return
            }

            // Verificar estoque disponível (usar Double)
            val quantidadeNoCarrinho = items.find { it.product.id == product.id }?.quantity ?: 0.0
            if (quantidadeNoCarrinho >= product.stockQuantity) {
                scope.launch {
                    snackbarHostState.showSnackbar("⚠ Estoque insuficiente! Disponível: ${product.stockQuantity} ${product.unit}")
                }
                skuInput = ""
                isProcessing = false
                return
            }

            // If product sold by unit (un), add immediately with quantity 1.0
            if (product.unit == "un") {
                println("→ Auto-adding unit product ${product.sku}")
                confirmAddQuantity(product, 1.0)
                lastAddedProduct = product.name
                skuInput = ""
                // restore focus
                scope.launch {
                    try { focusRequester.requestFocus() } catch (_: Exception) {}
                }
                isProcessing = false
            } else {
                // Open quantity dialog for fractional units (kg, g, L...)
                val defaultQty = 0.1
                showQuantityDialog = product to defaultQty
                // product will be added/updated when quantity dialog confirmed

                lastAddedProduct = product.name
                skuInput = ""
                isProcessing = false
            }
        } else {
            // handled above
        }
    }


    // Função para limpar carrinho
    fun clearCart() {
        items.clear()
        discount = 0.0
        clientAppliedDiscount = 0.0
        lastAddedProduct = null
        selectedClient = null
        chargeToAccount = false
    }

    val outerScroll = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val maxContentWidth: Dp = if (this.maxWidth < 1000.dp) this.maxWidth - 32.dp else 980.dp

        Column(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(outerScroll)
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Cabeçalho com título e status
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Ponto de Venda",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (itemCount > 0) {
                        val totalQtyDisplay = if (totalQuantity % 1.0 == 0.0) totalQuantity.toInt().toString() else String.format("%.2f", totalQuantity)
                        Text(
                            "$itemCount itens • $totalQtyDisplay total",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Indicador de vendas do dia
                val salesToday = remember { saleDao.getSalesCountToday() }
                val totalToday = remember { saleDao.getSalesToday() }
                Card(
                    elevation = 2.dp,
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Vendas Hoje", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                        Text(
                            "${salesToday} vendas • ${CurrencyUtils.format(totalToday)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }

            // ── Card de Cliente (separado, sempre visível) ─────────────
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth).padding(bottom = 8.dp),
                elevation = if (selectedClient != null) 3.dp else 1.dp,
                backgroundColor = when {
                    chargeToAccount && selectedClient != null -> Color(0xFFFFF8E1)
                    selectedClient != null -> MaterialTheme.colors.surface
                    else -> MaterialTheme.colors.surface.copy(alpha = 0.7f)
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selectedClient != null) Icons.Default.Person else Icons.Default.PersonAdd,
                        null,
                        tint = if (selectedClient != null) MaterialTheme.colors.primary
                               else MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))

                    if (selectedClient != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(selectedClient!!.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (!selectedClient!!.document.isNullOrBlank())
                                Text(selectedClient!!.document!!, fontSize = 11.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        }

                        // Toggle FIADO com destaque
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (chargeToAccount) Color(0xFFE65100).copy(alpha = 0.12f)
                                    else MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
                                )
                                .clickable { chargeToAccount = !chargeToAccount }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (chargeToAccount) Icons.Default.AccountBalance else Icons.Default.Payments,
                                null,
                                tint = if (chargeToAccount) Color(0xFFE65100) else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                if (chargeToAccount) "FIADO" else "À vista",
                                fontSize = 10.sp,
                                fontWeight = if (chargeToAccount) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (chargeToAccount) Color(0xFFE65100) else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Alterar / remover
                        IconButton(onClick = { showClientPicker = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { selectedClient = null; clientAppliedDiscount = 0.0; chargeToAccount = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Text("Nenhum cliente selecionado",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f))
                        TextButton(onClick = { showClientPicker = true }) {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Selecionar", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Status do Caixa
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth).padding(bottom = 12.dp),
                elevation = 2.dp,
                backgroundColor = if (currentSession != null) MaterialTheme.colors.surface else MaterialTheme.colors.error.copy(alpha = 0.06f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (currentSession != null) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (currentSession != null) MaterialTheme.colors.primary else MaterialTheme.colors.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                if (currentSession != null) "Caixa Aberto" else "Caixa Fechado",
                                fontWeight = FontWeight.Bold,
                                color = if (currentSession != null) MaterialTheme.colors.primary else MaterialTheme.colors.error
                            )
                            Text(
                                if (currentSession != null)
                                    "Operador: ${currentSession?.openedBy ?: "-"}"
                                else
                                    "Vá para a aba 'Caixa' para abrir",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (currentSession != null) {
                        // Mostrar saldo atual do caixa
                        val movements = remember(currentSession) {
                            currentSession?.let { cashDao.getMovementsForSession(it.id) } ?: emptyList()
                        }
                        val totalMovements = movements.sumOf { (it["amount"] as? Double) ?: 0.0 }
                        val saldoCaixa = (currentSession?.initialAmount ?: 0.0) + totalMovements

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Saldo em caixa", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                            Text(
                                CurrencyUtils.format(saldoCaixa),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                }
            }

            // Aviso de permissão
            if (!canMakeSales) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.06f),
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, null, tint = MaterialTheme.colors.error, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Acesso Negado", fontWeight = FontWeight.Bold, color = MaterialTheme.colors.error)
                            Text("Você não tem permissão para realizar vendas.", fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Campo de busca/scanner
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = skuInput,
                            onValueChange = { skuInput = it.uppercase() },
                            label = { Text("SKU ou Código de Barras") },
                            placeholder = { Text("Digite o código e pressione Enter...") },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                        addProduct()
                                        true
                                    } else {
                                        false
                                    }
                                },
                            singleLine = true,
                            enabled = canMakeSales && currentSession != null && !isProcessing,
                            leadingIcon = {
                                Icon(Icons.Default.QrCodeScanner, "Scanner", tint = MaterialTheme.colors.primary)
                            },
                            trailingIcon = {
                                if (skuInput.isNotBlank()) {
                                    IconButton(onClick = { skuInput = "" }) {
                                        Icon(Icons.Default.Clear, "Limpar", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colors.primary,
                                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = { addProduct() },
                            enabled = skuInput.isNotBlank() && canMakeSales && currentSession != null && !isProcessing,
                            modifier = Modifier.height(56.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                            )
                            } else {
                                Icon(Icons.Default.Add, "Adicionar")
                                Spacer(Modifier.width(4.dp))
                                Text("Adicionar")
                            }
                        }
                    }

                    // Último produto adicionado
                    lastAddedProduct?.let { productName ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Último: $productName", fontSize = 12.sp, color = MaterialTheme.colors.primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Lista de itens
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = maxContentWidth),
                elevation = 2.dp
            ) {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Carrinho vazio", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            Text("Escaneie ou digite o SKU do produto para começar", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f), fontSize = 14.sp)

                            if (currentSession == null) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "⚠ Abra o caixa primeiro",
                                    color = MaterialTheme.colors.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Column {
                        // Header da lista
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.06f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Produto", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                            Text("Qtd", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                            Text("Total", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.End)
                            Spacer(Modifier.width(48.dp))
                        }

                        LazyColumn(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            items(items, key = { "${it.product.id}-${it.quantity}" }) { item ->
                                SaleItemCard(
                                    item = item,
                                    onQuantityChange = { newQty ->
                                        val index = items.indexOfFirst { it.product.id == item.product.id }
                                        if (index >= 0) {
                                            val minQty = if (item.product.unit != "un") 0.1 else 1.0
                                            val validQty = newQty.coerceIn(minQty, item.product.stockQuantity)
                                            items[index] = SaleItem(item.product, validQty, item.discount)
                                        }
                                    },
                                    onRemove = {
                                        items.removeAll { it.product.id == item.product.id }
                                        if (items.isEmpty()) {
                                            discount = 0.0
                                            clientAppliedDiscount = 0.0
                                            lastAddedProduct = null
                                        }
                                    }
                                )
                                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Painel de totais e ações
            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                backgroundColor = MaterialTheme.colors.primary,
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal ($itemCount itens):", fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
                        Text(CurrencyUtils.format(subtotal), fontSize = 16.sp, color = Color.White)
                    }

                    // Desconto (se houver)
                    if (discount > 0 || clientAppliedDiscount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Descontos:", fontSize = 16.sp, color = Color(0xFFFFCDD2))
                                if (discount > 0) {
                                    Text("Venda: -${CurrencyUtils.format(discount)}", fontSize = 12.sp, color = Color(0xFFFFCDD2))
                                }
                                if (clientAppliedDiscount > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cliente: -${CurrencyUtils.format(clientAppliedDiscount)}", fontSize = 12.sp, color = Color(0xFFFFCDD2))
                                }
                                IconButton(
                                    onClick = { discount = 0.0; clientAppliedDiscount = 0.0; selectedClient = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Remover desconto", tint = Color(0xFFFFCDD2), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text("- ${CurrencyUtils.format(discount + clientAppliedDiscount)}", fontSize = 16.sp, color = Color(0xFFFFCDD2))
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.White.copy(alpha = 0.3f),
                        thickness = 2.dp
                    )

                    // Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL:", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            CurrencyUtils.format(total),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF69F0AE)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Botões de ação
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Botão Limpar
                        OutlinedButton(
                            onClick = { clearCart() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = items.isNotEmpty(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.DeleteSweep, "Limpar")
                            Spacer(Modifier.width(4.dp))
                            Text("Limpar")
                        }

                        // Botão Desconto
                        OutlinedButton(
                            onClick = { showDiscountDialog = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = items.isNotEmpty() && canMakeSales,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Discount, "Desconto")
                            Spacer(Modifier.width(4.dp))
                            Text("Desconto")
                        }

                        // Botão Finalizar
                        Button(
                            onClick = {
                                if (!canMakeSales) {
                                    scope.launch { snackbarHostState.showSnackbar("✗ Sem permissão para vendas") }
                                    return@Button
                                }
                                if (currentSession == null) {
                                    scope.launch { snackbarHostState.showSnackbar("✗ Abra o caixa primeiro") }
                                    return@Button
                                }
                                if (items.isEmpty()) {
                                    scope.launch { snackbarHostState.showSnackbar("✗ Carrinho vazio") }
                                    return@Button
                                }
                                showPaymentDialog = true
                            },
                            modifier = Modifier.weight(2f).height(50.dp),
                            enabled = items.isNotEmpty() && canMakeSales && currentSession != null,
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                        ) {
                            Icon(Icons.Default.Payment, "Finalizar")
                            Spacer(Modifier.width(8.dp))
                            Text("PAGAR", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Optional scrollbar when content overflows vertically
        if (outerScroll.maxValue > 0) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(outerScroll),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp)
            )
        }
    }

    // Render product picker when fuzzy search returns multiple matches
    if (showProductPicker) {
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("Selecione o produto") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    searchResults.forEach { p ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                showProductPicker = false
                                showQuantityDialog = p to if (p.unit != "un") 0.1 else 1.0
                            }
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text("SKU: ${p.sku} • ${NumberUtils.formatQuantity(p.stockQuantity, p.unit)}", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(CurrencyUtils.format(p.price) + if (p.unit != "un") " /${p.unit}" else "", fontWeight = FontWeight.Bold)
                            }
                        }
                        Divider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProductPicker = false }) { Text("Fechar") }
            }
        )
    }

    // Quantity dialog (improved with formatting + inline validation)
    showQuantityDialog?.let { (prod, defaultQty) ->
        var qtyField by remember { mutableStateOf(TextFieldValue(NumberUtils.formatDecimalForInput(defaultQty))) }
        var qtyError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuantityDialog = null },
            title = { Text("Quantidade - ${prod.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = qtyField,
                        onValueChange = { tf ->
                            val filtered = tf.text.filter { c -> c.isDigit() || c == ',' || c == '.' }
                            // simple preserve cursor: keep same selection when possible
                            val sel = tf.selection
                            qtyField = TextFieldValue(filtered, sel)
                            qtyError = ""
                        },
                        label = { Text("Quantidade (${prod.unit})") },
                        placeholder = { Text(if (prod.unit != "un") "0,10" else "1") },
                        singleLine = true,
                        leadingIcon = { Text("Qtd") },
                        isError = qtyError.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (qtyError.isNotBlank()) {
                        Text(qtyError, color = MaterialTheme.colors.error, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Estoque disponível: ${NumberUtils.formatQuantity(prod.stockQuantity, prod.unit)}", fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = NumberUtils.parseQuantity(qtyField.text)
                    val minQty = if (prod.unit != "un") 0.1 else 1.0
                    when {
                        parsed <= 0.0 -> { qtyError = "Quantidade inválida"; return@TextButton }
                        parsed < minQty -> { qtyError = "Mínimo ${NumberUtils.formatQuantity(minQty, prod.unit)}"; return@TextButton }
                        parsed > prod.stockQuantity -> { qtyError = "Máximo ${NumberUtils.formatQuantity(prod.stockQuantity, prod.unit)}"; return@TextButton }
                    }
                    // add to cart
                    confirmAddQuantity(prod, parsed)
                    lastAddedProduct = prod.name
                    showQuantityDialog = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showQuantityDialog = null }) { Text("Cancelar") } }
        )
    }

    // Dialog de Desconto
    if (showDiscountDialog) {
        var discountText by remember { mutableStateOf(if (discount > 0) CurrencyUtils.formatPlain(discount) else "") }

        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Discount, null, tint = MaterialTheme.colors.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Aplicar Desconto")
                }
            },
            text = {
                Column {
                    Text("Subtotal: ${CurrencyUtils.format(subtotal)}")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { v ->
                            discountText = v.filter { it.isDigit() || it == '.' || it == ',' }
                                .replace(",", ".")
                        },
                        label = { Text("Valor do desconto (R$)") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) }
                    )

                    val newDiscount = discountText.toDoubleOrNull() ?: 0.0
                    if (newDiscount > 0) {
                        Spacer(Modifier.height(8.dp))
                        val newTotal = (subtotal - newDiscount).coerceAtLeast(0.0)
                        Text(
                            "Novo total: ${CurrencyUtils.format(newTotal)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newDiscount = discountText.toDoubleOrNull() ?: 0.0
                        discount = newDiscount.coerceIn(0.0, subtotal)
                        showDiscountDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("✓ Desconto de ${CurrencyUtils.format(discount)} aplicado")
                        }
                    }
                ) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog de Pagamento
    if (showPaymentDialog) {
        PaymentDialogSplit(
            total = total,
            onDismiss = { showPaymentDialog = false },
            chargeToAccount = chargeToAccount,
            clientName = selectedClient?.name,
            onConfirm = { payments ->
                // payments: List<Pair<paymentMethodName, amount>>
                showPaymentDialog = false
                isProcessing = true

                scope.launch {
                    try {
                        println("=== INICIANDO PROCESSO DE VENDA (SPLIT) ===")
                        val currentUser = UserSession.getCurrentUser()
                        println("Usuário: ${currentUser?.fullName}")

                        val saleItems = items.toList()
                        val saleDiscount = discount
                        val saleTotal = total

                        // determine payment method summary
                        val methodName = if (payments.size == 1) payments.first().first else "MIXED"

                        println("Itens no carrinho: ${saleItems.size}")
                        println("Total: R$ $saleTotal")
                        println("Desconto: R$ $saleDiscount")
                        println("Método: $methodName")

                        val sale = Sale(
                            items = saleItems,
                            discount = saleDiscount,
                            paymentMethod = methodName,
                            paymentParts = payments.map { it.first to it.second },
                            operatorName = currentUser?.fullName ?: "Desconhecido",
                            clientId = selectedClient?.id,
                            clientDiscount = clientAppliedDiscount,
                            chargeToAccount = chargeToAccount
                        )

                        val saleId = saleDao.save(sale)
                        println("Resultado do save: saleId = $saleId")

                        if (saleId > 0) {
                            println("✓ Venda salva com sucesso!")

                            // Decrementar estoque
                            saleItems.forEach { item ->
                                val product = item.product
                                val newStock = (product.stockQuantity - item.quantity).coerceAtLeast(0.0)
                                productDao.update(product.copy(stockQuantity = newStock))
                            }

                            // Registrar movimento no caixa apenas para a parte em dinheiro
                            val cashAmount = payments.filter { it.first == PaymentMethod.DINHEIRO.name }.sumOf { it.second }
                            if (cashAmount > 0) {
                                currentSession?.let { session ->
                                    println("Registrando movimento em dinheiro no caixa (sessão ${session.id}) valor R$ $cashAmount")
                                    cashDao.recordMovement(session.id, "SALE", cashAmount, "Venda #$saleId")
                                }
                            }

                            // Limpar carrinho
                            clearCart()

                            // Mostrar sucesso
                            val recordedSale = sale.copy(id = saleId)
                            lastSaleForReceipt = recordedSale
                            showSuccessDialog = true
                            scope.launch { snackbarHostState.showSnackbar("✓ Venda #$saleId finalizada!") }
                        } else {
                            println("✗ saleId retornou 0 - falha ao salvar")
                            scope.launch { snackbarHostState.showSnackbar("✗ Erro ao salvar venda. Verifique o console.") }
                        }
                    } catch (e: Exception) {
                        println("✗ EXCEÇÃO ao processar venda: ${e.message}")
                        e.printStackTrace()
                        scope.launch { snackbarHostState.showSnackbar("✗ Erro: ${e.message ?: "Erro desconhecido"}") }
                    } finally {
                        isProcessing = false
                        println("=== FIM DO PROCESSO DE VENDA ===")
                    }
                }
            }
        )
    }

    // Dialog de Sucesso (simplificado) - show only when we don't have a sale ready for receipt
    if (showSuccessDialog && lastSaleForReceipt == null) {
         AlertDialog(
             onDismissRequest = { showSuccessDialog = false },
             title = {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(32.dp))
                     Spacer(Modifier.width(12.dp))
                     Text("Venda Concluída!", color = MaterialTheme.colors.primary)
                 }
             },
             text = {
                 Column {
                     Text("A venda foi registrada com sucesso!")
                     Spacer(Modifier.height(8.dp))
                     Text("O carrinho foi limpo e está pronto para uma nova venda.", fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                 }
             },
             confirmButton = {
                 Button(
                     onClick = {
                         showSuccessDialog = false
                         // Refocar no campo de SKU
                         scope.launch {
                             delay(100)
                             try { focusRequester.requestFocus() } catch (_: Exception) {}
                         }
                     },
                     colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                 ) {
                     Text("Nova Venda", color = Color.White)
                 }
             }
         )
     }

    // Extended success dialog with print/export options (shown when lastSaleForReceipt available)
     if (showSuccessDialog && lastSaleForReceipt != null) {
         AlertDialog(
             onDismissRequest = { showSuccessDialog = false },
             title = { Text("Venda Concluída") },
             text = {
                 // attach key handler to this column so E/P/V keyboard shortcuts trigger actions
                 Column(modifier = Modifier
                     .onKeyEvent { keyEvent ->
                         if (keyEvent.type == KeyEventType.KeyDown) {
                             val k = keyEvent.key
                             when (k) {
                                 androidx.compose.ui.input.key.Key.E -> {
                                     lastSaleForReceipt?.let {
                                         scope.launch { exportReceiptAction(it) }
                                     }
                                     true
                                 }
                                 androidx.compose.ui.input.key.Key.P -> {
                                     lastSaleForReceipt?.let {
                                         scope.launch { printReceiptAction(it) }
                                     }
                                     true
                                 }
                                 androidx.compose.ui.input.key.Key.V -> {
                                     lastSaleForReceipt?.let {
                                         scope.launch { viewReceiptAction(it) }
                                     }
                                     true
                                 }
                                 else -> false
                             }
                         } else false
                     }
                     .focusRequester(dialogFocusRequester)
                     .focusable()
                 ) {
                     Text("Deseja imprimir o cupom fiscal ou exportar em PDF? (Atalhos: E=Exportar, P=Imprimir, V=Visualizar)")
                     Spacer(Modifier.height(8.dp))
                 }
             },
             confirmButton = {
                 Row {
                     Button(onClick = {
                         // Export PDF
                         scope.launch {
                             lastSaleForReceipt?.let { exportReceiptAction(it) }
                             showSuccessDialog = false
                             lastSaleForReceipt = null
                          }
                      }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)) {
                         Icon(Icons.Default.PictureAsPdf, null)
                         Spacer(Modifier.width(8.dp))
                         Text("Exportar PDF", color = Color.White)
                     }

                     Spacer(Modifier.width(8.dp))

                     Button(onClick = {
                         // Print
                         scope.launch {
                             lastSaleForReceipt?.let { printReceiptAction(it) }
                             showSuccessDialog = false
                             lastSaleForReceipt = null
                          }
                      }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)) {
                         Icon(Icons.Default.Print, null)
                         Spacer(Modifier.width(8.dp))
                         Text("Imprimir", color = Color.White)
                     }

                     Spacer(Modifier.width(8.dp))

                     // Visualizar
                     Button(onClick = {
                         scope.launch {
                             lastSaleForReceipt?.let { viewReceiptAction(it) }
                             showSuccessDialog = false
                             lastSaleForReceipt = null
                          }
                      }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primaryVariant)) {
                         Icon(Icons.Default.Visibility, null)
                         Spacer(Modifier.width(8.dp))
                         Text("Visualizar", color = Color.White)
                     }
                 }
             },
             dismissButton = {
                 TextButton(onClick = { showSuccessDialog = false; lastSaleForReceipt = null }) { Text("Fechar") }
             }
         )
     }

    // Client picker dialog
    if (showClientPicker) {
        val clients = remember { clientDao.findAll() }
        Dialog(onCloseRequest = { showClientPicker = false }) {
            Card(modifier = Modifier.fillMaxWidth(0.6f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Selecionar Cliente", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = "", onValueChange = {})
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        clients.forEach { c ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                selectedClient = c
                                // aplicar desconto padrão do cliente (percentual)
                                if (c.defaultDiscountPercent > 0.0) {
                                    // calcular desconto absoluto baseado no subtotal
                                    clientAppliedDiscount = (items.sumOf { it.total } * (c.defaultDiscountPercent / 100.0))
                                }
                                showClientPicker = false
                            }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.name, fontWeight = FontWeight.Medium)
                                    Text(c.document ?: "", fontSize = 12.sp, color = Color.Gray)
                                }
                                if (c.defaultDiscountPercent > 0.0) {
                                    Text("${c.defaultDiscountPercent}%", color = MaterialTheme.colors.primary)
                                }
                            }
                            Divider()
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showClientPicker = false }) { Text("Fechar") }
                    }
                }
            }
        }
    }
}

@Composable
fun SaleItemCard(
    item: SaleItem,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Informações do produto
        Column(modifier = Modifier.weight(2f)) {
            Text(
                item.product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1
            )
            Row {
                Text(
                    CurrencyUtils.format(item.product.price),
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(" • ", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                Text(
                    "SKU: ${item.product.sku}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
            // Indicador de estoque
            val stockColor = when {
                item.quantity >= item.product.stockQuantity -> MaterialTheme.colors.error
                item.product.stockQuantity <= 5 -> MaterialTheme.colors.secondary
                else -> MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            }
            Text(
                "Estoque: ${item.product.stockQuantity} ${item.product.unit}",
                fontSize = 11.sp,
                color = stockColor
            )
        }

        // Controle de quantidade
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(120.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val step = if (item.product.unit != "un") 0.1 else 1.0
            IconButton(
                onClick = { if (item.quantity > step) onQuantityChange((item.quantity - step).coerceAtLeast(step)) },
                enabled = item.quantity > step,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.RemoveCircle,
                    "Diminuir",
                    tint = if (item.quantity > step) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                // Mostrar com duas casas para fracionários, sem casas para unidade
                if (item.product.unit != "un") String.format("%.2f", item.quantity) else item.quantity.toInt().toString(),
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { if (item.quantity < item.product.stockQuantity) onQuantityChange((item.quantity + step).coerceAtMost(item.product.stockQuantity)) },
                enabled = item.quantity < item.product.stockQuantity,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    "Aumentar",
                    tint = if (item.quantity < item.product.stockQuantity) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Total do item
        Text(
            CurrencyUtils.format(item.total),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colors.primary
        )

        // Botão remover
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                "Remover",
                tint = MaterialTheme.colors.error
            )
        }
    }

}
