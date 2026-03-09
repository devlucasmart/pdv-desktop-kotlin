package com.pdv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdv.data.*
import com.pdv.reports.PdfReportGenerator
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ReportsScreen() {
    val saleDao = remember { SaleDao() }
    val productDao = remember { ProductDao() }
    val pdfGenerator = remember { PdfReportGenerator() }
    val scope = rememberCoroutineScope()

    // Estados de dados
    var totalSales by remember { mutableStateOf(0.0) }
    var salesCount by remember { mutableStateOf(0) }
    var salesToday by remember { mutableStateOf(0.0) }
    var salesCountToday by remember { mutableStateOf(0) }
    var averageTicket by remember { mutableStateOf(0.0) }
    var totalProducts by remember { mutableStateOf(0) }
    var lowStockCount by remember { mutableStateOf(0) }
    var salesList by remember { mutableStateOf<List<Sale>>(emptyList()) }
    var lowStockProducts by remember { mutableStateOf<List<Product>>(emptyList()) }

    // Estados de UI
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Estados de filtro de período
    var selectedPeriod by remember { mutableStateOf("today") }
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var periodTotal by remember { mutableStateOf(0.0) }
    var periodCount by remember { mutableStateOf(0) }

    // Carregar dados baseado no período selecionado
    LaunchedEffect(refreshTrigger, selectedPeriod, startDate, endDate) {
        println("📊 Carregando dados dos relatórios (período: $selectedPeriod)...")

        // Dados gerais (sempre)
        totalSales = saleDao.getTotalSales()
        salesCount = saleDao.getSalesCount()
        salesToday = saleDao.getSalesToday()
        salesCountToday = saleDao.getSalesCountToday()
        averageTicket = saleDao.getAverageTicket()

        val products = productDao.findAll()
        totalProducts = products.size
        lowStockProducts = productDao.getLowStockProducts(10)
        lowStockCount = lowStockProducts.size

        // Dados do período selecionado
        when (selectedPeriod) {
            "today" -> {
                salesList = saleDao.findToday()
                periodTotal = salesToday
                periodCount = salesCountToday
            }
            "week" -> {
                val weekStart = LocalDate.now().minusDays(7).toString()
                val weekEnd = LocalDate.now().toString()
                salesList = saleDao.findByPeriod(weekStart, weekEnd)
                periodTotal = saleDao.getTotalByPeriod(weekStart, weekEnd)
                periodCount = saleDao.getCountByPeriod(weekStart, weekEnd)
            }
            "month" -> {
                val monthStart = LocalDate.now().withDayOfMonth(1).toString()
                val monthEnd = LocalDate.now().toString()
                salesList = saleDao.findByPeriod(monthStart, monthEnd)
                periodTotal = saleDao.getTotalByPeriod(monthStart, monthEnd)
                periodCount = saleDao.getCountByPeriod(monthStart, monthEnd)
            }
            "custom" -> {
                salesList = saleDao.findByPeriod(startDate, endDate)
                periodTotal = saleDao.getTotalByPeriod(startDate, endDate)
                periodCount = saleDao.getCountByPeriod(startDate, endDate)
            }
            "all" -> {
                salesList = saleDao.findAll()
                periodTotal = totalSales
                periodCount = salesCount
            }
        }

        println("📊 Dados carregados: $periodCount vendas, R$ $periodTotal no período")
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Painel principal (70%)
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "📊 Relatórios e Estatísticas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Última atualização: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, "Atualizar", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Atualizar")
                    }

                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, "Exportar PDF", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Exportar PDF", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Filtro de período
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                backgroundColor = Color(0xFFF5F5F5)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📅 Filtrar por Período", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeriodChip("Hoje", "today", selectedPeriod) { selectedPeriod = it }
                        PeriodChip("Últimos 7 dias", "week", selectedPeriod) { selectedPeriod = it }
                        PeriodChip("Este mês", "month", selectedPeriod) { selectedPeriod = it }
                        PeriodChip("Todos", "all", selectedPeriod) { selectedPeriod = it }
                        PeriodChip("Personalizado", "custom", selectedPeriod) { selectedPeriod = it }
                    }

                    // Campos de data personalizada
                    if (selectedPeriod == "custom") {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Data Inicial") },
                                placeholder = { Text("AAAA-MM-DD") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp)) }
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = { endDate = it },
                                label = { Text("Data Final") },
                                placeholder = { Text("AAAA-MM-DD") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp)) }
                            )
                            Button(
                                onClick = { refreshTrigger++ },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text("Filtrar")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Cards de resumo do período
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    title = "Faturamento do Período",
                    value = "R$ %.2f".format(periodTotal),
                    subtitle = "$periodCount venda(s)",
                    icon = Icons.Default.AttachMoney,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Ticket Médio",
                    value = "R$ %.2f".format(if (periodCount > 0) periodTotal / periodCount else 0.0),
                    subtitle = "No período selecionado",
                    icon = Icons.Default.Receipt,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Faturamento Total",
                    value = "R$ %.2f".format(totalSales),
                    subtitle = "$salesCount venda(s) total",
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Lista de vendas do período
            Text(
                "📋 Vendas do Período (${salesList.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (salesList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            Text("Nenhuma venda no período selecionado", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1976D2))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text("ID", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(60.dp))
                                Text("Data/Hora", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(120.dp))
                                Text("Operador", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                                Text("Pagamento", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(100.dp))
                                Text("Total", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(100.dp), textAlign = TextAlign.End)
                            }
                        }
                        items(salesList) { sale ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${sale.id}", modifier = Modifier.width(60.dp), color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    try {
                                        LocalDateTime.parse(sale.dateTime).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                                    } catch (e: Exception) { sale.dateTime.take(16) },
                                    modifier = Modifier.width(120.dp),
                                    fontSize = 13.sp
                                )
                                Text(sale.operatorName ?: "-", modifier = Modifier.weight(1f), fontSize = 13.sp)
                                Text(
                                    when (sale.paymentMethod) {
                                        "DINHEIRO" -> "💵 Dinheiro"
                                        "PIX" -> "📱 PIX"
                                        "CARTAO_CREDITO" -> "💳 Crédito"
                                        "CARTAO_DEBITO" -> "💳 Débito"
                                        else -> sale.paymentMethod ?: "-"
                                    },
                                    modifier = Modifier.width(100.dp),
                                    fontSize = 12.sp
                                )
                                Text(
                                    "R$ %.2f".format(sale.total),
                                    modifier = Modifier.width(100.dp),
                                    textAlign = TextAlign.End,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50),
                                    fontSize = 14.sp
                                )
                            }
                            Divider(color = Color(0xFFEEEEEE))
                        }
                        // Rodapé com total
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE3F2FD))
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL DO PERÍODO", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "R$ %.2f".format(salesList.sumOf { it.total }),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Painel lateral (30%)
        Card(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(top = 24.dp, end = 24.dp, bottom = 24.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Resumo geral
                Text("📈 Resumo Geral", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                QuickStatRow("Vendas Hoje", "$salesCountToday")
                QuickStatRow("Faturamento Hoje", "R$ %.2f".format(salesToday))
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                QuickStatRow("Total de Vendas", "$salesCount")
                QuickStatRow("Faturamento Total", "R$ %.2f".format(totalSales))
                QuickStatRow("Ticket Médio", "R$ %.2f".format(averageTicket))
                QuickStatRow("Produtos Ativos", "$totalProducts")

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                // Produtos com estoque baixo
                Text("⚠️ Estoque Baixo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))

                if (lowStockProducts.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFE8F5E9),
                        elevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(8.dp))
                            Text("Estoque OK!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(lowStockProducts.take(5)) { product ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                backgroundColor = if (product.stockQuantity <= 5) Color(0xFFFFEBEE) else Color(0xFFFFF3E0),
                                elevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
                                        Text("SKU: ${product.sku}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Text(
                                        "${product.stockQuantity} un",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (product.stockQuantity <= 5) Color(0xFFD32F2F) else Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }
                    if (lowStockProducts.size > 5) {
                        Text(
                            "+${lowStockProducts.size - 5} produtos",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Diálogo de exportação
    if (showExportDialog) {
        ExportPdfDialog(
            onDismiss = { showExportDialog = false },
            startDate = startDate,
            endDate = endDate,
            onStartDateChange = { startDate = it },
            onEndDateChange = { endDate = it },
            onExport = { reportType, exportStart, exportEnd ->
                isExporting = true
                exportMessage = ""

                scope.launch {
                    try {
                        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                        val fileName = "relatorio_${reportType}_$timestamp.pdf"
                        val outputPath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName

                        val success = when (reportType) {
                            "caixa" -> pdfGenerator.generateCashierReport(outputPath)
                            "categoria" -> pdfGenerator.generateCategoryReport(outputPath)
                            "vendas" -> pdfGenerator.generateDetailedSalesReport(outputPath)
                            "estoque" -> pdfGenerator.generateStockReport(outputPath)
                            "completo" -> pdfGenerator.generateCompleteReport(outputPath)
                            else -> false
                        }

                        isExporting = false

                        if (success) {
                            exportMessage = "✓ PDF gerado com sucesso!\n\nLocal: $outputPath"
                            try {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().open(File(outputPath))
                                }
                            } catch (e: Exception) {
                                println("Não foi possível abrir o PDF automaticamente")
                            }
                        } else {
                            exportMessage = "✗ Erro ao gerar PDF. Tente novamente."
                        }
                    } catch (e: Exception) {
                        isExporting = false
                        exportMessage = "✗ Erro: ${e.message}"
                    }
                }
            },
            isExporting = isExporting,
            exportMessage = exportMessage
        )
    }
}

@Composable
private fun PeriodChip(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == value
    Surface(
        modifier = Modifier.clickable { onSelect(value) },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colors.primary else Color.White,
        elevation = if (isSelected) 4.dp else 1.dp
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun QuickStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExportPdfDialog(
    onDismiss: () -> Unit,
    startDate: String,
    endDate: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onExport: (String, String, String) -> Unit,
    isExporting: Boolean,
    exportMessage: String
) {
    var selectedReport by remember { mutableStateOf("completo") }
    var exportStartDate by remember { mutableStateOf(startDate) }
    var exportEndDate by remember { mutableStateOf(endDate) }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFD32F2F))
                Spacer(Modifier.width(8.dp))
                Text("Exportar Relatório PDF", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.width(450.dp)) {
                if (exportMessage.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (exportMessage.startsWith("✓")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Text(exportMessage, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text("Tipo de Relatório:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                listOf(
                    "completo" to "📊 Relatório Completo",
                    "vendas" to "💰 Relatório de Vendas",
                    "caixa" to "🏧 Relatório de Caixa",
                    "estoque" to "📦 Relatório de Estoque",
                    "categoria" to "📁 Vendas por Categoria"
                ).forEach { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReport == key,
                            onClick = { selectedReport = key }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(label, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Período do Relatório:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = exportStartDate,
                        onValueChange = { exportStartDate = it },
                        label = { Text("Data Inicial") },
                        placeholder = { Text("AAAA-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = exportEndDate,
                        onValueChange = { exportEndDate = it },
                        label = { Text("Data Final") },
                        placeholder = { Text("AAAA-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedReport, exportStartDate, exportEndDate) },
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Gerando...", color = Color.White)
                } else {
                    Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gerar PDF", color = Color.White)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isExporting) {
                Text("Fechar")
            }
        }
    )
}
