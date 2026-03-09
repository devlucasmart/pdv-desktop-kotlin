package com.pdv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.*
import com.pdv.ui.components.PaymentDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SalesScreen(snackbarHostState: SnackbarHostState) {
    // Verificar permissão para fazer vendas
    val canMakeSales = UserSession.hasPermission(Permission.MAKE_SALES)

    var skuInput by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<SaleItem>() }
    var discount by remember { mutableStateOf(0.0) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var lastAddedProduct by remember { mutableStateOf<String?>(null) }

    val productDao = remember { ProductDao() }
    val saleDao = remember { SaleDao() }
    val cashDao = remember { CashRegisterDao() }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var currentSession by remember { mutableStateOf<CashSession?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

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
    val total = (subtotal - discount).coerceAtLeast(0.0)
    val itemCount = items.sumOf { it.quantity }

    // Função para adicionar produto
    fun addProduct() {
        if (skuInput.isBlank() || isProcessing) return

        isProcessing = true
        val product = productDao.findBySku(skuInput.trim())

        if (product != null) {
            // Verificar se o produto está ativo
            if (!product.active) {
                scope.launch {
                    snackbarHostState.showSnackbar("⚠ Produto '${product.name}' está inativo!")
                }
                skuInput = ""
                isProcessing = false
                return
            }

            // Verificar estoque disponível
            val quantidadeNoCarrinho = items.find { it.product.id == product.id }?.quantity ?: 0
            if (quantidadeNoCarrinho >= product.stockQuantity) {
                scope.launch {
                    snackbarHostState.showSnackbar("⚠ Estoque insuficiente! Disponível: ${product.stockQuantity}")
                }
                skuInput = ""
                isProcessing = false
                return
            }

            // Adicionar ou incrementar quantidade
            val existingIndex = items.indexOfFirst { it.product.id == product.id }
            if (existingIndex >= 0) {
                val existingItem = items[existingIndex]
                val newQuantity = existingItem.quantity + 1

                if (newQuantity > product.stockQuantity) {
                    scope.launch {
                        snackbarHostState.showSnackbar("⚠ Estoque máximo atingido: ${product.stockQuantity}")
                    }
                    skuInput = ""
                    isProcessing = false
                    return
                }

                items[existingIndex] = SaleItem(product, newQuantity, existingItem.discount)
            } else {
                items.add(SaleItem(product, 1))
            }

            lastAddedProduct = product.name
            skuInput = ""

            scope.launch {
                snackbarHostState.showSnackbar("✓ ${product.name} adicionado")
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("✗ Produto não encontrado: $skuInput")
            }
            skuInput = ""
        }
        isProcessing = false
    }

    // Função para limpar carrinho
    fun clearCart() {
        items.clear()
        discount = 0.0
        lastAddedProduct = null
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                    Text(
                        "$itemCount item(s) no carrinho",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Indicador de vendas do dia
            val salesToday = remember { saleDao.getSalesCountToday() }
            val totalToday = remember { saleDao.getSalesToday() }
            Card(
                elevation = 2.dp,
                backgroundColor = Color(0xFFE3F2FD)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Vendas Hoje", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "$salesToday vendas • R$ %.2f".format(totalToday),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                }
            }
        }

        // Status do Caixa
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            elevation = 2.dp,
            backgroundColor = if (currentSession != null) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
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
                        tint = if (currentSession != null) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            if (currentSession != null) "Caixa Aberto" else "Caixa Fechado",
                            fontWeight = FontWeight.Bold,
                            color = if (currentSession != null) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                        )
                        Text(
                            if (currentSession != null)
                                "Operador: ${currentSession?.openedBy ?: "-"}"
                            else
                                "Vá para a aba 'Caixa' para abrir",
                            fontSize = 12.sp,
                            color = Color.Gray
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
                        Text("Saldo em caixa", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            "R$ %.2f".format(saldoCaixa),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        // Aviso de permissão
        if (!canMakeSales) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                backgroundColor = Color(0xFFFFEBEE),
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Block, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Acesso Negado", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        Text("Você não tem permissão para realizar vendas.", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Campo de busca/scanner
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = skuInput,
                        onValueChange = { skuInput = it.uppercase().trim() },
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
                                    Icon(Icons.Default.Clear, "Limpar", tint = Color.Gray)
                                }
                            }
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
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
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Último: $productName", fontSize = 12.sp, color = Color(0xFF4CAF50))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Lista de itens
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
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
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Carrinho vazio", color = Color.Gray, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Text("Escaneie ou digite o SKU do produto para começar", color = Color.Gray, fontSize = 14.sp)

                        if (currentSession == null) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "⚠ Abra o caixa primeiro",
                                color = Color(0xFFFF9800),
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
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
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
                                        val validQty = newQty.coerceIn(1, item.product.stockQuantity)
                                        items[index] = SaleItem(item.product, validQty, item.discount)
                                    }
                                },
                                onRemove = {
                                    items.removeAll { it.product.id == item.product.id }
                                    if (items.isEmpty()) {
                                        discount = 0.0
                                        lastAddedProduct = null
                                    }
                                }
                            )
                            Divider(color = Color.Gray.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Painel de totais e ações
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    Text("R$ %.2f".format(subtotal), fontSize = 16.sp, color = Color.White)
                }

                // Desconto (se houver)
                if (discount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Desconto:", fontSize = 16.sp, color = Color(0xFFFFCDD2))
                            IconButton(
                                onClick = { discount = 0.0 },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, "Remover desconto", tint = Color(0xFFFFCDD2), modifier = Modifier.size(16.dp))
                            }
                        }
                        Text("-R$ %.2f".format(discount), fontSize = 16.sp, color = Color(0xFFFFCDD2))
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
                        "R$ %.2f".format(total),
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
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00C853))
                    ) {
                        Icon(Icons.Default.Payment, "Finalizar")
                        Spacer(Modifier.width(8.dp))
                        Text("PAGAR", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialog de Desconto
    if (showDiscountDialog) {
        var discountText by remember { mutableStateOf(if (discount > 0) "%.2f".format(discount) else "") }

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
                    Text("Subtotal: R$ %.2f".format(subtotal))
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
                            "Novo total: R$ %.2f".format(newTotal),
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
                            snackbarHostState.showSnackbar("✓ Desconto de R$ %.2f aplicado".format(discount))
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
        PaymentDialog(
            total = total,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { paymentMethod, receivedAmount ->
                // Fechar dialog imediatamente
                showPaymentDialog = false
                isProcessing = true

                scope.launch {
                    try {
                        println("=== INICIANDO PROCESSO DE VENDA ===")
                        val currentUser = UserSession.getCurrentUser()
                        println("Usuário: ${currentUser?.fullName}")

                        // Criar cópia dos itens antes de limpar
                        val saleItems = items.toList()
                        val saleDiscount = discount
                        val saleTotal = total

                        println("Itens no carrinho: ${saleItems.size}")
                        println("Total: R$ $saleTotal")
                        println("Desconto: R$ $saleDiscount")
                        println("Método: $paymentMethod")

                        val sale = Sale(
                            items = saleItems,
                            discount = saleDiscount,
                            paymentMethod = paymentMethod,
                            operatorName = currentUser?.fullName ?: "Desconhecido"
                        )

                        println("Venda criada. Salvando no banco...")
                        println("  - Subtotal: ${sale.subtotal}")
                        println("  - Total: ${sale.total}")
                        println("  - Status: ${sale.status}")

                        val saleId = saleDao.save(sale)
                        println("Resultado do save: saleId = $saleId")

                        if (saleId > 0) {
                            println("✓ Venda salva com sucesso!")

                            // Decrementar estoque
                            saleItems.forEach { item ->
                                val product = item.product
                                val newStock = (product.stockQuantity - item.quantity).coerceAtLeast(0)
                                productDao.update(product.copy(stockQuantity = newStock))
                            }

                            // Registrar movimento no caixa (para dinheiro)
                            if (paymentMethod == PaymentMethod.DINHEIRO.name) {
                                currentSession?.let { session ->
                                    println("Registrando movimento no caixa (sessão ${session.id})...")
                                    cashDao.recordMovement(session.id, "SALE", saleTotal, "Venda #$saleId")
                                }
                            }

                            // Limpar carrinho
                            clearCart()

                            // Mostrar sucesso
                            showSuccessDialog = true
                            snackbarHostState.showSnackbar("✓ Venda #$saleId finalizada!")
                        } else {
                            println("✗ saleId retornou 0 - falha ao salvar")
                            snackbarHostState.showSnackbar("✗ Erro ao salvar venda. Verifique o console.")
                        }
                    } catch (e: Exception) {
                        println("✗ EXCEÇÃO ao processar venda: ${e.message}")
                        e.printStackTrace()
                        snackbarHostState.showSnackbar("✗ Erro: ${e.message ?: "Erro desconhecido"}")
                    } finally {
                        isProcessing = false
                        println("=== FIM DO PROCESSO DE VENDA ===")
                    }
                }
            }
        )
    }

    // Dialog de Sucesso
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Venda Concluída!", color = Color(0xFF4CAF50))
                }
            },
            text = {
                Column {
                    Text("A venda foi registrada com sucesso!")
                    Spacer(Modifier.height(8.dp))
                    Text("O carrinho foi limpo e está pronto para uma nova venda.", fontSize = 14.sp, color = Color.Gray)
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                ) {
                    Text("Nova Venda", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun SaleItemCard(
    item: SaleItem,
    onQuantityChange: (Int) -> Unit,
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
                    "R$ %.2f".format(item.product.price),
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Text(" • ", color = Color.Gray)
                Text(
                    "SKU: ${item.product.sku}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            // Indicador de estoque
            val stockColor = when {
                item.quantity >= item.product.stockQuantity -> Color(0xFFFF5722)
                item.product.stockQuantity <= 5 -> Color(0xFFFF9800)
                else -> Color.Gray
            }
            Text(
                "Estoque: ${item.product.stockQuantity}",
                fontSize = 11.sp,
                color = stockColor
            )
        }

        // Controle de quantidade
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(100.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) },
                enabled = item.quantity > 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.RemoveCircle,
                    "Diminuir",
                    tint = if (item.quantity > 1) MaterialTheme.colors.primary else Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                "${item.quantity}",
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { if (item.quantity < item.product.stockQuantity) onQuantityChange(item.quantity + 1) },
                enabled = item.quantity < item.product.stockQuantity,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    "Aumentar",
                    tint = if (item.quantity < item.product.stockQuantity) MaterialTheme.colors.primary else Color.Gray.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Total do item
        Text(
            "R$ %.2f".format(item.total),
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
                tint = Color(0xFFE53935)
            )
        }
    }
}
