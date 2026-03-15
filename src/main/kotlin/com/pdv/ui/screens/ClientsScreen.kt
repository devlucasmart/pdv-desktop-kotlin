package com.pdv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.*
import kotlinx.coroutines.launch

// ── data class local para exibir dívida com estado mutável ──────────────
private data class DebtEntry(
    val id: Long,
    val saleId: Long?,
    val description: String,
    val amountDue: Double,
    var amountPaid: Double,
    val status: String,
    val createdAt: String
) {
    val remaining get() = (amountDue - amountPaid).coerceAtLeast(0.0)
    val isPaid get() = remaining <= 0.0
}

private fun Map<String, Any?>.toDebtEntry() = DebtEntry(
    id          = this["id"] as Long,
    saleId      = this["sale_id"] as? Long,
    description = this["description"] as? String ?: "",
    amountDue   = this["amount_due"] as Double,
    amountPaid  = this["amount_paid"] as Double,
    status      = this["status"] as? String ?: "OPEN",
    createdAt   = this["created_at"] as? String ?: ""
)

@Composable
fun ClientsScreen(snackbarHostState: SnackbarHostState) {
    val dao      = remember { ClientDao() }
    val debtDao  = remember { ClientDebtDao() }
    val scope    = rememberCoroutineScope()

    var clients        by remember { mutableStateOf(dao.findAll()) }
    var searchTerm     by remember { mutableStateOf("") }
    var showForm       by remember { mutableStateOf(false) }
    var editingClient  by remember { mutableStateOf<Client?>(null) }
    var debtClient     by remember { mutableStateOf<Client?>(null) }

    fun reload() { clients = if (searchTerm.isBlank()) dao.findAll() else dao.search(searchTerm) }

    // ── layout principal ────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // ── cabeçalho ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, null,
                    tint = MaterialTheme.colors.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Clientes", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
                    Text("${clients.size} cadastrados", fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
            Button(
                onClick = { editingClient = null; showForm = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Novo Cliente")
            }
        }

        // ── busca ──────────────────────────────────────────────────────
        OutlinedTextField(
            value = searchTerm,
            onValueChange = { v ->
                searchTerm = v
                clients = if (v.isBlank()) dao.findAll() else dao.search(v)
            },
            label = { Text("Buscar por nome ou documento") },
            leadingIcon = { Icon(Icons.Default.Search, null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)) },
            trailingIcon = {
                if (searchTerm.isNotBlank())
                    IconButton(onClick = { searchTerm = ""; clients = dao.findAll() }) {
                        Icon(Icons.Default.Clear, null)
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(Modifier.height(12.dp))

        // ── lista de clientes ──────────────────────────────────────────
        if (clients.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PersonOff, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhum cliente encontrado", fontSize = 18.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clients, key = { it.id }) { c ->
                    ClientCard(
                        client      = c,
                        debtDao     = debtDao,
                        onEdit      = { editingClient = c; showForm = true },
                        onDelete    = {
                            if (UserSession.isAdmin()) {
                                dao.delete(c.id)
                                reload()
                                scope.launch { snackbarHostState.showSnackbar("✓ Cliente desativado") }
                            }
                        },
                        onDebts     = { debtClient = c }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    // ── diálogo de formulário ─────────────────────────────────────────
    if (showForm) {
        ClientFormDialog(
            initial = editingClient,
            onDismiss = { showForm = false },
            onSave = { client ->
                if (editingClient == null) {
                    val id = dao.save(client)
                    if (id > 0) scope.launch { snackbarHostState.showSnackbar("✓ Cliente criado") }
                } else {
                    val ok = dao.update(client.copy(id = editingClient!!.id))
                    if (ok) scope.launch { snackbarHostState.showSnackbar("✓ Cliente atualizado") }
                }
                reload()
                showForm = false
            }
        )
    }

    // ── diálogo de dívidas ───────────────────────────────────────────
    debtClient?.let { c ->
        DebtManagementDialog(
            client      = c,
            debtDao     = debtDao,
            onDismiss   = { debtClient = null },
            onPayment   = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────
// Card individual do cliente
// ────────────────────────────────────────────────────────────────────────
@Composable
private fun ClientCard(
    client   : Client,
    debtDao  : ClientDebtDao,
    onEdit   : () -> Unit,
    onDelete : () -> Unit,
    onDebts  : () -> Unit
) {
    val debts        = remember(client.id) { debtDao.findOpenByClient(client.id).map { it.toDebtEntry() } }
    val totalPending = debts.filter { !it.isPaid }.sumOf { it.remaining }
    val hasDebt      = totalPending > 0.0

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape     = RoundedCornerShape(10.dp),
        backgroundColor = if (hasDebt)
            MaterialTheme.colors.error.copy(alpha = 0.04f)
        else
            MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── avatar ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = client.name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            // ── dados do cliente ────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(client.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (!client.document.isNullOrBlank())
                    Text(client.document, fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                if (!client.phone.isNullOrBlank())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.width(3.dp))
                        Text(client.phone, fontSize = 11.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                if (client.defaultDiscountPercent > 0)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colors.secondary)
                        Spacer(Modifier.width(3.dp))
                        Text("Desconto: ${client.defaultDiscountPercent}%",
                            fontSize = 11.sp, color = MaterialTheme.colors.secondary)
                    }
            }

            // ── badge de dívida ─────────────────────────────────────
            if (hasDebt) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colors.error.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .clickable { onDebts() }
                ) {
                    Text("PENDENTE", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.error)
                    Text("R$ ${"%.2f".format(totalPending)}", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colors.error)
                }
                Spacer(Modifier.width(8.dp))
            }

            // ── ações ────────────────────────────────────────────────
            Row {
                // Botão de dívidas (sempre visível)
                IconButton(onClick = onDebts) {
                    Icon(
                        Icons.Default.AccountBalance, null,
                        tint = if (hasDebt) MaterialTheme.colors.error
                               else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null,
                        tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
                }
                if (UserSession.isAdmin()) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colors.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Diálogo de formulário (criar / editar)
// ────────────────────────────────────────────────────────────────────────
@Composable
private fun ClientFormDialog(
    initial  : Client?,
    onDismiss: () -> Unit,
    onSave   : (Client) -> Unit
) {
    var name            by remember { mutableStateOf(initial?.name ?: "") }
    var document        by remember { mutableStateOf(initial?.document ?: "") }
    var phone           by remember { mutableStateOf(initial?.phone ?: "") }
    var email           by remember { mutableStateOf(initial?.email ?: "") }
    var address         by remember { mutableStateOf(initial?.address ?: "") }
    var discountPercent by remember { mutableStateOf(initial?.defaultDiscountPercent?.toString() ?: "0") }
    var nameError       by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (initial == null) Icons.Default.PersonAdd else Icons.Default.Edit,
                    null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (initial == null) "Novo Cliente" else "Editar Cliente",
                    fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false },
                    label = { Text("Nome *") }, modifier = Modifier.fillMaxWidth(),
                    isError = nameError, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )
                if (nameError)
                    Text("Nome é obrigatório", color = MaterialTheme.colors.error, fontSize = 11.sp)

                OutlinedTextField(
                    value = document, onValueChange = { document = it },
                    label = { Text("CPF / CNPJ") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, leadingIcon = { Icon(Icons.Default.Badge, null) }
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, leadingIcon = { Icon(Icons.Default.Phone, null) }
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, leadingIcon = { Icon(Icons.Default.Email, null) }
                )
                OutlinedTextField(
                    value = address, onValueChange = { address = it },
                    label = { Text("Endereço") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                )
                OutlinedTextField(
                    value = discountPercent,
                    onValueChange = { discountPercent = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Desconto padrão (%)") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, leadingIcon = { Icon(Icons.Default.LocalOffer, null) }
                )
            }
        },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    onSave(Client(
                        name                 = name.trim(),
                        document             = document.ifBlank { null },
                        phone                = phone.ifBlank { null },
                        email                = email.ifBlank { null },
                        address              = address.ifBlank { null },
                        defaultDiscountPercent = discountPercent.toDoubleOrNull() ?: 0.0
                    ))
                }
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ────────────────────────────────────────────────────────────────────────
// Diálogo de gestão de dívidas
// ────────────────────────────────────────────────────────────────────────
@Composable
private fun DebtManagementDialog(
    client   : Client,
    debtDao  : ClientDebtDao,
    onDismiss: () -> Unit,
    onPayment: (String) -> Unit
) {
    var debts by remember(client.id) {
        mutableStateOf(debtDao.findOpenByClient(client.id).map { it.toDebtEntry() })
    }
    val openDebts   = debts.filter { !it.isPaid }
    val closedDebts = debts.filter { it.isPaid }
    val totalPending = openDebts.sumOf { it.remaining }

    // estado do pagamento parcial
    var payingDebt    by remember { mutableStateOf<DebtEntry?>(null) }
    var partialAmount by remember { mutableStateOf("") }
    var partialError  by remember { mutableStateOf("") }

    fun reload() { debts = debtDao.findOpenByClient(client.id).map { it.toDebtEntry() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(14.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, null,
                        tint = MaterialTheme.colors.primary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Contas de ${client.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${openDebts.size} pendente(s)", fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                }

                if (totalPending > 0) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp), elevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total pendente", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("R$ ${"%.2f".format(totalPending)}",
                                fontWeight = FontWeight.Bold, fontSize = 18.sp,
                                color = MaterialTheme.colors.error)
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
            ) {
                // ── dívidas abertas ─────────────────────────────────
                if (openDebts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp),
                                tint = Color(0xFF4CAF50))
                            Spacer(Modifier.height(8.dp))
                            Text("Sem dívidas pendentes!", color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    Text("Pendentes", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = MaterialTheme.colors.error, modifier = Modifier.padding(bottom = 6.dp))

                    openDebts.forEach { debt ->
                        DebtRow(
                            debt = debt,
                            onPayFull = {
                                val ok = debtDao.recordPayment(debt.id, debt.remaining)
                                val msg = if (ok) "✓ Quitado: R$ ${"%.2f".format(debt.remaining)}"
                                          else "✗ Falha ao registrar pagamento"
                                onPayment(msg)
                                reload()
                            },
                            onPayPartial = { payingDebt = debt; partialAmount = ""; partialError = "" }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                // ── dívidas pagas (colapsadas) ──────────────────────
                if (closedDebts.isNotEmpty()) {
                    var showClosed by remember { mutableStateOf(false) }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showClosed = !showClosed }.padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pagas (${closedDebts.size})", fontSize = 13.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        Icon(
                            if (showClosed) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    if (showClosed) {
                        closedDebts.forEach { debt ->
                            ClosedDebtRow(debt)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )

    // ── sub-diálogo: pagamento parcial ─────────────────────────────────
    payingDebt?.let { debt ->
        AlertDialog(
            onDismissRequest = { payingDebt = null },
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, null,
                        tint = MaterialTheme.colors.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar Pagamento", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // info da dívida
                    Card(
                        backgroundColor = MaterialTheme.colors.surface,
                        elevation = 1.dp, shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            if (debt.description.isNotBlank())
                                Text(debt.description, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total devido:", fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                                Text("R$ ${"%.2f".format(debt.amountDue)}", fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Já pago:", fontSize = 13.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                                Text("R$ ${"%.2f".format(debt.amountPaid)}", fontSize = 13.sp)
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Restante:", fontWeight = FontWeight.Bold)
                                Text("R$ ${"%.2f".format(debt.remaining)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.error)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = partialAmount,
                        onValueChange = {
                            partialAmount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                .replace(',', '.')
                            partialError = ""
                        },
                        label = { Text("Valor a pagar (R$)") },
                        leadingIcon = { Text("R$", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp)) },
                        placeholder = { Text("${"%.2f".format(debt.remaining)}") },
                        isError = partialError.isNotBlank(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (partialError.isNotBlank())
                        Text(partialError, color = MaterialTheme.colors.error, fontSize = 11.sp)

                    // botão atalho: pagar tudo
                    OutlinedButton(
                        onClick = { partialAmount = "%.2f".format(debt.remaining); partialError = "" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pagar total (R$ ${"%.2f".format(debt.remaining)})")
                    }
                }
            },
            confirmButton = {
                Button(
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        val v = partialAmount.replace(',', '.').toDoubleOrNull()
                        when {
                            v == null || v <= 0.0 -> partialError = "Valor inválido"
                            v > debt.remaining    -> partialError = "Valor maior que o saldo (R$ ${"%.2f".format(debt.remaining)})"
                            else -> {
                                val ok = debtDao.recordPayment(debt.id, v)
                                val msg = if (ok) "✓ Pagamento de R$ ${"%.2f".format(v)} registrado"
                                          else "✗ Falha ao registrar pagamento"
                                onPayment(msg)
                                payingDebt = null
                                reload()
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { payingDebt = null }) { Text("Cancelar") }
            }
        )
    }
}

// ── linha de dívida aberta ───────────────────────────────────────────────
@Composable
private fun DebtRow(
    debt       : DebtEntry,
    onPayFull  : () -> Unit,
    onPayPartial: () -> Unit
) {
    Card(
        elevation = 1.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (debt.description.isNotBlank())
                        Text(debt.description, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                            maxLines = 2)
                    if (debt.saleId != null)
                        Text("Venda #${debt.saleId}", fontSize = 11.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                    Text(debt.createdAt.take(10), fontSize = 10.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("R$ ${"%.2f".format(debt.remaining)}",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = MaterialTheme.colors.error)
                    if (debt.amountPaid > 0)
                        Text("pago R$ ${"%.2f".format(debt.amountPaid)}", fontSize = 10.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(8.dp))

            // barra de progresso de pagamento
            if (debt.amountDue > 0) {
                val progress = (debt.amountPaid / debt.amountDue).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF4CAF50),
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPayPartial,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.Payments, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Parcial", fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onPayFull,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(14.dp),
                        tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Quitar", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

// ── linha de dívida quitada ──────────────────────────────────────────────
@Composable
private fun ClosedDebtRow(debt: DebtEntry) {
    Card(
        elevation = 0.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp),
                    tint = Color(0xFF4CAF50))
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(debt.description.ifBlank { "Venda #${debt.saleId ?: debt.id}" },
                        fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    Text(debt.createdAt.take(10), fontSize = 10.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
                }
            }
            Text("R$ ${"%.2f".format(debt.amountDue)}", fontSize = 13.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
        }
    }
}
