package com.pdv.data

import java.sql.ResultSet
import java.sql.SQLException

class UserDao {
    private fun getConn() = Database.getConnection()

    fun authenticate(username: String, password: String): User? {
        val conn = getConn() ?: return null
        return try {
            val stmt = conn.prepareStatement(
                """
                SELECT id, username, password, full_name, role, active, created_at
                FROM user 
                WHERE username = ? AND password = ? AND active = 1
                """
            )
            stmt.setString(1, username.trim())
            stmt.setString(2, password)

            val rs = stmt.executeQuery()

            if (rs.next()) {
                rs.toUser()
            } else {
                null
            }
        } catch (e: SQLException) {
            println("✗ Erro ao autenticar usuário: ${e.message}")
            null
        }
    }

    fun findByUsername(username: String): User? {
        val conn = getConn() ?: return null
        return try {
            val stmt = conn.prepareStatement(
                """
                SELECT id, username, password, full_name, role, active, created_at
                FROM user 
                WHERE username = ?
                """
            )
            stmt.setString(1, username.trim())
            val rs = stmt.executeQuery()

            if (rs.next()) rs.toUser() else null
        } catch (e: SQLException) {
            println("✗ Erro ao buscar usuário: ${e.message}")
            null
        }
    }

    fun findAll(): List<User> {
        val conn = getConn() ?: return emptyList()
        return try {
            val users = mutableListOf<User>()
            val rs = conn.createStatement().executeQuery(
                """
                SELECT id, username, password, full_name, role, active, created_at
                FROM user 
                WHERE active = 1 
                ORDER BY full_name
                """
            )

            while (rs.next()) {
                users.add(rs.toUser())
            }

            users
        } catch (e: SQLException) {
            println("✗ Erro ao buscar usuários: ${e.message}")
            emptyList()
        }
    }

    fun save(user: User): Long {
        val conn = getConn() ?: return 0L
        return try {
            val stmt = conn.prepareStatement(
                """
                INSERT INTO user (username, password, full_name, role) 
                VALUES (?, ?, ?, ?)
                """,
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.setString(1, user.username.trim())
            stmt.setString(2, user.password)
            stmt.setString(3, user.fullName.trim())
            stmt.setString(4, user.role.name)
            stmt.executeUpdate()

            val rs = stmt.generatedKeys
            val id = if (rs.next()) rs.getLong(1) else 0L

            println("✓ Usuário criado: ${user.username} (ID: $id)")
            id
        } catch (e: SQLException) {
            println("✗ Erro ao salvar usuário: ${e.message}")
            0L
        }
    }

    fun update(user: User): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                """
                UPDATE user 
                SET full_name = ?, role = ?, password = ?
                WHERE username = ?
                """
            )
            stmt.setString(1, user.fullName.trim())
            stmt.setString(2, user.role.name)
            stmt.setString(3, user.password)
            stmt.setString(4, user.username.trim())

            val updated = stmt.executeUpdate() > 0

            if (updated) {
                println("✓ Usuário atualizado: ${user.username}")
            }

            updated
        } catch (e: SQLException) {
            println("✗ Erro ao atualizar usuário: ${e.message}")
            false
        }
    }

    fun changePassword(username: String, newPassword: String): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                "UPDATE user SET password = ? WHERE username = ?"
            )
            stmt.setString(1, newPassword)
            stmt.setString(2, username.trim())

            val updated = stmt.executeUpdate() > 0

            if (updated) {
                println("✓ Senha alterada para usuário: $username")
            }

            updated
        } catch (e: SQLException) {
            println("✗ Erro ao alterar senha: ${e.message}")
            false
        }
    }

    fun delete(username: String): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                "UPDATE user SET active = 0 WHERE username = ?"
            )
            stmt.setString(1, username.trim())

            val deleted = stmt.executeUpdate() > 0

            if (deleted) {
                println("✓ Usuário desativado: $username")
            }

            deleted
        } catch (e: SQLException) {
            println("✗ Erro ao deletar usuário: ${e.message}")
            false
        }
    }

    private fun ResultSet.toUser() = User(
        id = getLong("id"),
        username = getString("username"),
        password = getString("password"),
        fullName = getString("full_name"),
        role = UserRole.valueOf(getString("role")),
        active = getInt("active") == 1
    )
}
