package com.pdv.data

import java.sql.SQLException

class OutboxDao {
    private fun getConn() = Database.getConnection()

    fun enqueue(clientUuid: String, payload: String): Long {
        val conn = getConn() ?: return 0L
        return try {
            val stmt = conn.prepareStatement(
                "INSERT INTO outbox_sale (client_uuid, payload, status, attempts, created_at) VALUES (?, ?, 'PENDING', 0, datetime('now'))",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.setString(1, clientUuid)
            stmt.setString(2, payload)
            stmt.executeUpdate()
            val rs = stmt.generatedKeys
            if (rs.next()) rs.getLong(1) else 0L
        } catch (e: SQLException) {
            println("✗ Erro ao enfileirar outbox: ${e.message}")
            0L
        }
    }

    fun listPending(limit: Int = 100): List<Map<String, Any>> {
        val conn = getConn() ?: return emptyList()
        return try {
            val items = mutableListOf<Map<String, Any>>()
            val stmt = conn.prepareStatement("SELECT id, client_uuid, payload, attempts, last_error, created_at FROM outbox_sale WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT ?")
            stmt.setInt(1, limit)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                items.add(
                    mapOf(
                        "id" to rs.getLong("id"),
                        "client_uuid" to rs.getString("client_uuid"),
                        "payload" to rs.getString("payload"),
                        "attempts" to rs.getInt("attempts"),
                        "last_error" to rs.getString("last_error"),
                        "created_at" to rs.getString("created_at")
                    )
                )
            }
            items
        } catch (e: SQLException) {
            println("✗ Erro ao listar outbox: ${e.message}")
            emptyList()
        }
    }

    fun incrementAttempt(id: Long, error: String?): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement("UPDATE outbox_sale SET attempts = attempts + 1, last_attempt_at = datetime('now'), last_error = ? WHERE id = ?")
            stmt.setString(1, error)
            stmt.setLong(2, id)
            stmt.executeUpdate()
            true
        } catch (e: SQLException) {
            println("✗ Erro ao incrementar attempt no outbox: ${e.message}")
            false
        }
    }

    fun markSynced(id: Long): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement("UPDATE outbox_sale SET status = 'SYNCHED' WHERE id = ?")
            stmt.setLong(1, id)
            stmt.executeUpdate()
            true
        } catch (e: SQLException) {
            println("✗ Erro ao marcar outbox como synched: ${e.message}")
            false
        }
    }

    fun markFailed(id: Long, error: String?): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement("UPDATE outbox_sale SET status = 'FAILED', last_error = ?, last_attempt_at = datetime('now') WHERE id = ?")
            stmt.setString(1, error)
            stmt.setLong(2, id)
            stmt.executeUpdate()
            true
        } catch (e: SQLException) {
            println("✗ Erro ao marcar outbox como failed: ${e.message}")
            false
        }
    }

    fun countPending(): Int {
        val conn = getConn() ?: return 0
        return try {
            val rs = conn.createStatement().executeQuery("SELECT COUNT(*) as cnt FROM outbox_sale WHERE status = 'PENDING'")
            if (rs.next()) rs.getInt("cnt") else 0
        } catch (e: SQLException) {
            println("✗ Erro ao contar outbox pendente: ${e.message}")
            0
        }
    }
}
