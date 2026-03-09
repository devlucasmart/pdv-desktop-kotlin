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
import com.pdv.data.*
import kotlinx.coroutines.launch

@Composable
fun UsersScreen(snackbarHostState: SnackbarHostState) {
    val userDao = remember { UserDao() }
    var users by remember { mutableStateOf(userDao.findAll()) }
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Verificar permissão
    if (!UserSession.hasPermission(Permission.MANAGE_USERS)) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Acesso Negado",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Você não tem permissão para gerenciar usuários",
                    color = Color.Gray
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Gerenciar Usuários",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Controle de acesso e permissões",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Icon(Icons.Default.PersonAdd, "Adicionar")
                Spacer(Modifier.width(4.dp))
                Text("Novo Usuário")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Lista de usuários
        Card(modifier = Modifier.weight(1f).fillMaxWidth(), elevation = 2.dp) {
            if (users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum usuário cadastrado", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(users) { user ->
                        UserCard(
                            user = user,
                            onDelete = {
                                userDao.delete(user.username)
                                users = userDao.findAll()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Usuário removido")
                                }
                            },
                            onResetPassword = {
                                userDao.changePassword(user.username, "123456")
                                scope.launch {
                                    snackbarHostState.showSnackbar("Senha resetada para: 123456")
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
        UserDialog(
            onDismiss = { showDialog = false },
            onSave = { user ->
                userDao.save(user)
                users = userDao.findAll()
                showDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Usuário criado com sucesso!")
                }
            }
        )
    }
}

@Composable
fun UserCard(
    user: User,
    onDelete: () -> Unit,
    onResetPassword: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do cargo
            Surface(
                modifier = Modifier.size(50.dp),
                shape = MaterialTheme.shapes.medium,
                color = when (user.role) {
                    UserRole.ADMIN -> Color(0xFFE3F2FD)
                    UserRole.MANAGER -> Color(0xFFFFF3E0)
                    UserRole.CASHIER -> Color(0xFFE8F5E9)
                    UserRole.STOCK -> Color(0xFFF3E5F5)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (user.role) {
                            UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                            UserRole.MANAGER -> Icons.Default.ManageAccounts
                            UserRole.CASHIER -> Icons.Default.PointOfSale
                            UserRole.STOCK -> Icons.Default.Inventory
                        },
                        contentDescription = null,
                        tint = when (user.role) {
                            UserRole.ADMIN -> Color(0xFF1976D2)
                            UserRole.MANAGER -> Color(0xFFFF9800)
                            UserRole.CASHIER -> Color(0xFF4CAF50)
                            UserRole.STOCK -> Color(0xFF9C27B0)
                        }
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("@${user.username}", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.width(16.dp))
                    Chip(user.role.displayName, user.role)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Permissões: ${user.role.permissions.size}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row {
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Default.LockReset, "Resetar senha", tint = Color(0xFFFF9800))
                    }
                    if (user.username != UserSession.getCurrentUser()?.username) {
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
            text = { Text("Deseja realmente remover o usuário '${user.fullName}'?") },
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

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Resetar Senha") },
            text = { Text("A senha será redefinida para: 123456\n\nO usuário deverá alterá-la no próximo login.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetPassword()
                        showResetConfirm = false
                    }
                ) {
                    Text("Resetar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun Chip(text: String, role: UserRole) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when (role) {
            UserRole.ADMIN -> Color(0xFF1976D2)
            UserRole.MANAGER -> Color(0xFFFF9800)
            UserRole.CASHIER -> Color(0xFF4CAF50)
            UserRole.STOCK -> Color(0xFF9C27B0)
        }
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UserDialog(onDismiss: () -> Unit, onSave: (User) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.CASHIER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Usuário") },
        text = {
            Column(modifier = Modifier.width(400.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuário *") },
                    placeholder = { Text("Ex: joao.silva") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha *") },
                    placeholder = { Text("Mínimo 6 caracteres") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nome Completo *") },
                    placeholder = { Text("João Silva") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))

                Text("Cargo:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                UserRole.values().forEach { role ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role }
                        )
                        Column {
                            Text(role.displayName, fontWeight = FontWeight.Bold)
                            Text(
                                "${role.permissions.size} permissões",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val user = User(
                        username = username.trim(),
                        password = password,
                        fullName = fullName.trim(),
                        role = selectedRole
                    )
                    onSave(user)
                },
                enabled = username.isNotBlank() && password.length >= 6 && fullName.isNotBlank()
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

