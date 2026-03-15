package com.pdv.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Product(
    val id: Long = 0,
    val sku: String,
    val name: String,
    val price: Double,
    val stockQuantity: Double = 0.0,
    val unit: String = "un",
    val category: String? = null,
    val active: Boolean = true
) {
    fun isLowStock(threshold: Double = 10.0): Boolean = stockQuantity < threshold

    fun formattedPrice(): String = "R$ %.2f".format(price)
}

data class SaleItem(
    val product: Product,
    var quantity: Double = 1.0,
    val discount: Double = 0.0
) {
    val unitPrice: Double
        get() = product.price

    val totalWithoutDiscount: Double
        get() = product.price * quantity

    val total: Double
        get() = totalWithoutDiscount - discount

    fun incrementQuantity() {
        quantity += 1.0
    }

    fun decrementQuantity() {
        if (quantity > 0.0) quantity -= 1.0
    }
}

data class Client(
    val id: Long = 0,
    val name: String,
    val document: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    // default discount em percentual (ex.: 10.0 = 10%)
    val defaultDiscountPercent: Double = 0.0,
    val active: Boolean = true,
    val createdAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
)

data class Sale(
    val id: Long = 0,
    val dateTime: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val items: List<SaleItem> = emptyList(),
    val discount: Double = 0.0,
    val paymentMethod: String? = null,
    val status: String = "COMPLETED",
    val operatorName: String? = null,
    // paymentParts: list of pairs (methodName, amount) - not persisted directly by model, used by DAO
    val paymentParts: List<Pair<String, Double>> = emptyList(),
    private val _total: Double = 0.0,
    private val _subtotal: Double = 0.0,
    // Referência opcional a cliente e desconto aplicado (valor absoluto)
    val clientId: Long? = null,
    val clientDiscount: Double = 0.0,
    // se true, criar dívida no nome do cliente mesmo que haja pagamento parcial/total
    val chargeToAccount: Boolean = false
) {
    val total: Double
        get() = if (_total > 0.0) _total else items.sumOf { it.total } - discount - clientDiscount

    val subtotal: Double
        get() = if (_subtotal > 0.0) _subtotal else items.sumOf { it.totalWithoutDiscount }

    // Compatibilidade: alias para APIs existentes
    val totalDiscount: Double
        get() = discount + clientDiscount

    // Expose raw backing values for serialization if needed
    fun rawTotal(): Double = _total
    fun rawSubtotal(): Double = _subtotal
}
