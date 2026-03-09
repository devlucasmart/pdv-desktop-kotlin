package com.pdv.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Product(
    val id: Long = 0,
    val sku: String,
    val name: String,
    val price: Double,
    val stockQuantity: Int = 0,
    val category: String? = null,
    val active: Boolean = true
) {
    fun isLowStock(threshold: Int = 10): Boolean = stockQuantity < threshold

    fun formattedPrice(): String = "R$ %.2f".format(price)
}

data class SaleItem(
    val product: Product,
    var quantity: Int = 1,
    val discount: Double = 0.0
) {
    val unitPrice: Double
        get() = product.price

    val totalWithoutDiscount: Double
        get() = product.price * quantity

    val total: Double
        get() = totalWithoutDiscount - discount

    fun incrementQuantity() {
        quantity++
    }

    fun decrementQuantity() {
        if (quantity > 1) quantity--
    }
}

data class Sale(
    val id: Long = 0,
    val dateTime: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val items: List<SaleItem>,
    val discount: Double = 0.0,
    val paymentMethod: String? = null,
    val status: String = "COMPLETED",
    val operatorName: String? = null,
    // Campos para valores carregados do banco (quando não temos itens)
    val _total: Double? = null,
    val _subtotal: Double? = null
) {
    val subtotal: Double
        get() = _subtotal ?: items.sumOf { it.totalWithoutDiscount }

    val totalDiscount: Double
        get() = items.sumOf { it.discount } + discount

    val total: Double
        get() = _total ?: (subtotal - totalDiscount)

    fun getFormattedDateTime(): String {
        return try {
            val dt = LocalDateTime.parse(dateTime)
            dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        } catch (e: Exception) {
            dateTime
        }
    }

    fun formattedTotal(): String = "R$ %.2f".format(total)
}

enum class PaymentMethod(val displayName: String) {
    DINHEIRO("Dinheiro"),
    CARTAO_DEBITO("Cartão de Débito"),
    CARTAO_CREDITO("Cartão de Crédito"),
    PIX("PIX"),
    OUTROS("Outros")
}

