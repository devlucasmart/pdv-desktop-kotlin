package com.pdv.reports

import com.pdv.data.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.PDFont
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PdfReportGenerator {

    companion object {
        private const val MARGIN = 50f
        private const val FONT_SIZE_TITLE = 18f
        private const val FONT_SIZE_SUBTITLE = 14f
        private const val FONT_SIZE_NORMAL = 11f
        private const val FONT_SIZE_SMALL = 9f
        private const val LINE_HEIGHT = 15f
    }

    private val productDao = ProductDao()
    private val saleDao = SaleDao()

    // Gerar relatório completo do caixa
    fun generateCashierReport(outputPath: String): Boolean {
        return try {
            val document = PDDocument()
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            val contentStream = PDPageContentStream(document, page)
            var yPosition = page.mediaBox.height - MARGIN

            // Cabeçalho
            yPosition = drawHeader(contentStream, yPosition, "RELATÓRIO DE CAIXA")
            yPosition -= LINE_HEIGHT * 2

            // Informações gerais
            val sales = saleDao.findAll()
            val totalSales = sales.size
            val totalRevenue = sales.sumOf { it.total }
            val todaySales = saleDao.findToday()
            val todayRevenue = todaySales.sumOf { it.total }
            val averageTicket = if (totalSales > 0) totalRevenue / totalSales else 0.0
            yPosition -= LINE_HEIGHT * 2

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("Resumo Geral")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 2

            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_NORMAL)

            val summaryData = listOf(
                "Total de Vendas:" to totalSales.toString(),
                "Faturamento Total:" to "R$ %.2f".format(totalRevenue),
                "Vendas Hoje:" to todaySales.size.toString(),
                "Faturamento Hoje:" to "R$ %.2f".format(todayRevenue),
                "Ticket Médio:" to "R$ %.2f".format(averageTicket)
            )

            summaryData.forEach { (label, value) ->
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText("$label  $value")
                contentStream.endText()
                yPosition -= LINE_HEIGHT
            }

            yPosition -= LINE_HEIGHT * 2

            // Vendas por forma de pagamento
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("Vendas por Forma de Pagamento")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 2

            val salesByPayment = sales.groupBy { it.paymentMethod }
            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_NORMAL)

            salesByPayment.forEach { (method, salesList) ->
                val count = salesList.size
                val total = salesList.sumOf { it.total }
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText("$method: $count vendas - R$ %.2f".format(total))
                contentStream.endText()
                yPosition -= LINE_HEIGHT
            }

            // Rodapé
            drawFooter(contentStream, page)

            contentStream.close()
            document.save(outputPath)
            document.close()

            println("✓ Relatório de Caixa gerado: $outputPath")
            true
        } catch (e: Exception) {
            println("✗ Erro ao gerar relatório: ${e.message}")
            false
        }
    }

    // Gerar relatório por categoria
    fun generateCategoryReport(outputPath: String): Boolean {
        return try {
            val document = PDDocument()
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            val contentStream = PDPageContentStream(document, page)
            var yPosition = page.mediaBox.height - MARGIN

            // Cabeçalho
            yPosition = drawHeader(contentStream, yPosition, "RELATÓRIO POR CATEGORIA")
            yPosition -= LINE_HEIGHT * 2

            // Produtos por categoria
            val products = productDao.findAll()
            val productsByCategory = products.groupBy { it.category ?: "Sem Categoria" }

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("Produtos por Categoria")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 2

            productsByCategory.entries.forEach { (category, productList) ->
                val totalValue = productList.sumOf { it.price * it.stockQuantity }
                val totalItems = productList.sumOf { it.stockQuantity }

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_NORMAL)
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText("$category (${productList.size} produtos)")
                contentStream.endText()
                yPosition -= LINE_HEIGHT

                contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_SMALL)
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN + 20, yPosition)
                contentStream.showText("Qtd. Total: $totalItems un. | Valor em Estoque: R$ %.2f".format(totalValue))
                contentStream.endText()
                yPosition -= LINE_HEIGHT

                // Listar produtos da categoria
                productList.take(5).forEach { product ->
                    contentStream.beginText()
                    contentStream.newLineAtOffset(MARGIN + 20, yPosition)
                    contentStream.showText("- ${product.name} | R$ %.2f | Estoque: ${product.stockQuantity}".format(product.price))
                    contentStream.endText()
                    yPosition -= LINE_HEIGHT

                    if (yPosition < MARGIN + 50) {
                        contentStream.close()
                        val newPage = PDPage(PDRectangle.A4)
                        document.addPage(newPage)
                        val newStream = PDPageContentStream(document, newPage)
                        yPosition = page.mediaBox.height - MARGIN
                        return@forEach
                    }
                }

                yPosition -= LINE_HEIGHT
            }

            // Rodapé
            drawFooter(contentStream, page)

            contentStream.close()
            document.save(outputPath)
            document.close()

            println("✓ Relatório por Categoria gerado: $outputPath")
            true
        } catch (e: Exception) {
            println("✗ Erro ao gerar relatório: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Gerar relatório detalhado de vendas
    fun generateDetailedSalesReport(outputPath: String, startDate: String? = null, endDate: String? = null): Boolean {
        return try {
            val document = PDDocument()
            var page = PDPage(PDRectangle.A4)
            document.addPage(page)

            var contentStream = PDPageContentStream(document, page)
            var yPosition = page.mediaBox.height - MARGIN

            // Cabeçalho
            yPosition = drawHeader(contentStream, yPosition, "RELATÓRIO DETALHADO DE VENDAS")
            yPosition -= LINE_HEIGHT

            // Período
            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_SMALL)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            val period = if (startDate != null && endDate != null) {
                "Período: $startDate a $endDate"
            } else {
                "Período: Todas as vendas"
            }
            contentStream.showText(period)
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 2

            // Lista de vendas
            val sales = saleDao.findAll()

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("Vendas Realizadas (${sales.size})")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 2

            sales.forEach { sale ->
                // Verificar se precisa de nova página
                if (yPosition < MARGIN + 100) {
                    contentStream.close()
                    page = PDPage(PDRectangle.A4)
                    document.addPage(page)
                    contentStream = PDPageContentStream(document, page)
                    yPosition = page.mediaBox.height - MARGIN
                }

                // Informações da venda
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_NORMAL)
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText("Venda #${sale.id} - ${sale.dateTime}")
                contentStream.endText()
                yPosition -= LINE_HEIGHT

                contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_SMALL)
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN + 20, yPosition)
                contentStream.showText("Total: R$ %.2f | Pagamento: ${sale.paymentMethod}".format(sale.total))
                contentStream.endText()
                yPosition -= LINE_HEIGHT * 1.5f
            }

            // Rodapé
            drawFooter(contentStream, page)

            contentStream.close()
            document.save(outputPath)
            document.close()

            println("✓ Relatório Detalhado de Vendas gerado: $outputPath")
            true
        } catch (e: Exception) {
            println("✗ Erro ao gerar relatório: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Gerar relatório de estoque
    fun generateStockReport(outputPath: String): Boolean {
        return try {
            val document = PDDocument()
            var page = PDPage(PDRectangle.A4)
            document.addPage(page)

            var contentStream = PDPageContentStream(document, page)
            var yPosition = page.mediaBox.height - MARGIN

            // Cabeçalho
            yPosition = drawHeader(contentStream, yPosition, "RELATÓRIO DE ESTOQUE")
            yPosition -= LINE_HEIGHT * 2

            // Estatísticas gerais
            val products = productDao.findAll()
            val totalProducts = products.size
            val totalStockValue = products.sumOf { it.price * it.stockQuantity }
            val lowStockProducts = products.filter { it.isLowStock() }

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("Resumo do Estoque")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 2

            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_NORMAL)
            val stockSummary = listOf(
                "Total de Produtos:" to totalProducts.toString(),
                "Valor Total em Estoque:" to "R$ %.2f".format(totalStockValue),
                "Produtos com Estoque Baixo:" to lowStockProducts.size.toString()
            )

            stockSummary.forEach { (label, value) ->
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText("$label  $value")
                contentStream.endText()
                yPosition -= LINE_HEIGHT
            }

            yPosition -= LINE_HEIGHT * 2

            // Lista de produtos
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("Lista de Produtos")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 2

            // Cabeçalho da tabela
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SMALL)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("SKU")
            contentStream.newLineAtOffset(60f, 0f)
            contentStream.showText("Nome")
            contentStream.newLineAtOffset(180f, 0f)
            contentStream.showText("Preco")
            contentStream.newLineAtOffset(80f, 0f)
            contentStream.showText("Estoque")
            contentStream.newLineAtOffset(80f, 0f)
            contentStream.showText("Total")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 1.5f

            // Lista de produtos
            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_SMALL)
            products.forEach { product ->
                if (yPosition < MARGIN + 50) {
                    contentStream.close()
                    page = PDPage(PDRectangle.A4)
                    document.addPage(page)
                    contentStream = PDPageContentStream(document, page)
                    yPosition = page.mediaBox.height - MARGIN
                }

                val totalValue = product.price * product.stockQuantity
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText(product.sku)
                contentStream.newLineAtOffset(60f, 0f)
                val name = if (product.name.length > 25) product.name.substring(0, 25) + "..." else product.name
                contentStream.showText(name)
                contentStream.newLineAtOffset(180f, 0f)
                contentStream.showText("R$ %.2f".format(product.price))
                contentStream.newLineAtOffset(80f, 0f)
                contentStream.showText("${product.stockQuantity} un")
                contentStream.newLineAtOffset(80f, 0f)
                contentStream.showText("R$ %.2f".format(totalValue))
                contentStream.endText()
                yPosition -= LINE_HEIGHT
            }

            // Rodapé
            drawFooter(contentStream, page)

            contentStream.close()
            document.save(outputPath)
            document.close()

            println("✓ Relatório de Estoque gerado: $outputPath")
            true
        } catch (e: Exception) {
            println("✗ Erro ao gerar relatório: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Gerar relatório completo (todos os dados)
    fun generateCompleteReport(outputPath: String): Boolean {
        return try {
            val document = PDDocument()
            var page = PDPage(PDRectangle.A4)
            document.addPage(page)

            var contentStream = PDPageContentStream(document, page)
            var yPosition = page.mediaBox.height - MARGIN

            // Cabeçalho
            yPosition = drawHeader(contentStream, yPosition, "RELATÓRIO COMPLETO DO SISTEMA")
            yPosition -= LINE_HEIGHT * 2

            // Seção 1: Resumo de Vendas
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("1. VENDAS E FATURAMENTO")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 1.5f

            val sales = saleDao.findAll()
            val totalRevenue = sales.sumOf { it.total }
            val todaySales = saleDao.findToday()
            val todayRevenue = todaySales.sumOf { it.total }

            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_NORMAL)
            val salesData = listOf(
                "Total de Vendas: ${sales.size}",
                "Faturamento Total: R$ %.2f".format(totalRevenue),
                "Vendas Hoje: ${todaySales.size}",
                "Faturamento Hoje: R$ %.2f".format(todayRevenue),
                "Ticket Medio: R$ %.2f".format(if (sales.isNotEmpty()) totalRevenue / sales.size else 0.0)
            )

            salesData.forEach { line ->
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText(line)
                contentStream.endText()
                yPosition -= LINE_HEIGHT
            }

            yPosition -= LINE_HEIGHT * 2

            // Seção 2: Vendas por Pagamento
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("2. VENDAS POR FORMA DE PAGAMENTO")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 1.5f

            val salesByPayment = sales.groupBy { it.paymentMethod }
            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_NORMAL)

            salesByPayment.entries.forEach { (method, salesList) ->
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText("$method: ${salesList.size} vendas - R$ %.2f".format(salesList.sumOf { it.total }))
                contentStream.endText()
                yPosition -= LINE_HEIGHT
            }

            yPosition -= LINE_HEIGHT * 2

            // Seção 3: Produtos por Categoria
            if (yPosition < MARGIN + 200) {
                contentStream.close()
                page = PDPage(PDRectangle.A4)
                document.addPage(page)
                contentStream = PDPageContentStream(document, page)
                yPosition = page.mediaBox.height - MARGIN
            }

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("3. PRODUTOS POR CATEGORIA")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 1.5f

            val products = productDao.findAll()
            val productsByCategory = products.groupBy { it.category ?: "Sem Categoria" }

            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_NORMAL)

            productsByCategory.entries.forEach { (category, productList) ->
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText("$category: ${productList.size} produtos")
                contentStream.endText()
                yPosition -= LINE_HEIGHT
            }

            yPosition -= LINE_HEIGHT * 2

            // Seção 4: Estoque
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_SUBTITLE)
            contentStream.beginText()
            contentStream.newLineAtOffset(MARGIN, yPosition)
            contentStream.showText("4. SITUACAO DO ESTOQUE")
            contentStream.endText()
            yPosition -= LINE_HEIGHT * 1.5f

            val lowStockProducts = products.filter { it.isLowStock() }
            val totalStockValue = products.sumOf { it.price * it.stockQuantity }

            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE_NORMAL)
            val stockData = listOf(
                "Total de Produtos: ${products.size}",
                "Valor Total em Estoque: R$ %.2f".format(totalStockValue),
                "Produtos com Estoque Baixo: ${lowStockProducts.size}"
            )

            stockData.forEach { line ->
                contentStream.beginText()
                contentStream.newLineAtOffset(MARGIN, yPosition)
                contentStream.showText(line)
                contentStream.endText()
                yPosition -= LINE_HEIGHT
            }

            // Rodapé
            drawFooter(contentStream, page)

            contentStream.close()
            document.save(outputPath)
            document.close()

            println("✓ Relatório Completo gerado: $outputPath")
            true
        } catch (e: Exception) {
            println("✗ Erro ao gerar relatório: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Funções auxiliares para cabeçalho e rodapé
    private fun drawHeader(stream: PDPageContentStream, yPosition: Float, title: String): Float {
        var y = yPosition

        // Título
        stream.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_TITLE)
        stream.beginText()
        stream.newLineAtOffset(MARGIN, y)
        stream.showText(title)
        stream.endText()
        y -= LINE_HEIGHT

        // Linha separadora
        stream.setLineWidth(1f)
        stream.moveTo(MARGIN, y)
        stream.lineTo(PDRectangle.A4.width - MARGIN, y)
        stream.stroke()
        y -= LINE_HEIGHT

        // Data e hora
        stream.setFont(PDType1Font.HELVETICA, FONT_SIZE_SMALL)
        stream.beginText()
        stream.newLineAtOffset(MARGIN, y)
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        stream.showText("Gerado em: $now")
        stream.endText()
        y -= LINE_HEIGHT

        stream.beginText()
        stream.newLineAtOffset(MARGIN, y)
        stream.showText("Sistema: PDV Desktop v1.0.0")
        stream.endText()
        y -= LINE_HEIGHT

        return y
    }

    private fun drawFooter(stream: PDPageContentStream, page: PDPage) {
        val y = MARGIN - 20

        stream.setFont(PDType1Font.HELVETICA, FONT_SIZE_SMALL)
        stream.beginText()
        stream.newLineAtOffset(MARGIN, y)
        stream.showText("PDV Desktop - Sistema de Ponto de Venda")
        stream.endText()

        stream.beginText()
        stream.newLineAtOffset(page.mediaBox.width - MARGIN - 100, y)
        stream.showText("www.pdvsystems.com")
        stream.endText()
    }
}

