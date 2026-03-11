package com.pdv.util

import com.pdv.data.Config
import java.awt.print.PrinterJob
import javax.print.PrintService
import javax.print.PrintServiceLookup

object PrinterService {

    /**
     * Lista todas as impressoras disponíveis no sistema
     */
    fun listPrinters(): List<String> {
        val services = PrintServiceLookup.lookupPrintServices(null, null)
        return services.map { it.name }
    }

    /**
     * Obtém a impressora padrão do sistema
     */
    fun getDefaultPrinter(): String? {
        return PrintServiceLookup.lookupDefaultPrintService()?.name
    }

    /**
     * Obtém a impressora configurada ou a padrão
     */
    fun getConfiguredPrinter(): String {
        val configured = Config.printerName
        return if (configured.isNotBlank()) {
            configured
        } else {
            getDefaultPrinter() ?: ""
        }
    }

    /**
     * Encontra o serviço de impressão pelo nome
     */
    fun findPrintService(printerName: String): PrintService? {
        val services = PrintServiceLookup.lookupPrintServices(null, null)
        return services.find { it.name.equals(printerName, ignoreCase = true) }
    }

    /**
     * Verifica se a impressora configurada está disponível
     */
    fun isPrinterAvailable(): Boolean {
        val printerName = Config.printerName
        if (printerName.isBlank()) return false
        return findPrintService(printerName) != null
    }

    /**
     * Imprime texto simples (recibo)
     */
    fun printText(text: String, printerName: String? = null): PrintResult {
        val targetPrinter = printerName ?: Config.printerName

        if (targetPrinter.isBlank()) {
            return PrintResult(false, "Nenhuma impressora configurada")
        }

        val printService = findPrintService(targetPrinter)
            ?: return PrintResult(false, "Impressora não encontrada: $targetPrinter")

        return try {
            val job = PrinterJob.getPrinterJob()
            job.printService = printService

            job.setPrintable { graphics, pageFormat, pageIndex ->
                if (pageIndex > 0) {
                    java.awt.print.Printable.NO_SUCH_PAGE
                } else {
                    val g2d = graphics as java.awt.Graphics2D
                    g2d.translate(pageFormat.imageableX.toInt(), pageFormat.imageableY.toInt())

                    val font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 10)
                    g2d.font = font

                    val lines = text.split("\n")
                    var y = 15
                    for (line in lines) {
                        g2d.drawString(line, 0, y)
                        y += 12
                    }

                    java.awt.print.Printable.PAGE_EXISTS
                }
            }

            job.print()
            PrintResult(true, "Impresso com sucesso")
        } catch (e: Exception) {
            PrintResult(false, "Erro ao imprimir: ${e.message}")
        }
    }

    /**
     * Testa a impressora com uma página de teste
     */
    fun testPrinter(printerName: String? = null): PrintResult {
        val testText = buildString {
            appendLine("=".repeat(40))
            appendLine("        TESTE DE IMPRESSORA")
            appendLine("=".repeat(40))
            appendLine()
            appendLine("PDV Desktop - Sistema de Vendas")
            appendLine()
            appendLine("Se você está vendo esta mensagem,")
            appendLine("a impressora está funcionando")
            appendLine("corretamente!")
            appendLine()
            appendLine("Data/Hora: ${java.time.LocalDateTime.now()}")
            appendLine()
            appendLine("=".repeat(40))
        }

        return printText(testText, printerName)
    }
}

data class PrintResult(
    val success: Boolean,
    val message: String
)

