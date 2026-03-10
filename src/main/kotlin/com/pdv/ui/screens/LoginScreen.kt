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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.UserDao
import com.pdv.data.UserSession
import com.pdv.data.Config
import com.pdv.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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

    // Connection choices (local/remote)
    var useRemote by remember { mutableStateOf(Config.clientUseRemote) }
    var hostInput by remember { mutableStateOf(Config.clientHost) }
    var portInput by remember { mutableStateOf(Config.clientPort.toString()) }

    val scrollState = rememberScrollState()

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

        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val maxCardWidth: Dp = if (this.maxWidth < 700.dp) this.maxWidth * 0.9f else 600.dp

            Card(
                modifier = Modifier
                    .widthIn(max = maxCardWidth)
                    .padding(24.dp),
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Row {
                    // Content area with vertical scroll
                    Box(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                        Column(
                            modifier = Modifier.padding(28.dp),
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

                            // Conexão (local / host custom)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Conexão", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = !useRemote, onClick = { useRemote = false })
                                    Text("Localhost", modifier = Modifier.padding(end = 12.dp))
                                    RadioButton(selected = useRemote, onClick = { useRemote = true })
                                    Text("Usar host/personalizado")
                                }

                                if (useRemote) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = hostInput, onValueChange = { hostInput = it }, label = { Text("Host (IP ou hostname)") }, modifier = Modifier.weight(1f), singleLine = true)
                                        OutlinedTextField(value = portInput, onValueChange = { portInput = it.filter { ch -> ch.isDigit() } }, label = { Text("Porta") }, modifier = Modifier.width(110.dp), singleLine = true)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(onClick = {
                                            // salvar escolha
                                            Config.clientUseRemote = true
                                            Config.clientHost = hostInput
                                            Config.clientPort = portInput.toIntOrNull() ?: Config.clientPort
                                        }) {
                                            Text("Salvar conexão")
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(onClick = {
                                            Config.clientUseRemote = false
                                            Config.clientHost = "localhost"
                                            Config.clientPort = 8080
                                            hostInput = Config.clientHost
                                            portInput = Config.clientPort.toString()
                                        }) {
                                            Text("Usar localhost")
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Botão de login
                            Button(
                                onClick = {
                                    // aplicar configurações de conexão antes de autenticar
                                    Config.clientUseRemote = useRemote
                                    Config.clientHost = hostInput
                                    Config.clientPort = portInput.toIntOrNull() ?: Config.clientPort

                                    // (Ainda autenticação local via UserDao; se futuramente quisermos autenticar via servidor remoto, adaptar aqui.)
                                    if (username.isBlank() || password.isBlank()) {
                                        errorMessage = "Preencha usuário e senha"
                                        return@Button
                                    }

                                    isLoading = true
                                    scope.launch {
                                        var user: com.pdv.data.User? = null
                                        // tentar autenticar remotamente se configurado
                                        if (Config.clientUseRemote) {
                                            try {
                                                val url = URL("http://${Config.clientHost}:${Config.clientPort}/api/auth")
                                                val conn = (url.openConnection() as HttpURLConnection).apply {
                                                    requestMethod = "POST"
                                                    doOutput = true
                                                    setRequestProperty("Content-Type", "application/json")
                                                }
                                                val payload = JSONObject().put("username", username).put("password", password).toString()
                                                conn.outputStream.use { it.write(payload.toByteArray()) }
                                                val code = conn.responseCode
                                                if (code in 200..299) {
                                                    val respText = conn.inputStream.bufferedReader().readText()
                                                    val jr = JSONObject(respText)
                                                    // construir um User mínimo local a partir da resposta
                                                    val id = jr.optLong("id", 0L)
                                                    val uname = jr.optString("username", username)
                                                    val fullName = jr.optString("fullName", uname)
                                                    val roleName = jr.optString("role", "CASHIER")
                                                    val role = try { com.pdv.data.UserRole.valueOf(roleName) } catch (t: Exception) { com.pdv.data.UserRole.CASHIER }
                                                    user = com.pdv.data.User(id = id, username = uname, password = password, fullName = fullName, role = role)
                                                } else {
                                                    // fallback: set error message but continue to local auth below
                                                    val err = try { conn.errorStream?.bufferedReader()?.readText() } catch (_: Exception) { "" }
                                                    println("✗ Autenticação remota falhou: HTTP $code - $err")
                                                }
                                            } catch (e: Exception) {
                                                println("✗ Erro ao contatar servidor remoto: ${e.message}")
                                            }
                                        }

                                        if (user == null) {
                                            // autenticação local
                                            val localUser = userDao.authenticate(username, password)
                                            if (localUser != null) user = localUser
                                        }

                                        isLoading = false

                                        if (user != null) {
                                            // login bem-sucedido
                                            com.pdv.data.UserSession.login(user)
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
            }

            // Optional scrollbar for wide screens
            if (scrollState.maxValue > 0) {
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scrollState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp)
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
