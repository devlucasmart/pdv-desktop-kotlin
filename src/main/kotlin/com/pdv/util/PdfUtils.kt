package com.pdv.util

import com.pdv.data.Sale
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.PDPageContentStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.print.DocFlavor
import javax.print.SimpleDoc
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.Doc
import javax.print.PrintServiceLookup

object PdfUtils {
    // Generate a receipt PDF in a thermal/receipt layout and return the file path
    fun generateReceiptPdf(sale: Sale, outFile: File): File {
        // Estimate lines to compute a reasonable page height (thermal receipts are usually long and narrow)
        val charPerLine = 36 // approx for courier at chosen font size and page width

        fun wrap(text: String, max: Int): List<String> {
            val parts = mutableListOf<String>()
            var remaining = text.trim()
            while (remaining.isNotEmpty()) {
                if (remaining.length <= max) {
                    parts.add(remaining)
                    break
                }
                // try to break at last space before max
                val idx = remaining.substring(0, max).lastIndexOf(' ')
                if (idx <= 0) {
                    parts.add(remaining.substring(0, max))
                    remaining = remaining.substring(max)
                } else {
                    parts.add(remaining.substring(0, idx))
                    remaining = remaining.substring(idx).trimStart()
                }
            }
            return parts
        }

        // count lines: header (fixed) + for each item wrap lines + footer
        val headerLines = 6
        var itemLines = 0
        sale.items.forEach { item ->
            val name = item.product.name ?: ""
            val wrapped = wrap(name, charPerLine - 10) // reserve chars for qty/price columns
            itemLines += wrapped.size
        }
        val footerLines = 6
        val totalLines = headerLines + itemLines + footerLines

        val lineHeight = 12f
        val marginTop = 20f
        val marginLeft = 8f

        val pageWidth = 226f // ~80mm in points (72 dpi)
        val pageHeight = marginTop + totalLines * lineHeight + 40f

        PDDocument().use { doc ->
            val page = PDPage(PDRectangle(pageWidth, pageHeight))
            doc.addPage(page)

            PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.setFont(PDType1Font.COURIER_BOLD, 12f)
                cs.newLineAtOffset(marginLeft, pageHeight - marginTop)

                // Header - use StoreConfig for configurable store info
                val storeName = StoreConfig.getName()
                val storeAddress = StoreConfig.getAddress()
                val storeDocument = StoreConfig.getDocument()

                cs.showText(storeName)
                cs.newLineAtOffset(0f, -14f)
                cs.setFont(PDType1Font.COURIER, 9f)
                wrap(storeAddress, charPerLine).forEach { line ->
                    cs.showText(line)
                    cs.newLineAtOffset(0f, -11f)
                }
                cs.showText("CNPJ: $storeDocument")
                cs.newLineAtOffset(0f, -11f)

                cs.newLineAtOffset(0f, -4f)
                cs.setFont(PDType1Font.COURIER_BOLD, 10f)
                cs.showText("CUPOM FISCAL")
                cs.newLineAtOffset(0f, -14f)

                // Items header
                cs.setFont(PDType1Font.COURIER_BOLD, 9f)
                // Columns: ITEM/DESC    QTD   VALOR
                val colDescWidth = 18 // chars approx
                cs.showText(String.format(Locale.getDefault(), "%-${colDescWidth}s %6s %8s", "ITEM/DESC", "QTD", "VAL"))
                cs.newLineAtOffset(0f, -12f)
                cs.setFont(PDType1Font.COURIER, 9f)

                // Items
                sale.items.forEachIndexed { index, item ->
                    val name = item.product.name ?: ""
                    val qty = item.quantity
                    val price = item.unitPrice
                    val wrapped = wrap(name, charPerLine - 12)
                    wrapped.forEachIndexed { wi, line ->
                        if (wi == 0) {
                            // first line: show columns
                            val descField = if (line.length > colDescWidth) line.substring(0, colDescWidth) else line
                            val qtyStr = String.format(Locale("pt", "BR"), "%.2f", qty)
                            val priceStr = String.format(Locale("pt", "BR"), "%.2f", price)
                            // align description left, qty center-ish, price right
                            val lineText = String.format(Locale.getDefault(), "%-${colDescWidth}s %6s %8s", descField, qtyStr, priceStr)
                            cs.showText(lineText)
                        } else {
                            // continuation line for description
                            cs.showText(line)
                        }
                        cs.newLineAtOffset(0f, -11f)
                    }
                }

                cs.newLineAtOffset(0f, -6f)
                cs.setFont(PDType1Font.COURIER_BOLD, 10f)
                cs.showText("Subtotal: ${String.format(Locale("pt", "BR"), "R$ %.2f", sale.subtotal)}")
                cs.newLineAtOffset(0f, -12f)
                cs.showText("Desconto: ${String.format(Locale("pt", "BR"), "R$ %.2f", sale.totalDiscount)}")
                cs.newLineAtOffset(0f, -12f)
                cs.showText("TOTAL: ${String.format(Locale("pt", "BR"), "R$ %.2f", sale.total)}")
                cs.newLineAtOffset(0f, -14f)

                cs.setFont(PDType1Font.COURIER, 9f)
                cs.showText("Forma de Pagamento: ${sale.paymentMethod ?: "---"}")
                cs.newLineAtOffset(0f, -12f)
                // Se houver partes salvas no banco, mostre detalhamento
                try {
                    if (sale.id > 0) {
                        val partDao = com.pdv.data.PaymentPartDao()
                        val parts = partDao.findBySaleId(sale.id)
                        if (parts.isNotEmpty()) {
                            cs.setFont(PDType1Font.COURIER_BOLD, 9f)
                            cs.showText("Detalhamento de pagamento:")
                            cs.newLineAtOffset(0f, -12f)
                            cs.setFont(PDType1Font.COURIER, 9f)
                            parts.forEach { pp ->
                                val line = String.format(Locale("pt", "BR"), "%s: R$ %.2f", pp.method, pp.amount)
                                cs.showText(line)
                                cs.newLineAtOffset(0f, -11f)
                            }
                            cs.newLineAtOffset(0f, -4f)
                        }
                    }
                } catch (e: Exception) {
                    // ignore - printing shouldn't fail for missing parts
                }
                cs.showText("Operador: ${sale.operatorName ?: ""}")
                cs.newLineAtOffset(0f, -12f)

                // Footer notes - use sale.dateTime
                val dateStr = try {
                    LocalDateTime.parse(sale.dateTime).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                } catch (e: Exception) {
                    sale.dateTime
                }
                cs.showText("Operacao finalizada em: $dateStr")
                cs.newLineAtOffset(0f, -12f)
                cs.showText("Obrigado pela preferencia!")

                cs.endText()
            }

            outFile.parentFile?.mkdirs()
            doc.save(outFile)
        }
        return outFile
    }

    fun printPdf(file: File): Boolean {
        try {
            val fis = FileInputStream(file)
            val flavor = DocFlavor.INPUT_STREAM.AUTOSENSE
            val doc: Doc = SimpleDoc(fis, flavor, null)
            val ps = HashPrintRequestAttributeSet()
            val printServices = PrintServiceLookup.lookupPrintServices(flavor, ps)
            val service = if (printServices.isNotEmpty()) printServices[0] else PrintServiceLookup.lookupDefaultPrintService()
            if (service == null) { fis.close(); return false }
            val printJob = service.createPrintJob()
            printJob.print(doc, ps)
            fis.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
