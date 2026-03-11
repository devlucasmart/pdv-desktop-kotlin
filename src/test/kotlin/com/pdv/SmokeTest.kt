package com.pdv

import com.pdv.data.*
import kotlin.test.*

class SmokeTest {
    @Test
    fun testSaveSale() {
        Database.initialize()

        val productDao = ProductDao()
        val products = productDao.findAll()
        assertTrue(products.isNotEmpty(), "Deve haver produtos de exemplo")

        val prod = products.first()
        val qty: Double = 1.0
        val item = SaleItem(product = prod, quantity = qty)
        val sale = Sale(items = listOf(item), discount = 0.0, paymentMethod = "DINHEIRO", operatorName = "Test")

        val saleDao = SaleDao()
        val saleId = saleDao.save(sale)

        println("SmokeTest: saleId = $saleId")
        assertTrue(saleId > 0, "Venda deve ser salva com ID > 0")

        Database.close()
    }
}
