package com.pdv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CashRegisterScreen(snackbarHostState: SnackbarHostState) {
    val cashDao = remember { CashRegisterDao() }
    val scope = rememberCoroutineScope()

    var currentSession by remember { mutableStateOf<CashSession?>(null) }
    var movements by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showOpenCashDialog by remember { mutableStateOf(false) }
    var showCloseCashDialog by remember { mutableStateOf(false) }
    var showWithdrawalDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // utilitário para sanear entrada de moeda (retorna string com '.' como separador decimal, sem separadores de milhar)
    fun sanitizeCurrencyInputRaw(input: String): String {
        val cleaned = input.filter { it.isDigit() || it == '.' || it == ',' }
        if (cleaned.isBlank()) return ""
        val dot = cleaned.replace(',', '.')
        val parts = dot.split('.')
        return if (parts.size <= 1) {
            dot
        } else {
            // junta tudo depois do primeiro ponto (evita pontos adicionais)
            parts[0] + "." + parts.drop(1).joinToString("")
        }
    }

    // formata para exibição com duas casas e separador de milhar no padrão pt-BR: 1.234,56
    fun formatCurrencyDisplayFromRaw(raw: String): String {
        val d = raw.toDoubleOrNull() ?: return ""
        val symbols = DecimalFormatSymbols(Locale("pt", "BR")).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
        val df = DecimalFormat("#,##0.00", symbols)
        return df.format(d)
    }

    // parseia string exibida (pode conter '.' como separador de milhar e ',' decimal) para Double
    fun parseDisplayCurrency(display: String): Double {
        if (display.isBlank()) return 0.0
        // remover espaços
        val trimmed = display.trim()
        // remover agrupadores de milhares (pontos) e substituir vírgula por ponto
        val normalized = trimmed.replace(".", "").replace(',', '.')
        return normalized.toDoubleOrNull() ?: 0.0
    }

    // Função para atualizar dados
    fun refreshData() {
        currentSession = cashDao.getCurrentOpenSession()
        currentSession?.let { session ->
            movements = cashDao.getMovementsForSession(session.id)
        } ?: run {
            movements = emptyList()
        }
    }

    // Carregar dados inicial e periodicamente
    LaunchedEffect(refreshTrigger) { refreshData() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            refreshData()
        }
    }

    // Calcular totais
    val totalVendas = movements.filter { (it["type"] as? String) == "SALE" }
        .sumOf { (it["amount"] as? Double) ?: 0.0 }
    val totalSangrias = movements.filter { (it["type"] as? String) == "WITHDRAWAL" }
        .sumOf { (it["amount"] as? Double) ?: 0.0 }
    val totalReforcos = movements.filter { (it["type"] as? String) == "DEPOSIT" }
        .sumOf { (it["amount"] as? Double) ?: 0.0 }
    val saldoAtual = (currentSession?.initialAmount ?: 0.0) + totalVendas + totalReforcos - totalSangrias

    val outerScroll = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthContent: Dp = if (this.maxWidth < 1000.dp) this.maxWidth - 32.dp else 980.dp

        Column(modifier = Modifier.fillMaxSize().verticalScroll(outerScroll).padding(16.dp)) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Controle de Caixa",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                // Botão de atualizar
                IconButton(onClick = { refreshTrigger++ }) {
                    Icon(Icons.Default.Refresh, "Atualizar", tint = MaterialTheme.colors.primary)
                }
            }

            // Status do Caixa
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                backgroundColor = if (currentSession != null) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (currentSession != null) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (currentSession != null) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    if (currentSession != null) "CAIXA ABERTO" else "CAIXA FECHADO",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentSession != null) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                                )
                                currentSession?.let { session ->
                                    Text(
                                        "Aberto por: ${session.openedBy ?: "Desconhecido"}",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        "Abertura: ${formatDateTime(session.openedAt)}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        if (currentSession == null) {
                            Button(
                                onClick = { showOpenCashDialog = true },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Abrir Caixa", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showCloseCashDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Lock, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Fechar Caixa", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Resumo Financeiro (apenas se caixa aberto)
            if (currentSession != null) {
                // Cards de resumo
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Valor Inicial
                    SummaryCard(
                        title = "Valor Inicial",
                        value = currentSession?.initialAmount ?: 0.0,
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )

                    // Vendas
                    SummaryCard(
                        title = "Vendas",
                        value = totalVendas,
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )

                    // Reforços
                    SummaryCard(
                        title = "Reforços",
                        value = totalReforcos,
                        icon = Icons.Default.Add,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )

                    // Sangrias
                    SummaryCard(
                        title = "Sangrias",
                        value = totalSangrias,
                        icon = Icons.Default.Remove,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.weight(1f)
                    )

                    // Saldo Atual
                    Card(
                        modifier = Modifier.weight(1f),
                        elevation = 4.dp,
                        backgroundColor = Color(0xFF1976D2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AttachMoney, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Saldo Atual", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                "R$ %.2f".format(saldoAtual),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Botões de ação: Sangria e Reforço
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showDepositDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF9C27B0))
                    ) {
                        Icon(Icons.Default.AddCircle, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Reforço de Caixa", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showWithdrawalDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF5722))
                    ) {
                        Icon(Icons.Default.RemoveCircle, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Sangria", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Lista de Movimentações
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f).widthIn(max = maxWidthContent),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Movimentações do Caixa",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${movements.size} registro(s)",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }

                        Divider()

                        if (movements.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.Gray.copy(alpha = 0.5f)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text("Nenhuma movimentação ainda", color = Color.Gray, fontSize = 16.sp)
                                    Text("As vendas e operações aparecerão aqui", fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.padding(8.dp)) {
                                items(movements.reversed()) { movement ->
                                    MovementItem(movement)
                                    Divider(color = Color.Gray.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }
            } else {
                // Caixa fechado - mostrar mensagem
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PointOfSale,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp),
                                tint = Color.Gray.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Caixa Fechado",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Abra o caixa para começar a registrar vendas",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        if (outerScroll.maxValue > 0) {
            VerticalScrollbar(adapter = rememberScrollbarAdapter(outerScroll), modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp))
        }
    }

    // Dialog Abrir Caixa
    if (showOpenCashDialog) {
        var initialText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isLoading) showOpenCashDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockOpen, null, tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir Caixa", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.width(350.dp)) {
                    Text("Informe o valor inicial em dinheiro no caixa:")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = initialText,
                        onValueChange = { v ->
                            // sanitiza e formata imediatamente para exibição com 2 casas
                            val raw = sanitizeCurrencyInputRaw(v)
                            initialText = if (raw.isBlank()) "" else formatCurrencyDisplayFromRaw(raw)
                        },
                        label = { Text("Valor inicial (R$)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        val value = parseDisplayCurrency(initialText)
                        val user = UserSession.getCurrentUser()
                        val id = cashDao.openSession(user?.fullName ?: "Desconhecido", value)
                        if (id > 0) {
                            refreshTrigger++
                            scope.launch { snackbarHostState.showSnackbar("✓ Caixa aberto com sucesso!") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("✗ Erro ao abrir caixa") }
                        }
                        isLoading = false
                        showOpenCashDialog = false
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Abrir Caixa", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showOpenCashDialog = false },
                    enabled = !isLoading
                ) { Text("Cancelar") }
            }
        )
    }

    // Dialog Fechar Caixa
    if (showCloseCashDialog && currentSession != null) {
        val session = currentSession!!
        var closingText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        val expectedAmount = saldoAtual
        val countedAmount = parseDisplayCurrency(closingText)
        val difference = countedAmount - expectedAmount

        AlertDialog(
            onDismissRequest = { if (!isLoading) showCloseCashDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = Color(0xFFD32F2F))
                    Spacer(Modifier.width(8.dp))
                    Text("Fechar Caixa", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.width(400.dp)) {
                    // Resumo
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFF5F5F5),
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Valor inicial:")
                                Text("R$ %.2f".format(session.initialAmount), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vendas:", color = Color(0xFF4CAF50))
                                Text("+ R$ %.2f".format(totalVendas), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Reforços:", color = Color(0xFF9C27B0))
                                Text("+ R$ %.2f".format(totalReforcos), fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0))
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sangrias:", color = Color(0xFFFF5722))
                                Text("- R$ %.2f".format(totalSangrias), fontWeight = FontWeight.Bold, color = Color(0xFFFF5722))
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("VALOR ESPERADO:", fontWeight = FontWeight.Bold)
                                Text("R$ %.2f".format(expectedAmount), fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = closingText,
                        onValueChange = { v ->
                            val raw = sanitizeCurrencyInputRaw(v)
                            closingText = if (raw.isBlank()) "" else formatCurrencyDisplayFromRaw(raw)
                        },
                        label = { Text("Valor contado (R$)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) }
                    )

                    // Mostrar diferença se valor foi informado
                    if (closingText.isNotBlank() && countedAmount > 0) {
                        Spacer(Modifier.height(12.dp))
                        val diffColor = when {
                            difference > 0.01 -> Color(0xFF4CAF50)
                            difference < -0.01 -> Color(0xFFE53935)
                            else -> Color(0xFF2196F3)
                        }
                        val diffIcon = when {
                            difference > 0.01 -> Icons.Default.TrendingUp
                            difference < -0.01 -> Icons.Default.TrendingDown
                            else -> Icons.Default.Check
                        }
                        val diffText = when {
                            difference > 0.01 -> "Sobra: + R$ %.2f".format(difference)
                            difference < -0.01 -> "Falta: - R$ %.2f".format(-difference)
                            else -> "✓ Caixa confere!"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(diffIcon, null, tint = diffColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(diffText, color = diffColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        val counted = parseDisplayCurrency(closingText)
                        val closed = cashDao.closeSession(session.id, counted)
                        if (closed) {
                            refreshTrigger++
                            scope.launch { snackbarHostState.showSnackbar("✓ Caixa fechado com sucesso!") }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("✗ Erro ao fechar caixa") }
                        }
                        isLoading = false
                        showCloseCashDialog = false
                    },
                    enabled = !isLoading && closingText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Fechar Caixa", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCloseCashDialog = false },
                    enabled = !isLoading
                ) { Text("Cancelar") }
            }
        )
    }

    // Dialog Sangria
    if (showWithdrawalDialog && currentSession != null) {
        var amountText by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        val amount = parseDisplayCurrency(amountText)

        AlertDialog(
            onDismissRequest = { if (!isLoading) showWithdrawalDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RemoveCircle, null, tint = Color(0xFFFF5722))
                    Spacer(Modifier.width(8.dp))
                    Text("Sangria de Caixa", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.width(350.dp)) {
                    Text("Registre a retirada de valores do caixa.")
                    Spacer(Modifier.height(4.dp))
                    Text("Saldo atual: R$ %.2f".format(saldoAtual), color = Color.Gray, fontSize = 14.sp)

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { v ->
                            val raw = sanitizeCurrencyInputRaw(v)
                            amountText = if (raw.isBlank()) "" else formatCurrencyDisplayFromRaw(raw)
                        },
                        label = { Text("Valor da sangria (R$)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) },
                        isError = amount > saldoAtual
                    )

                    if (amount > saldoAtual) {
                        Text(
                            "⚠ Valor maior que o saldo disponível!",
                            color = Color(0xFFE53935),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Motivo/Descrição") },
                        placeholder = { Text("Ex: Pagamento fornecedor, troco, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        currentSession?.let { session ->
                            val success = cashDao.recordMovement(
                                session.id,
                                "WITHDRAWAL",
                                amount,
                                description.ifBlank { "Sangria" }
                            )
                            if (success) {
                                refreshTrigger++
                                scope.launch { snackbarHostState.showSnackbar("✓ Sangria de R$ %.2f registrada".format(amount)) }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("✗ Erro ao registrar sangria") }
                            }
                        }
                        isLoading = false
                        showWithdrawalDialog = false
                    },
                    enabled = !isLoading && amount > 0 && amount <= saldoAtual,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF5722))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Confirmar Sangria", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showWithdrawalDialog = false },
                    enabled = !isLoading
                ) { Text("Cancelar") }
            }
        )
    }

    // Dialog Reforço
    if (showDepositDialog && currentSession != null) {
        var amountText by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        val amount = parseDisplayCurrency(amountText)

        AlertDialog(
            onDismissRequest = { if (!isLoading) showDepositDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircle, null, tint = Color(0xFF9C27B0))
                    Spacer(Modifier.width(8.dp))
                    Text("Reforço de Caixa", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.width(350.dp)) {
                    Text("Registre a entrada de valores no caixa.")
                    Spacer(Modifier.height(4.dp))
                    Text("Saldo atual: R$ %.2f".format(saldoAtual), color = Color.Gray, fontSize = 14.sp)

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { v ->
                            val raw = sanitizeCurrencyInputRaw(v)
                            amountText = if (raw.isBlank()) "" else formatCurrencyDisplayFromRaw(raw)
                        },
                        label = { Text("Valor do reforço (R$)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) }
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Motivo/Descrição") },
                        placeholder = { Text("Ex: Troco adicional, reforço, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        currentSession?.let { session ->
                            val success = cashDao.recordMovement(
                                session.id,
                                "DEPOSIT",
                                amount,
                                description.ifBlank { "Reforço" }
                            )
                            if (success) {
                                refreshTrigger++
                                scope.launch { snackbarHostState.showSnackbar("✓ Reforço de R$ %.2f registrado".format(amount)) }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("✗ Erro ao registrar reforço") }
                            }
                        }
                        isLoading = false
                        showDepositDialog = false
                    },
                    enabled = !isLoading && amount > 0,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF9C27B0))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Confirmar Reforço", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDepositDialog = false },
                    enabled = !isLoading
                ) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = Color.Gray)
            Text(
                "R$ %.2f".format(value),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun MovementItem(movement: Map<String, Any>) {
    val type = movement["type"] as? String ?: ""
    val amount = movement["amount"] as? Double ?: 0.0
    val description = movement["description"] as? String
    val createdAt = movement["created_at"] as? String ?: ""

    val (icon, color, typeText, prefix) = when (type) {
        "SALE" -> listOf(Icons.Default.ShoppingCart, Color(0xFF4CAF50), "Venda", "+")
        "WITHDRAWAL" -> listOf(Icons.Default.RemoveCircle, Color(0xFFFF5722), "Sangria", "-")
        "DEPOSIT" -> listOf(Icons.Default.AddCircle, Color(0xFF9C27B0), "Reforço", "+")
        else -> listOf(Icons.Default.Receipt, Color.Gray, type, "")
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon as androidx.compose.ui.graphics.vector.ImageVector,
            null,
            tint = color as Color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(typeText as String, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            description?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 13.sp, color = Color.Gray)
            }
            Text(formatDateTime(createdAt), fontSize = 12.sp, color = Color.Gray)
        }
        Text(
            "$prefix R$ %.2f".format(amount),
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 16.sp
        )
    }
}

private fun formatDateTime(dateTimeStr: String): String {
    return try {
        val dt = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    } catch (e: Exception) {
        try {
            // Tentar formato alternativo
            val dt = LocalDateTime.parse(dateTimeStr.replace(" ", "T"))
            dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } catch (e2: Exception) {
            dateTimeStr
        }
    }
}
