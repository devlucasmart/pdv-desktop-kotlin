package com.pdv.data

import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class CashSession(
    val id: Long = 0,
    val openedBy: String?,
    val openedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val initialAmount: Double = 0.0,
    val closedAt: String? = null,
    val closingAmount: Double? = null,
    val status: String = "OPEN"
)

class CashRegisterDao {
    private fun getConn() = Database.getConnection()

    fun openSession(openedBy: String?, initialAmount: Double): Long {
        val conn = getConn() ?: return 0L
        return try {
            val stmt = conn.prepareStatement(
                """
                INSERT INTO cash_register (opened_by, opened_at, initial_amount, status)
                VALUES (?, ?, ?, 'OPEN')
                """,
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.setString(1, openedBy)
            stmt.setString(2, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            stmt.setDouble(3, initialAmount)
            stmt.executeUpdate()
            // Tentar obter generatedKeys
            val rs = stmt.generatedKeys
            var id = 0L
            if (rs != null && rs.next()) {
                id = rs.getLong(1)
            }
            // Fallback para SQLite: last_insert_rowid() se generatedKeys não retornou
            if (id == 0L) {
                try {
                    val rs2 = conn.createStatement().executeQuery("SELECT last_insert_rowid()")
                    if (rs2 != null && rs2.next()) {
                        id = rs2.getLong(1)
                    }
                } catch (e: Exception) {
                    // ignorar, manter id = 0
                }
            }
            id
        } catch (e: SQLException) {
            println("✗ Erro ao abrir sessão do caixa: ${e.message}")
            0L
        }
    }

    fun closeSession(sessionId: Long, closingAmount: Double): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                """
                UPDATE cash_register SET closed_at = ?, closing_amount = ?, status = 'CLOSED' WHERE id = ?
                """
            )
            stmt.setString(1, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            stmt.setDouble(2, closingAmount)
            stmt.setLong(3, sessionId)
            stmt.executeUpdate()
            true
        } catch (e: SQLException) {
            println("✗ Erro ao fechar sessão do caixa: ${e.message}")
            false
        }
    }

    fun getCurrentOpenSession(): CashSession? {
        val conn = getConn() ?: return null
        return try {
            val rs = conn.createStatement().executeQuery(
                "SELECT * FROM cash_register WHERE status = 'OPEN' ORDER BY opened_at DESC LIMIT 1"
            )
            if (rs.next()) {
                CashSession(
                    id = rs.getLong("id"),
                    openedBy = rs.getString("opened_by"),
                    openedAt = rs.getString("opened_at"),
                    initialAmount = rs.getDouble("initial_amount"),
                    closedAt = rs.getString("closed_at"),
                    closingAmount = rs.getDouble("closing_amount"),
                    status = rs.getString("status")
                )
            } else null
        } catch (e: SQLException) {
            println("✗ Erro ao obter sessão do caixa: ${e.message}")
            null
        }
    }

    fun recordMovement(sessionId: Long, type: String, amount: Double, description: String? = null): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                """
                INSERT INTO cash_movement (session_id, type, amount, description) VALUES (?, ?, ?, ?)
                """
            )
            stmt.setLong(1, sessionId)
            stmt.setString(2, type)
            stmt.setDouble(3, amount)
            stmt.setString(4, description)
            stmt.executeUpdate()
            true
        } catch (e: SQLException) {
            println("✗ Erro ao registrar movimento de caixa: ${e.message}")
            false
        }
    }

    fun getMovementsForSession(sessionId: Long): List<Map<String, Any>> {
        val conn = getConn() ?: return emptyList()
        return try {
            val movements = mutableListOf<Map<String, Any>>()
            val stmt = conn.prepareStatement("SELECT * FROM cash_movement WHERE session_id = ? ORDER BY created_at ASC")
            stmt.setLong(1, sessionId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                movements.add(
                    mapOf(
                        "id" to rs.getLong("id"),
                        "type" to rs.getString("type"),
                        "amount" to rs.getDouble("amount"),
                        "description" to rs.getString("description"),
                        "created_at" to rs.getString("created_at"),
                        "session_id" to rs.getLong("session_id")
                    )
                )
            }
            movements
        } catch (e: SQLException) {
            println("✗ Erro ao buscar movimentos: ${e.message}")
            emptyList()
        }
    }

    // Retorna lista de movimentos no período (inclusive)
    fun findByPeriod(startDate: String, endDate: String): List<Map<String, Any>> {
        val conn = getConn() ?: return emptyList()
        return try {
            val movements = mutableListOf<Map<String, Any>>()
            val stmt = conn.prepareStatement(
                "SELECT * FROM cash_movement WHERE date(created_at) >= date(?) AND date(created_at) <= date(?) ORDER BY created_at ASC"
            )
            stmt.setString(1, startDate)
            stmt.setString(2, endDate)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                movements.add(
                    mapOf(
                        "id" to rs.getLong("id"),
                        "session_id" to rs.getLong("session_id"),
                        "type" to rs.getString("type"),
                        "amount" to rs.getDouble("amount"),
                        "description" to rs.getString("description"),
                        "created_at" to rs.getString("created_at")
                    )
                )
            }
            movements
        } catch (e: SQLException) {
            println("✗ Erro ao buscar movimentos por período: ${e.message}")
            emptyList()
        }
    }

    // Insere movimento e retorna id gerado
    fun registerMovement(sessionId: Long, type: String, amount: Double, description: String? = null, operator: String? = null): Long {
        val conn = getConn() ?: return 0L
        return try {
            val stmt = conn.prepareStatement(
                "INSERT INTO cash_movement (session_id, type, amount, description) VALUES (?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.setLong(1, sessionId)
            stmt.setString(2, type)
            stmt.setDouble(3, amount)
            stmt.setString(4, description)
            stmt.executeUpdate()
            val rs = stmt.generatedKeys
            if (rs.next()) rs.getLong(1) else 0L
        } catch (e: SQLException) {
            println("✗ Erro ao registrar movimento de caixa: ${e.message}")
            0L
        }
    }
}
