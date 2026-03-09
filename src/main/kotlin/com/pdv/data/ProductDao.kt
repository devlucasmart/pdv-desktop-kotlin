package com.pdv.data

import java.sql.ResultSet
import java.sql.SQLException

class ProductDao {
    private fun getConn() = Database.getConnection()

    fun findAll(): List<Product> {
        val conn = getConn() ?: return emptyList()
        return try {
            val products = mutableListOf<Product>()
            val rs = conn.createStatement().executeQuery(
                """
                SELECT id, sku, name, price, stock_quantity, category, active 
                FROM product 
                WHERE active = 1 
                ORDER BY name
                """
            )

            while (rs.next()) {
                products.add(rs.toProduct())
            }

            products
        } catch (e: SQLException) {
            println("✗ Erro ao buscar produtos: ${e.message}")
            emptyList()
        }
    }

    fun findBySku(sku: String): Product? {
        val conn = getConn() ?: return null
        return try {
            val stmt = conn.prepareStatement(
                """
                SELECT id, sku, name, price, stock_quantity, category, active 
                FROM product 
                WHERE sku = ? AND active = 1
                """
            )
            stmt.setString(1, sku.trim())
            val rs = stmt.executeQuery()

            if (rs.next()) {
                rs.toProduct()
            } else {
                null
            }
        } catch (e: SQLException) {
            println("✗ Erro ao buscar produto por SKU: ${e.message}")
            null
        }
    }

    fun findById(id: Long): Product? {
        val conn = getConn() ?: return null
        return try {
            val stmt = conn.prepareStatement(
                """
                SELECT id, sku, name, price, stock_quantity, category, active 
                FROM product 
                WHERE id = ?
                """
            )
            stmt.setLong(1, id)
            val rs = stmt.executeQuery()

            if (rs.next()) rs.toProduct() else null
        } catch (e: SQLException) {
            println("✗ Erro ao buscar produto por ID: ${e.message}")
            null
        }
    }

    fun save(product: Product): Long {
        val conn = getConn() ?: return 0L
        return try {
            val stmt = conn.prepareStatement(
                """
                INSERT INTO product (sku, name, price, stock_quantity, category) 
                VALUES (?, ?, ?, ?, ?)
                """,
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            stmt.setString(1, product.sku.trim())
            stmt.setString(2, product.name.trim())
            stmt.setDouble(3, product.price)
            stmt.setInt(4, product.stockQuantity)
            stmt.setString(5, product.category?.trim())
            stmt.executeUpdate()

            val rs = stmt.generatedKeys
            val id = if (rs.next()) rs.getLong(1) else 0L

            println("✓ Produto salvo: ${product.name} (ID: $id)")
            id
        } catch (e: SQLException) {
            println("✗ Erro ao salvar produto: ${e.message}")
            0L
        }
    }

    fun update(product: Product): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                """
                UPDATE product 
                SET name = ?, price = ?, stock_quantity = ?, category = ?, updated_at = datetime('now')
                WHERE sku = ?
                """
            )
            stmt.setString(1, product.name.trim())
            stmt.setDouble(2, product.price)
            stmt.setInt(3, product.stockQuantity)
            stmt.setString(4, product.category?.trim())
            stmt.setString(5, product.sku.trim())

            val updated = stmt.executeUpdate() > 0

            if (updated) {
                println("✓ Produto atualizado: ${product.name}")
            }

            updated
        } catch (e: SQLException) {
            println("✗ Erro ao atualizar produto: ${e.message}")
            false
        }
    }

    fun delete(sku: String): Boolean {
        val conn = getConn() ?: return false
        return try {
            // Soft delete
            val stmt = conn.prepareStatement(
                "UPDATE product SET active = 0, updated_at = datetime('now') WHERE sku = ?"
            )
            stmt.setString(1, sku.trim())

            val deleted = stmt.executeUpdate() > 0

            if (deleted) {
                println("✓ Produto desativado: $sku")
            }

            deleted
        } catch (e: SQLException) {
            println("✗ Erro ao deletar produto: ${e.message}")
            false
        }
    }

    fun updateStock(productId: Long, newQuantity: Int): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                """
                UPDATE product 
                SET stock_quantity = ?, updated_at = datetime('now') 
                WHERE id = ?
                """
            )
            stmt.setInt(1, newQuantity)
            stmt.setLong(2, productId)

            stmt.executeUpdate() > 0
        } catch (e: SQLException) {
            println("✗ Erro ao atualizar estoque: ${e.message}")
            false
        }
    }

    fun getLowStockProducts(threshold: Int = 10): List<Product> {
        val conn = getConn() ?: return emptyList()
        return try {
            val products = mutableListOf<Product>()
            val stmt = conn.prepareStatement(
                """
                SELECT id, sku, name, price, stock_quantity, category, active 
                FROM product 
                WHERE active = 1 AND stock_quantity < ? 
                ORDER BY stock_quantity ASC
                """
            )
            stmt.setInt(1, threshold)
            val rs = stmt.executeQuery()

            while (rs.next()) {
                products.add(rs.toProduct())
            }

            products
        } catch (e: SQLException) {
            println("✗ Erro ao buscar produtos com estoque baixo: ${e.message}")
            emptyList()
        }
    }

    private fun ResultSet.toProduct() = Product(
        id = getLong("id"),
        sku = getString("sku"),
        name = getString("name"),
        price = getDouble("price"),
        stockQuantity = getInt("stock_quantity"),
        category = getString("category"),
        active = getInt("active") == 1
    )
}

