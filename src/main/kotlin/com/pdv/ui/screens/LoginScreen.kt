package com.pdv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.UserDao
import com.pdv.data.UserSession
import com.pdv.ui.theme.ThemeManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showHelpDialog by remember { mutableStateOf(false) }

    val userDao = remember { UserDao() }
    val scope = rememberCoroutineScope()
    val isDarkTheme = ThemeManager.isDarkTheme

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                if (isDarkTheme) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D47A1),
                            Color(0xFF1565C0),
                            Color(0xFF1976D2)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1976D2),
                            Color(0xFF1565C0),
                            Color(0xFF0D47A1)
                        )
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Botão de tema no canto superior direito
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { ThemeManager.toggleTheme() },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(
                    if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Tema Claro" else "Tema Escuro",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .width(450.dp)
                .padding(24.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo/Ícone
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "PDV",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colors.primary
                )

                Spacer(Modifier.height(16.dp))

                // Título
                Text(
                    "PDV Desktop",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )

                Text(
                    "Sistema de Ponto de Venda",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(32.dp))

                // Campo de usuário
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = ""
                    },
                    label = { Text("Usuário") },
                    placeholder = { Text("Digite seu usuário") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, "Usuário")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(Modifier.height(16.dp))

                // Campo de senha
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    label = { Text("Senha") },
                    placeholder = { Text("Digite sua senha") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, "Senha")
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                "Toggle senha"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                // Mensagem de erro
                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            errorMessage,
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Botão de login
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            errorMessage = "Preencha usuário e senha"
                            return@Button
                        }

                        isLoading = true
                        scope.launch {
                            val user = userDao.authenticate(username, password)
                            isLoading = false

                            if (user != null) {
                                UserSession.login(user)
                                println("✓ Login bem-sucedido: ${user.fullName} (${user.role.displayName})")
                                onLoginSuccess()
                            } else {
                                errorMessage = "Usuário ou senha incorretos"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.Login, "Login")
                        Spacer(Modifier.width(8.dp))
                        Text("ENTRAR", fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Botão de ajuda
                TextButton(
                    onClick = { showHelpDialog = true }
                ) {
                    Icon(
                        Icons.Default.Help,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Usuários de teste")
                }

                Spacer(Modifier.height(8.dp))

                // Versão
                Text(
                    "Versão 1.0.0",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }

    // Diálogo de ajuda
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colors.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Usuários de Teste")
                }
            },
            text = {
                Column {
                    Text("Você pode usar os seguintes usuários para teste:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    UserHelpItem("admin", "admin123", "Administrador", "Acesso total ao sistema")
                    Divider(Modifier.padding(vertical = 8.dp))

                    UserHelpItem("gerente", "gerente123", "Gerente", "Vendas, produtos e relatórios")
                    Divider(Modifier.padding(vertical = 8.dp))

                    UserHelpItem("caixa1", "caixa123", "Caixa", "Apenas vendas")
                    Divider(Modifier.padding(vertical = 8.dp))

                    UserHelpItem("estoque", "estoque123", "Estoquista", "Gerenciar produtos")
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Entendi")
                }
            }
        )
    }
}

@Composable
private fun UserHelpItem(username: String, password: String, role: String, description: String) {
    Column {
        Row {
            Text("Usuário: ", color = Color.Gray, fontSize = 14.sp)
            Text(username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Row {
            Text("Senha: ", color = Color.Gray, fontSize = 14.sp)
            Text(password, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Row {
            Text("Cargo: ", color = Color.Gray, fontSize = 14.sp)
            Text(role, color = MaterialTheme.colors.primary, fontSize = 14.sp)
        }
        Text(description, fontSize = 12.sp, color = Color.Gray)
    }
}

