package com.pdv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.*
import kotlinx.coroutines.launch

@Composable
fun ClientsScreen(snackbarHostState: SnackbarHostState) {
    val dao = remember { ClientDao() }
    var clients by remember { mutableStateOf(dao.findAll()) }
    var showForm by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<Client?>(null) }
    var searchTerm by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Clientes", style = MaterialTheme.typography.h4)
            Row {
                OutlinedButton(onClick = { showForm = true; editingClient = null }) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Novo")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(value = searchTerm, onValueChange = { v -> searchTerm = v; clients = if (v.isBlank()) dao.findAll() else dao.search(v) }, label = { Text("Buscar cliente") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))

        Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(clients) { c ->
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text(c.document ?: "", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        }
                        Row {
                            IconButton(onClick = { editingClient = c; showForm = true }) {
                                Icon(Icons.Default.Edit, null)
                            }
                            IconButton(onClick = {
                                if (UserSession.isAdmin()) {
                                    dao.delete(c.id)
                                    clients = dao.findAll()
                                    scope.launch { snackbarHostState.showSnackbar("✓ Cliente desativado") }
                                }
                            }) {
                                Icon(Icons.Default.Delete, null)
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }

    if (showForm) {
        var name by remember { mutableStateOf(editingClient?.name ?: "") }
        var document by remember { mutableStateOf(editingClient?.document ?: "") }
        var phone by remember { mutableStateOf(editingClient?.phone ?: "") }
        var email by remember { mutableStateOf(editingClient?.email ?: "") }
        var address by remember { mutableStateOf(editingClient?.address ?: "") }
        var discountPercent by remember { mutableStateOf(editingClient?.defaultDiscountPercent?.toString() ?: "0") }

        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text(if (editingClient == null) "Novo Cliente" else "Editar Cliente") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") })
                    OutlinedTextField(value = document, onValueChange = { document = it }, label = { Text("Documento") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefone") })
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Endereço") })
                    OutlinedTextField(value = discountPercent, onValueChange = { discountPercent = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Desconto padrão (%)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isBlank()) return@Button
                    val dp = discountPercent.toDoubleOrNull() ?: 0.0
                    if (editingClient == null) {
                        val id = dao.save(Client(name = name, document = document.ifBlank { null }, phone = phone.ifBlank { null }, email = email.ifBlank { null }, address = address.ifBlank { null }, defaultDiscountPercent = dp))
                        if (id > 0) scope.launch { snackbarHostState.showSnackbar("✓ Cliente criado") }
                    } else {
                        val updated = dao.update(editingClient!!.copy(name = name, document = document.ifBlank { null }, phone = phone.ifBlank { null }, email = email.ifBlank { null }, address = address.ifBlank { null }, defaultDiscountPercent = dp))
                        if (updated) scope.launch { snackbarHostState.showSnackbar("✓ Cliente atualizado") }
                    }
                    clients = dao.findAll()
                    showForm = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showForm = false }) { Text("Cancelar") } }
        )
    }
}
