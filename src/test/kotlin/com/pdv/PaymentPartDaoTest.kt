package com.pdv

import com.pdv.data.Database
import com.pdv.data.PaymentPartDao
import com.pdv.data.SaleDao
import com.pdv.data.Sale
import com.pdv.data.SaleItem
import com.pdv.data.Product
import kotlin.test.*

class PaymentPartDaoTest {
    @BeforeTest
    fun setup() {
        Database.initialize()
    }

    @AfterTest
    fun teardown() {
        Database.close()
    }

    @Test
    fun testSaveAndFindParts() {
        val product = Product(id = 1, sku = "TEST001", name = "Produto Teste", price = 1.0)
        val item = SaleItem(product = product, quantity = 1.0)
        val sale = Sale(items = listOf(item), discount = 0.0, paymentMethod = "DINHEIRO", operatorName = "Test", paymentParts = listOf("DINHEIRO" to 1.0))
        val saleDao = SaleDao()
        val saleId = saleDao.save(sale)
        assertTrue(saleId > 0, "saleId should be > 0")

        val partDao = PaymentPartDao()
        val parts = partDao.findBySaleId(saleId)
        assertTrue(parts.isNotEmpty(), "parts should not be empty for saved sale")
        assertEquals(1, parts.size)
        assertEquals("DINHEIRO", parts[0].method)
        assertEquals(1.0, parts[0].amount)
    }
}

