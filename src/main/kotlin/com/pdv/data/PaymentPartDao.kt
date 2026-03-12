package com.pdv.data

import java.sql.SQLException
import java.util.UUID

data class PaymentPart(
    val id: Long = 0,
    val saleId: Long,
    val method: String,
    val amount: Double,
    val authCode: String? = null
)

class PaymentPartDao {
    private fun getConn() = Database.getConnection()

    fun saveParts(saleId: Long, parts: List<Pair<String, Double>>): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement("INSERT INTO payment_part (sale_id, method, amount, auth_code, created_at) VALUES (?, ?, ?, ?, datetime('now'))")
            conn.autoCommit = false
            parts.forEach { p ->
                stmt.setLong(1, saleId)
                stmt.setString(2, p.first)
                stmt.setDouble(3, p.second)
                val code = UUID.randomUUID().toString().replace("-", "").take(16)
                stmt.setString(4, code)
                stmt.addBatch()
            }
            stmt.executeBatch()
            conn.commit()
            true
        } catch (e: SQLException) {
            try { getConn()?.rollback() } catch (_: Exception) {}
            println("✗ Erro ao salvar partes de pagamento: ${e.message}")
            false
        } finally {
            try { getConn()?.autoCommit = true } catch (_: Exception) {}
        }
    }

    fun findBySaleId(saleId: Long): List<PaymentPart> {
        val conn = getConn() ?: return emptyList()
        return try {
            val parts = mutableListOf<PaymentPart>()
            val stmt = conn.prepareStatement("SELECT id, sale_id, method, amount, auth_code FROM payment_part WHERE sale_id = ? ORDER BY id")
            stmt.setLong(1, saleId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                parts.add(PaymentPart(rs.getLong("id"), rs.getLong("sale_id"), rs.getString("method"), rs.getDouble("amount"), rs.getString("auth_code")))
            }
            parts
        } catch (e: SQLException) {
            println("✗ Erro ao buscar payment parts: ${e.message}")
            emptyList()
        }
    }
}
