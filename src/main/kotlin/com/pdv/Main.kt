package com.pdv

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pdv.data.*
import com.pdv.ui.screens.*
import com.pdv.ui.theme.ThemeManager
import com.pdv.server.EmbeddedServer
import com.pdv.data.Config
import com.pdv.sync.SyncService
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

enum class Screen {
    VENDAS, CAIXA, PRODUTOS, RELATORIOS, CONFIGURACOES, USUARIOS
}

@Composable
@Preview
fun App() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(Screen.VENDAS) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Estado do tema (reativo)
    val isDarkTheme = ThemeManager.isDarkTheme

    MaterialTheme(
        colors = if (isDarkTheme) ThemeManager.getDarkColors() else ThemeManager.getLightColors()
    ) {
        if (!isLoggedIn) {
            LoginScreen(
                onLoginSuccess = {
                    isLoggedIn = true
                    // Redirecionar para tela apropriada baseado no cargo
                    currentScreen = when {
                        UserSession.hasPermission(Permission.MAKE_SALES) -> Screen.VENDAS
                        UserSession.hasPermission(Permission.VIEW_PRODUCTS) -> Screen.PRODUTOS
                        UserSession.hasPermission(Permission.VIEW_REPORTS) -> Screen.RELATORIOS
                        else -> Screen.CONFIGURACOES
                    }
                }
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Menu lateral
                NavigationRail(
                    modifier = Modifier.width(90.dp).fillMaxHeight(),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 4.dp
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Vendas
                    if (UserSession.hasPermission(Permission.VIEW_SALES)) {
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = "Vendas",
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = { Text("Vendas") },
                            selected = currentScreen == Screen.VENDAS,
                            onClick = { currentScreen = Screen.VENDAS }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Caixa
                    if (UserSession.hasPermission(Permission.MAKE_SALES)) {
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    Icons.Default.PointOfSale,
                                    contentDescription = "Caixa",
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = { Text("Caixa") },
                            selected = currentScreen == Screen.CAIXA,
                            onClick = { currentScreen = Screen.CAIXA }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Produtos
                    if (UserSession.hasPermission(Permission.VIEW_PRODUCTS)) {
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = "Produtos",
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = { Text("Produtos") },
                            selected = currentScreen == Screen.PRODUTOS,
                            onClick = { currentScreen = Screen.PRODUTOS }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Relatórios
                    if (UserSession.hasPermission(Permission.VIEW_REPORTS)) {
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    Icons.Default.Assessment,
                                    contentDescription = "Relatórios",
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = { Text("Relatórios") },
                            selected = currentScreen == Screen.RELATORIOS,
                            onClick = { currentScreen = Screen.RELATORIOS }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.weight(1f))

                    // Usuários (apenas admin)
                    if (UserSession.hasPermission(Permission.MANAGE_USERS)) {
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = "Usuários",
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = { Text("Usuários") },
                            selected = currentScreen == Screen.USUARIOS,
                            onClick = { currentScreen = Screen.USUARIOS }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Config
                    NavigationRailItem(
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Config",
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = { Text("Config") },
                        selected = currentScreen == Screen.CONFIGURACOES,
                        onClick = { currentScreen = Screen.CONFIGURACOES }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Logout
                    NavigationRailItem(
                        icon = {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Sair",
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = { Text("Sair") },
                        selected = false,
                        onClick = {
                            UserSession.logout()
                            isLoggedIn = false
                        }
                    )

                    Spacer(Modifier.height(16.dp))
                }

                // Conteúdo principal
                Column(modifier = Modifier.fillMaxSize()) {
                    // Barra superior com informações do usuário
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        elevation = 4.dp,
                        color = MaterialTheme.colors.surface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when (UserSession.getCurrentUser()?.role) {
                                        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                        UserRole.MANAGER -> Icons.Default.ManageAccounts
                                        UserRole.CASHIER -> Icons.Default.PointOfSale
                                        UserRole.STOCK -> Icons.Default.Inventory
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        UserSession.getCurrentUser()?.fullName ?: "Usuário",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        UserSession.getCurrentUser()?.role?.displayName ?: "",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "PDV Desktop v1.0",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Spacer(Modifier.width(16.dp))

                                // Botão de toggle de tema
                                IconButton(
                                    onClick = { ThemeManager.toggleTheme() }
                                ) {
                                    Icon(
                                        if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = if (isDarkTheme) "Tema Claro" else "Tema Escuro",
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Conteúdo das telas
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colors.background
                        ) {
                            when (currentScreen) {
                                Screen.VENDAS -> {
                                    if (UserSession.hasPermission(Permission.VIEW_SALES)) {
                                        SalesScreen(snackbarHostState)
                                    } else {
                                        PermissionDeniedScreen("Vendas")
                                    }
                                }
                                Screen.CAIXA -> {
                                    if (UserSession.hasPermission(Permission.MAKE_SALES)) {
                                        CashRegisterScreen(snackbarHostState)
                                    } else {
                                        PermissionDeniedScreen("Caixa")
                                    }
                                }
                                Screen.PRODUTOS -> {
                                    if (UserSession.hasPermission(Permission.VIEW_PRODUCTS)) {
                                        ProductsScreen(snackbarHostState)
                                    } else {
                                        PermissionDeniedScreen("Produtos")
                                    }
                                }
                                Screen.RELATORIOS -> {
                                    if (UserSession.hasPermission(Permission.VIEW_REPORTS)) {
                                        ReportsScreen()
                                    } else {
                                        PermissionDeniedScreen("Relatórios")
                                    }
                                }
                                Screen.USUARIOS -> {
                                    if (UserSession.hasPermission(Permission.MANAGE_USERS)) {
                                        UsersScreen(snackbarHostState)
                                    } else {
                                        PermissionDeniedScreen("Usuários")
                                    }
                                }
                                Screen.CONFIGURACOES -> ConfigScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedScreen(screenName: String) {
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
                "Você não tem permissão para acessar $screenName",
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Cargo atual: ${UserSession.getCurrentUser()?.role?.displayName}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ConfigScreen() {
    val currentUser = UserSession.getCurrentUser()
    val isDarkTheme = ThemeManager.isDarkTheme

    val outerScroll = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxContentWidth = if (this.maxWidth < 1000.dp) this.maxWidth - 32.dp else 980.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(outerScroll)
                    .padding(24.dp)
                    .widthIn(max = maxContentWidth)
            ) {
                Text(
                    "Configurações",
                    style = MaterialTheme.typography.h4
                )

                Spacer(Modifier.height(24.dp))

                // Aparência e Tema
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp,
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colors.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Aparência",
                                style = MaterialTheme.typography.h6
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Tema Escuro",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (isDarkTheme) "Ativado - Interface escura para reduzir cansaço visual"
                                    else "Desativado - Interface clara e tradicional",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { ThemeManager.toggleTheme() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF90CAF9),
                                    checkedTrackColor = Color(0xFF42A5F5),
                                    uncheckedThumbColor = Color(0xFF1976D2),
                                    uncheckedTrackColor = Color(0xFFBBDEFB)
                                )
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Preview do tema
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f).height(60.dp),
                                elevation = 2.dp,
                                backgroundColor = if (!isDarkTheme) MaterialTheme.colors.primary else Color(0xFFE3F2FD)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.LightMode,
                                            contentDescription = null,
                                            tint = if (!isDarkTheme) Color.White else Color(0xFF1976D2)
                                        )
                                        Text(
                                            "Claro",
                                            fontSize = 10.sp,
                                            color = if (!isDarkTheme) Color.White else Color(0xFF1976D2)
                                        )
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f).height(60.dp),
                                elevation = 2.dp,
                                backgroundColor = if (isDarkTheme) Color(0xFF90CAF9) else Color(0xFF424242)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = if (isDarkTheme) Color.Black else Color.White
                                        )
                                        Text(
                                            "Escuro",
                                            fontSize = 10.sp,
                                            color = if (isDarkTheme) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Informações do Usuário
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when (currentUser?.role) {
                                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                    UserRole.MANAGER -> Icons.Default.ManageAccounts
                                    UserRole.CASHIER -> Icons.Default.PointOfSale
                                    UserRole.STOCK -> Icons.Default.Inventory
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colors.primary
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Usuário Logado",
                                    style = MaterialTheme.typography.h6
                                )
                                Text(
                                    currentUser?.fullName ?: "Desconhecido",
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "@${currentUser?.username} - ${currentUser?.role?.displayName}",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Permissões Ativas",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        Column {
                            currentUser?.role?.permissions?.forEach { permission ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        permission.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Impressora",
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            label = { Text("Nome da Impressora") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = UserSession.isAdmin()
                        )
                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Sistema de Backup",
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Checkbox(
                                checked = false,
                                onCheckedChange = {},
                                enabled = UserSession.isAdmin()
                            )
                            Text("Ativar backup automático")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Server & Remote settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Servidor e Sincronização", style = MaterialTheme.typography.h6)
                        Spacer(Modifier.height(8.dp))
                        val serverEnabledState = remember { mutableStateOf(Config.serverEnabled) }
                        val serverBindState = remember { mutableStateOf(Config.serverBind) }
                        val serverPortState = remember { mutableStateOf(Config.serverPort.toString()) }
                        val serverTokenState = remember { mutableStateOf(Config.serverToken) }
                        val remoteUrlState = remember { mutableStateOf(Config.remoteUrl) }
                        val remoteTokenState = remember { mutableStateOf(Config.remoteToken) }
                        val serverRunning = remember { mutableStateOf(false) }
                        val outboxDao = remember { OutboxDao() }
                        var outboxCount by remember { mutableStateOf(outboxDao.countPending()) }
                        var healthStatus by remember { mutableStateOf("--") }

                        // Small status row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Status do Servidor", fontWeight = FontWeight.Bold)
                                Text("Saúde: $healthStatus", color = Color.Gray, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Fila outbox", fontWeight = FontWeight.Bold)
                                Text("$outboxCount pendente(s)", color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo Servidor", fontWeight = FontWeight.Bold)
                                Text(if (serverEnabledState.value) "Ativado" else "Desativado", fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(checked = serverEnabledState.value, onCheckedChange = { checked ->
                                serverEnabledState.value = checked
                                Config.setBoolean("server.enabled", checked)
                                if (!checked) {
                                    // stop server
                                    EmbeddedServer.stop()
                                }
                            })
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = serverBindState.value, onValueChange = { serverBindState.value = it }, label = { Text("Bind (host)") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = serverPortState.value, onValueChange = { serverPortState.value = it.filter { ch -> ch.isDigit() } }, label = { Text("Porta") }, modifier = Modifier.width(120.dp))
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(value = serverTokenState.value, onValueChange = { serverTokenState.value = it }, label = { Text("Token (opcional)") }, modifier = Modifier.fillMaxWidth())

                        Spacer(Modifier.height(12.dp))

                        Text("Servidor remoto (replicar vendas)", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = remoteUrlState.value, onValueChange = { remoteUrlState.value = it }, label = { Text("Remote URL") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = remoteTokenState.value, onValueChange = { remoteTokenState.value = it }, label = { Text("Remote Token (opcional)") }, modifier = Modifier.fillMaxWidth())

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = {
                                // salvar
                                Config.setBoolean("server.enabled", serverEnabledState.value)
                                Config.set("server.bind", serverBindState.value)
                                Config.set("server.port", serverPortState.value)
                                Config.set("server.token", serverTokenState.value)
                                Config.set("remote.url", remoteUrlState.value)
                                Config.set("remote.token", remoteTokenState.value)
                                // aplicar mudanças
                                if (serverEnabledState.value) EmbeddedServer.start(serverPortState.value.toIntOrNull() ?: 8080, serverBindState.value) else EmbeddedServer.stop()
                                if (!remoteUrlState.value.isBlank()) SyncService.start() else SyncService.stop()

                                // atualizar health/outbox
                                outboxCount = outboxDao.countPending()
                                try {
                                    val bind = serverBindState.value
                                    val port = serverPortState.value.toIntOrNull() ?: 8080
                                    val url = URL("http://$bind:$port/health")
                                    val conn = url.openConnection() as HttpURLConnection
                                    conn.requestMethod = "GET"
                                    conn.connectTimeout = 2000
                                    conn.readTimeout = 2000
                                    val code = conn.responseCode
                                    healthStatus = if (code in 200..299) "OK" else "HTTP $code"
                                } catch (e: Exception) {
                                    healthStatus = "Offline"
                                }

                            }) {
                                Text("Salvar e Aplicar")
                            }

                            OutlinedButton(onClick = {
                                // resetar para valores atuais
                                serverEnabledState.value = Config.serverEnabled
                                serverBindState.value = Config.serverBind
                                serverPortState.value = Config.serverPort.toString()
                                serverTokenState.value = Config.serverToken
                                remoteUrlState.value = Config.remoteUrl
                                remoteTokenState.value = Config.remoteToken
                                outboxCount = outboxDao.countPending()
                            }) {
                                Text("Recarregar")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Informações do Sistema",
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Versão: 1.0.0", style = MaterialTheme.typography.body2)
                        Text("Banco de dados: SQLite", style = MaterialTheme.typography.body2)
                        Text("Framework: Compose Desktop", style = MaterialTheme.typography.body2)
                        Text("Kotlin: 1.9.22", style = MaterialTheme.typography.body2)
                    }
                }
            }
        }

        // Nota: barra de rolagem visual removida para compatibilidade com a versão do Compose usada.
        // A rolagem continuará funcionando com mouse/trackpad via `verticalScroll(outerScroll)`.
    }
}

fun main() {
    // Inicializar banco e outras coisas
    try {
        Database.initialize()
    } catch (e: Exception) {
        println("✗ Falha ao inicializar: ${e.message}")
        e.printStackTrace()
        // Não podemos continuar sem DB
        return
    }

    // Checar se devemos iniciar modo servidor via env/prop CLI
    val serverEnabled = System.getProperty("pdv.server.enabled") == "true" || System.getenv("PDV_SERVER") == "1"
    if (serverEnabled) {
        val portProp = System.getProperty("pdv.server.port") ?: System.getenv("PDV_SERVER_PORT") ?: "8080"
        val port = try { portProp.toInt() } catch (e: Exception) { 8080 }
        val bind = System.getProperty("pdv.server.bind") ?: System.getenv("PDV_SERVER_BIND") ?: "127.0.0.1"
        EmbeddedServer.start(port, bind)
    }

    // Start server from persisted config if enabled
    EmbeddedServer.startFromConfig()

    // Start background sync if remote is configured
    if (!Config.remoteUrl.isBlank()) {
        SyncService.start()
        println("✓ SyncService iniciado (enviando para: ${Config.remoteUrl})")
    }

    application {
        Window(onCloseRequest = {
            EmbeddedServer.stop()
            SyncService.stop()
            Database.close()
            exitProcess(0)
        }, title = "PDV Desktop") {
            App()
        }
    }
}
