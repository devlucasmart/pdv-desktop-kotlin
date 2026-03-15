package com.pdv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.PaymentMethod
import com.pdv.util.CurrencyUtils
import kotlin.math.max
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.launch

@Composable
fun PaymentDialog(
    total: Double,
    onDismiss: () -> Unit,
    onConfirm: (paymentMethod: String, receivedAmount: Double?) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.DINHEIRO) }
    var receivedText by remember { mutableStateOf("") }

    // Use CurrencyUtils.parse para converter entrada para Double
    val receivedAmount = CurrencyUtils.parse(receivedText)
    val change = max(0.0, receivedAmount - total)
    val canConfirm = when (selectedMethod) {
        PaymentMethod.DINHEIRO -> receivedAmount >= total
        else -> true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Finalizar Pagamento", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(modifier = Modifier.widthIn(max = 520.dp)) {
                // Total a pagar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colors.primary,
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL A PAGAR", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text(
                            CurrencyUtils.format(total),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Métodos de pagamento
                Text("Forma de Pagamento", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.values().forEach { method ->
                        PaymentMethodCard(
                            method = method,
                            isSelected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Campo de valor recebido (apenas para dinheiro)
                if (selectedMethod == PaymentMethod.DINHEIRO) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.surface,
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Valor Recebido", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))

                            // Calcular uma string de exibição a partir do texto cru
                            val displayReceived = if (receivedText.isBlank()) "" else CurrencyUtils.formatPlain(CurrencyUtils.parse(receivedText))

                            OutlinedTextField(
                                value = displayReceived,
                                onValueChange = { v ->
                                    // Guardar entrada crua; aceitar apenas dígitos, '.' e ','
                                    receivedText = v.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }
                                },
                                placeholder = { Text("0,00") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Text("R$", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    backgroundColor = Color.White
                                )
                            )

                            Spacer(Modifier.height(12.dp))

                            // Atalhos de valores
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(total, 20.0, 50.0, 100.0, 200.0).forEach { value ->
                                    OutlinedButton(
                                        onClick = { receivedText = "%.2f".format(value).replace('.', ',') },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text(
                                            if (value == total) "Exato" else "R$ ${value.toInt()}",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Troco
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TROCO:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    CurrencyUtils.format(change),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (change > 0) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.9f)
                                )
                            }

                            if (receivedAmount > 0 && receivedAmount < total) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "⚠ Valor insuficiente! Faltam ${CurrencyUtils.format(total - receivedAmount)}",
                                    color = MaterialTheme.colors.error,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    // Confirmação para outros métodos
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.surface,
                        elevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Pagamento via ${selectedMethod.displayName}",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val received = if (selectedMethod == PaymentMethod.DINHEIRO) CurrencyUtils.parse(receivedText) else null
                    onConfirm(selectedMethod.name, if (received == 0.0) null else received)
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Confirmar Pagamento", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    )
}

// NOTE: we will provide a new composable below with the actual implementation and a clearer signature.
@Composable
fun PaymentDialogSplit(
    total: Double,
    onDismiss: () -> Unit,
    onConfirm: (payments: List<Pair<String, Double>>) -> Unit,
    chargeToAccount: Boolean = false,
    clientName: String? = null
){
    // payments: list of pairs (paymentMethodName, amount)
    var selectedMethod by remember { mutableStateOf(PaymentMethod.DINHEIRO) }
    var amountText by remember { mutableStateOf("") }
    val parsedAmount = CurrencyUtils.parse(amountText)
    val amountFocusRequester = remember { FocusRequester() }
    val dialogScope = rememberCoroutineScope()

    // Keep a list of payment parts
    val parts = remember { mutableStateListOf<Pair<PaymentMethod, Double>>() }

    fun partsSum(): Double = parts.sumOf { it.second }
    val remaining = (total - partsSum()).coerceAtLeast(0.0)

    // request focus when dialog opens
    LaunchedEffect(Unit) {
        try { amountFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Finalizar Pagamento", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(modifier = Modifier.widthIn(max = 640.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.R -> {
                                // preencher restante
                                if (remaining > 0.0) {
                                    amountText = "%.2f".format(remaining).replace('.', ',')
                                }
                                true
                            }
                            Key.Enter -> {
                                // adicionar parte se valor válido
                                val amt = CurrencyUtils.parse(amountText)
                                if (amt > 0.0) {
                                    parts.add(selectedMethod to amt)
                                    amountText = ""
                                    // refocar
                                    dialogScope.launch { try { amountFocusRequester.requestFocus() } catch (_: Exception) {} }
                                }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
             ) {
                // Total a pagar - destaque grande e moderno
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colors.primary,
                    elevation = 6.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL A PAGAR", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(CurrencyUtils.format(total), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Partes adicionadas em um card
                Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pagamentos adicionados", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))

                        if (parts.isEmpty()) {
                            Text("Nenhuma parte adicionada.", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                parts.forEachIndexed { idx, p ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(p.first.displayName, fontWeight = FontWeight.Medium)
                                            Text(CurrencyUtils.format(p.second), color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))
                                        }
                                        IconButton(onClick = { parts.removeAt(idx) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remover parte")
                                        }
                                    }
                                    if (idx < parts.lastIndex) Divider()
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total pago:", fontWeight = FontWeight.Bold)
                            Text(CurrencyUtils.format(partsSum()), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Restante:", fontWeight = FontWeight.Bold)
                            Text(CurrencyUtils.format(remaining), fontWeight = FontWeight.Bold, color = if (remaining > 0) MaterialTheme.colors.error else MaterialTheme.colors.primary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Selector de métodos com estilo - usar cards compactos
                Text("Forma de pagamento", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    val methods = PaymentMethod.values().toList()
                    // layout em duas linhas quando necessário
                    methods.chunked(3).forEach { rowMethods ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowMethods.forEach { method ->
                                Box(modifier = Modifier.weight(1f)) {
                                    PaymentMethodCard(method = method, isSelected = selectedMethod == method, onClick = { selectedMethod = method }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            if (rowMethods.size < 3) {
                                repeat(3 - rowMethods.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Campo de valor e botões rápidos
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = if (amountText.isBlank()) "" else CurrencyUtils.formatPlain(parsedAmount),
                        onValueChange = { v -> amountText = v.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
                        label = { Text("Valor") },
                        singleLine = true,
                        leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).focusRequester(amountFocusRequester)
                    )

                    Column(modifier = Modifier.width(220.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // botões rápidos menores e consistentes
                            listOf(remaining, 20.0, 50.0, 100.0).forEach { v ->
                                val isRest = v == remaining && remaining > 0.0
                                Button(
                                    onClick = { amountText = "%.2f".format(v).replace('.', ',') },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(6.dp),
                                    colors = if (isRest) ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary, contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(if (v == remaining) "Restante" else "R$ ${v.toInt()}")
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            val amt = CurrencyUtils.parse(amountText).coerceAtLeast(0.0)
                            if (amt > 0.0) {
                                parts.add(selectedMethod to amt)
                                amountText = ""
                                // refocar no campo de valor
                                dialogScope.launch { try { amountFocusRequester.requestFocus() } catch (_: Exception) {} }
                            }
                        }, enabled = CurrencyUtils.parse(amountText) > 0.0, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar Parte")
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Dica: adicione a parte em dinheiro primeiro para que o troco seja calculado no caixa.", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
            }
        },
        confirmButton = {
            val sum = parts.sumOf { it.second }
            // Se chargeToAccount, pode confirmar mesmo sem pagar (vai na conta do cliente)
            val can = if (chargeToAccount) true else sum >= total
            Column {
                // Banner de aviso quando vai para conta
                if (chargeToAccount && clientName != null) {
                    Card(
                        backgroundColor = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        elevation = 0.dp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalance, null,
                                tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("Lançar na conta de $clientName",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100))
                                val pago = parts.sumOf { it.second }
                                val restante = (total - pago).coerceAtLeast(0.0)
                                if (restante > 0)
                                    Text("R$ ${"%.2f".format(restante)} ficará em aberto",
                                        fontSize = 11.sp, color = Color(0xFFBF360C))
                                else
                                    Text("Venda inteira será registrada no histórico",
                                        fontSize = 11.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
                Button(onClick = {
                    val out = parts.map { it.first.name to it.second }
                    onConfirm(out)
                }, enabled = can, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp))
                    Text(if (chargeToAccount && parts.sumOf { it.second } < total)
                        "Confirmar (restante vai para conta)"
                    else "Confirmar Pagamentos")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (method) {
        PaymentMethod.DINHEIRO -> Icons.Default.Money
        PaymentMethod.CARTAO_DEBITO -> Icons.Default.CreditCard
        PaymentMethod.CARTAO_CREDITO -> Icons.Default.CreditCard
        PaymentMethod.PIX -> Icons.Default.QrCode
        PaymentMethod.OUTROS -> Icons.Default.MoreHoriz
    }

    val backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent
    val borderColor = if (isSelected) MaterialTheme.colors.primary else Color.Gray.copy(alpha = 0.3f)
    val contentColor = if (isSelected) MaterialTheme.colors.primary else Color.Gray

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = method.displayName,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            method.displayName.replace(" ", "\n"),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
