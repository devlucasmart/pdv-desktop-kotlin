package com.pdv.data

import java.sql.SQLException

class ClientDebtDao {
    private fun getConn() = Database.getConnection()

    fun createDebt(clientId: Long, saleId: Long?, amountDue: Double, description: String? = null): Long {
        val conn = getConn() ?: return 0L
        return try {
            val stmt = conn.prepareStatement("INSERT INTO client_debt (client_id, sale_id, amount_due, amount_paid, description, status) VALUES (?, ?, ?, ?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS)
            stmt.setLong(1, clientId)
            if (saleId == null) stmt.setNull(2, java.sql.Types.INTEGER) else stmt.setLong(2, saleId)
            stmt.setDouble(3, amountDue)
            stmt.setDouble(4, 0.0)
            if (description == null) stmt.setNull(5, java.sql.Types.VARCHAR) else stmt.setString(5, description)
            stmt.setString(6, "OPEN")
            stmt.executeUpdate()
            val rs = stmt.generatedKeys
            val id = if (rs.next()) rs.getLong(1) else 0L
            id
        } catch (e: SQLException) {
            println("✗ Erro SQL ao criar dívida: ${e.message}")
            0L
        }
    }

    fun findOpenByClient(clientId: Long): List<Map<String, Any?>> {
        val conn = getConn() ?: return emptyList()
        return try {
            val stmt = conn.prepareStatement("SELECT id, sale_id, amount_due, amount_paid, description, status, created_at, paid_at FROM client_debt WHERE client_id = ? ORDER BY created_at DESC")
            stmt.setLong(1, clientId)
            val rs = stmt.executeQuery()
            val list = mutableListOf<Map<String, Any?>>()
            while (rs.next()) {
                val saleLong: Long? = rs.getLong("sale_id").takeIf { !rs.wasNull() }
                list.add(mapOf(
                    "id" to rs.getLong("id"),
                    "sale_id" to saleLong,
                    "amount_due" to rs.getDouble("amount_due"),
                    "amount_paid" to rs.getDouble("amount_paid"),
                    "description" to (rs.getString("description") ?: ""),
                    "status" to rs.getString("status"),
                    "created_at" to rs.getString("created_at"),
                    "paid_at" to (rs.getString("paid_at") ?: "")
                ))
            }
            list
        } catch (e: SQLException) {
            println("✗ Erro SQL ao buscar dívidas: ${e.message}")
            emptyList()
        }
    }

    fun recordPayment(debtId: Long, amount: Double): Boolean {
        val conn = getConn() ?: return false
        return try {
            // obter dívida atual
            val rs = conn.prepareStatement("SELECT amount_due, amount_paid FROM client_debt WHERE id = ?").apply { setLong(1, debtId) }.executeQuery()
            if (!rs.next()) return false
            val due = rs.getDouble("amount_due")
            val paid = rs.getDouble("amount_paid")
            val newPaid = (paid + amount).coerceAtMost(due)
            val status = if (newPaid >= due) "CLOSED" else "OPEN"
            val stmt = conn.prepareStatement("UPDATE client_debt SET amount_paid = ?, status = ?, paid_at = CASE WHEN ? THEN datetime('now') ELSE paid_at END WHERE id = ?")
            stmt.setDouble(1, newPaid)
            stmt.setString(2, status)
            // set boolean param to indicate closure
            val willClose = newPaid >= due
            stmt.setBoolean(3, willClose)
            stmt.setLong(4, debtId)
            val updated = stmt.executeUpdate() > 0
            updated
        } catch (e: SQLException) {
            println("✗ Erro SQL ao registrar pagamento da dívida: ${e.message}")
            false
        }
    }
}
