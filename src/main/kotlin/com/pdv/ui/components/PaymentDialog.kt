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
