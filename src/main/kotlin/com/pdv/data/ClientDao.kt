package com.pdv.data

import java.sql.SQLException

class ClientDao {
    private fun getConn() = Database.getConnection()

    fun save(client: Client): Long {
        val conn = getConn() ?: return 0L
        return try {
            val stmt = conn.prepareStatement(
                "INSERT INTO client (name, document, phone, email, address, default_discount_percent, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.setString(1, client.name.trim())
            if (client.document == null) stmt.setNull(2, java.sql.Types.VARCHAR) else stmt.setString(2, client.document)
            if (client.phone == null) stmt.setNull(3, java.sql.Types.VARCHAR) else stmt.setString(3, client.phone)
            if (client.email == null) stmt.setNull(4, java.sql.Types.VARCHAR) else stmt.setString(4, client.email)
            if (client.address == null) stmt.setNull(5, java.sql.Types.VARCHAR) else stmt.setString(5, client.address)
            stmt.setDouble(6, client.defaultDiscountPercent)
            stmt.setInt(7, if (client.active) 1 else 0)

            stmt.executeUpdate()
            val rs = stmt.generatedKeys
            val id = if (rs.next()) rs.getLong(1) else 0L
            println("✓ Cliente criado: ${client.name} (ID: $id)")
            id
        } catch (e: SQLException) {
            println("✗ Erro SQL ao salvar cliente: ${e.message}")
            0L
        }
    }

    fun update(client: Client): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                "UPDATE client SET name = ?, document = ?, phone = ?, email = ?, address = ?, default_discount_percent = ?, active = ? WHERE id = ?"
            )
            stmt.setString(1, client.name.trim())
            if (client.document == null) stmt.setNull(2, java.sql.Types.VARCHAR) else stmt.setString(2, client.document)
            if (client.phone == null) stmt.setNull(3, java.sql.Types.VARCHAR) else stmt.setString(3, client.phone)
            if (client.email == null) stmt.setNull(4, java.sql.Types.VARCHAR) else stmt.setString(4, client.email)
            if (client.address == null) stmt.setNull(5, java.sql.Types.VARCHAR) else stmt.setString(5, client.address)
            stmt.setDouble(6, client.defaultDiscountPercent)
            stmt.setInt(7, if (client.active) 1 else 0)
            stmt.setLong(8, client.id)

            val updated = stmt.executeUpdate() > 0
            if (updated) println("✓ Cliente atualizado: ${client.name} (ID: ${client.id})")
            updated
        } catch (e: SQLException) {
            println("✗ Erro SQL ao atualizar cliente: ${e.message}")
            false
        }
    }

    fun delete(id: Long): Boolean {
        // Soft-delete
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement("UPDATE client SET active = 0 WHERE id = ?")
            stmt.setLong(1, id)
            val updated = stmt.executeUpdate() > 0
            if (updated) println("✓ Cliente desativado: ID $id")
            updated
        } catch (e: SQLException) {
            println("✗ Erro SQL ao deletar cliente: ${e.message}")
            false
        }
    }

    fun findById(id: Long): Client? {
        val conn = getConn() ?: return null
        return try {
            val stmt = conn.prepareStatement("SELECT id, name, document, phone, email, address, default_discount_percent, active, created_at FROM client WHERE id = ?")
            stmt.setLong(1, id)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                Client(
                    id = rs.getLong("id"),
                    name = rs.getString("name"),
                    document = rs.getString("document"),
                    phone = rs.getString("phone"),
                    email = rs.getString("email"),
                    address = rs.getString("address"),
                    defaultDiscountPercent = rs.getDouble("default_discount_percent"),
                    active = rs.getInt("active") == 1,
                    createdAt = rs.getString("created_at")
                )
            } else null
        } catch (e: SQLException) {
            println("✗ Erro SQL ao buscar cliente: ${e.message}")
            null
        }
    }

    fun findAll(activeOnly: Boolean = true): List<Client> {
        val conn = getConn() ?: return emptyList()
        return try {
            val clients = mutableListOf<Client>()
            val sql = if (activeOnly) "SELECT id, name, document, phone, email, address, default_discount_percent, active, created_at FROM client WHERE active = 1 ORDER BY name" else "SELECT id, name, document, phone, email, address, default_discount_percent, active, created_at FROM client ORDER BY name"
            val rs = conn.createStatement().executeQuery(sql)
            while (rs.next()) {
                clients.add(
                    Client(
                        id = rs.getLong("id"),
                        name = rs.getString("name"),
                        document = rs.getString("document"),
                        phone = rs.getString("phone"),
                        email = rs.getString("email"),
                        address = rs.getString("address"),
                        defaultDiscountPercent = rs.getDouble("default_discount_percent"),
                        active = rs.getInt("active") == 1,
                        createdAt = rs.getString("created_at")
                    )
                )
            }
            clients
        } catch (e: SQLException) {
            println("✗ Erro SQL ao listar clientes: ${e.message}")
            emptyList()
        }
    }

    fun search(term: String): List<Client> {
        val conn = getConn() ?: return emptyList()
        return try {
            val stmt = conn.prepareStatement("SELECT id, name, document, phone, email, address, default_discount_percent, active, created_at FROM client WHERE active = 1 AND (name LIKE ? OR document LIKE ?) ORDER BY name")
            val q = "%${term.trim()}%"
            stmt.setString(1, q)
            stmt.setString(2, q)
            val rs = stmt.executeQuery()
            val list = mutableListOf<Client>()
            while (rs.next()) {
                list.add(
                    Client(
                        id = rs.getLong("id"),
                        name = rs.getString("name"),
                        document = rs.getString("document"),
                        phone = rs.getString("phone"),
                        email = rs.getString("email"),
                        address = rs.getString("address"),
                        defaultDiscountPercent = rs.getDouble("default_discount_percent"),
                        active = rs.getInt("active") == 1,
                        createdAt = rs.getString("created_at")
                    )
                )
            }
            list
        } catch (e: SQLException) {
            println("✗ Erro SQL ao buscar clientes: ${e.message}")
            emptyList()
        }
    }
}

