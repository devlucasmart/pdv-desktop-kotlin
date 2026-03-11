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
                SELECT id, sku, name, price, stock_quantity, unit, category, active 
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
                SELECT id, sku, name, price, stock_quantity, unit, category, active 
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
                SELECT id, sku, name, price, stock_quantity, unit, category, active 
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
        val skuTrim = product.sku.trim()
        if (skuTrim.isEmpty()) {
            println("✗ SKU vazio ao salvar produto")
            return 0L
        }

        // First attempt: try UPSERT (preferred)
        try {
            val sql = """
                INSERT INTO product (sku, name, price, stock_quantity, unit, category, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 1, datetime('now'), datetime('now'))
                ON CONFLICT(sku) DO UPDATE SET
                    name = excluded.name,
                    price = excluded.price,
                    stock_quantity = excluded.stock_quantity,
                    unit = excluded.unit,
                    category = excluded.category,
                    active = 1,
                    updated_at = datetime('now')
            """

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, skuTrim)
                stmt.setString(2, product.name.trim())
                stmt.setDouble(3, product.price)
                stmt.setDouble(4, product.stockQuantity)
                stmt.setString(5, product.unit)
                stmt.setString(6, product.category?.trim())
                stmt.executeUpdate()
            }

            val inserted = findBySku(skuTrim)
            val id = inserted?.id ?: 0L
            println("✓ Produto salvo/atualizado (upsert): ${product.name} (ID: $id)")
            return id
        } catch (e: SQLException) {
            // UPSERT may not be supported by older SQLite/JDBC configurations; fallback silently
            println("→ UPSERT não suportado - usando fallback seguro")
            // fallback to manual upsert below
        }

        // Fallback: manual upsert using transaction (works on older SQLite/JDBC)
        try {
            conn.autoCommit = false
            try {
                val existing = findBySku(skuTrim)
                if (existing != null) {
                    // update
                    val ok = update(product.copy(id = existing.id, sku = skuTrim))
                    if (!ok) throw SQLException("Falha ao atualizar produto existente (fallback)")
                } else {
                    // insert and try to get generated id
                    val insertSql = "INSERT INTO product (sku, name, price, stock_quantity, unit, category, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 1, datetime('now'), datetime('now'))"
                    conn.prepareStatement(insertSql).use { istmt ->
                        istmt.setString(1, skuTrim)
                        istmt.setString(2, product.name.trim())
                        istmt.setDouble(3, product.price)
                        istmt.setDouble(4, product.stockQuantity)
                        istmt.setString(5, product.unit)
                        istmt.setString(6, product.category?.trim())
                        istmt.executeUpdate()
                    }
                }
                conn.commit()
            } catch (inner: Exception) {
                try { conn.rollback() } catch (_: Exception) {}
                throw inner
            } finally {
                try { conn.autoCommit = true } catch (_: Exception) {}
            }

            val inserted = findBySku(skuTrim)
            val id = inserted?.id ?: 0L
            println("✓ Produto salvo/atualizado (fallback): ${product.name} (ID: $id)")
            return id
        } catch (e: Exception) {
            println("✗ Erro ao salvar produto (fallback): ${e.message}")
            return 0L
        }
    }

    fun update(product: Product): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                """
                UPDATE product 
                SET name = ?, price = ?, stock_quantity = ?, unit = ?, category = ?, updated_at = datetime('now')
                WHERE sku = ?
                """
            )
            stmt.setString(1, product.name.trim())
            stmt.setDouble(2, product.price)
            stmt.setDouble(3, product.stockQuantity)
            stmt.setString(4, product.unit)
            stmt.setString(5, product.category?.trim())
            stmt.setString(6, product.sku.trim())

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

    fun updateStock(productId: Long, newQuantity: Double): Boolean {
        val conn = getConn() ?: return false
        return try {
            val stmt = conn.prepareStatement(
                """
                UPDATE product 
                SET stock_quantity = ?, updated_at = datetime('now') 
                WHERE id = ?
                """
            )
            stmt.setDouble(1, newQuantity)
            stmt.setLong(2, productId)

            stmt.executeUpdate() > 0
        } catch (e: SQLException) {
            println("✗ Erro ao atualizar estoque: ${e.message}")
            false
        }
    }

    fun getLowStockProducts(threshold: Double = 10.0): List<Product> {
        val conn = getConn() ?: return emptyList()
        return try {
            val products = mutableListOf<Product>()
            val stmt = conn.prepareStatement(
                """
                SELECT id, sku, name, price, stock_quantity, unit, category, active 
                FROM product 
                WHERE active = 1 AND stock_quantity < ? 
                ORDER BY stock_quantity ASC
                """
            )
            stmt.setDouble(1, threshold)
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

    // Busca por termo em SKU ou nome (usado para input manual/fuzzy search)
    fun findByQuery(query: String): List<Product> {
        val conn = getConn() ?: return emptyList()
        return try {
            val q = "%${query.trim()}%"
            val stmt = conn.prepareStatement(
                """
                SELECT id, sku, name, price, stock_quantity, unit, category, active 
                FROM product 
                WHERE active = 1 AND (sku LIKE ? OR name LIKE ?)
                ORDER BY name
                """
            )
            stmt.setString(1, q)
            stmt.setString(2, q)
            val rs = stmt.executeQuery()
            val products = mutableListOf<Product>()
            while (rs.next()) products.add(rs.toProduct())
            products
        } catch (e: SQLException) {
            println("✗ Erro ao buscar produto por query: ${e.message}")
            emptyList()
        }
    }

    private fun ResultSet.toProduct() = Product(
        id = getLong("id"),
        sku = getString("sku"),
        name = getString("name"),
        price = getDouble("price"),
        stockQuantity = getDouble("stock_quantity"),
        unit = try { getString("unit") } catch (_: Exception) { "un" },
        category = getString("category"),
        active = getInt("active") == 1
    )
}
