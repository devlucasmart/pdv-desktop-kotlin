package com.pdv.data

import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SaleDao {
    private fun getConn() = Database.getConnection()

    fun save(sale: Sale): Long {
        val conn = getConn()
        if (conn == null) {
            println("✗ Erro: Conexão com banco de dados não disponível")
            return 0L
        }

        return try {
            val saleStmt = conn.prepareStatement(
                """
                INSERT INTO sale (date_time, total, subtotal, discount, payment_method, status, operator_name) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """
            )

            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            saleStmt.setString(1, now)
            saleStmt.setDouble(2, sale.total)
            saleStmt.setDouble(3, sale.subtotal)
            saleStmt.setDouble(4, sale.totalDiscount)
            saleStmt.setString(5, sale.paymentMethod)
            saleStmt.setString(6, sale.status)
            saleStmt.setString(7, sale.operatorName)

            val rowsAffected = saleStmt.executeUpdate()
            println("✓ INSERT venda executado, rows affected: $rowsAffected")

            if (rowsAffected == 0) {
                println("✗ Nenhuma linha inserida")
                return 0L
            }

            val idStmt = conn.createStatement()
            val idRs = idStmt.executeQuery("SELECT last_insert_rowid()")
            val saleId = if (idRs.next()) idRs.getLong(1) else 0L

            if (saleId == 0L) {
                println("✗ Erro: Não foi possível obter ID da venda")
                return 0L
            }

            println("✓ Venda criada com ID: $saleId")

            val itemStmt = conn.prepareStatement(
                """
                INSERT INTO sale_item (sale_id, product_id, quantity, unit_price, total_price, discount) 
                VALUES (?, ?, ?, ?, ?, ?)
                """
            )

            sale.items.forEach { item ->
                itemStmt.setLong(1, saleId)
                itemStmt.setLong(2, item.product.id)
                itemStmt.setInt(3, item.quantity)
                itemStmt.setDouble(4, item.unitPrice)
                itemStmt.setDouble(5, item.total)
                itemStmt.setDouble(6, item.discount)
                itemStmt.executeUpdate()
                println("  ✓ Item salvo: ${item.product.name} x${item.quantity}")
            }

            println("✓ Venda salva com sucesso (ID: $saleId, Total: R$ %.2f)".format(sale.total))
            saleId
        } catch (e: SQLException) {
            println("✗ Erro SQL ao salvar venda: ${e.message}")
            e.printStackTrace()
            0L
        } catch (e: Exception) {
            println("✗ Erro geral ao salvar venda: ${e.message}")
            e.printStackTrace()
            0L
        }
    }

    fun getTotalSales(): Double {
        val conn = getConn() ?: return 0.0
        return try {
            val rs = conn.createStatement().executeQuery(
                "SELECT COALESCE(SUM(total), 0.0) as total FROM sale WHERE status = 'COMPLETED'"
            )
            if (rs.next()) rs.getDouble("total") else 0.0
        } catch (e: SQLException) {
            println("✗ Erro ao obter total de vendas: ${e.message}")
            0.0
        }
    }

    fun getSalesCount(): Int {
        val conn = getConn() ?: return 0
        return try {
            val rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) as count FROM sale WHERE status = 'COMPLETED'"
            )
            if (rs.next()) rs.getInt("count") else 0
        } catch (e: SQLException) {
            println("✗ Erro ao obter contagem de vendas: ${e.message}")
            0
        }
    }

    fun getSalesToday(): Double {
        val conn = getConn() ?: return 0.0
        return try {
            val today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val stmt = conn.prepareStatement(
                """
                SELECT COALESCE(SUM(total), 0.0) as total 
                FROM sale 
                WHERE date(date_time) = date(?) AND status = 'COMPLETED'
                """
            )
            stmt.setString(1, today)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getDouble("total") else 0.0
        } catch (e: SQLException) {
            println("✗ Erro ao obter vendas do dia: ${e.message}")
            0.0
        }
    }

    fun getSalesCountToday(): Int {
        val conn = getConn() ?: return 0
        return try {
            val today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val stmt = conn.prepareStatement(
                """
                SELECT COUNT(*) as count 
                FROM sale 
                WHERE date(date_time) = date(?) AND status = 'COMPLETED'
                """
            )
            stmt.setString(1, today)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getInt("count") else 0
        } catch (e: SQLException) {
            println("✗ Erro ao obter contagem de vendas do dia: ${e.message}")
            0
        }
    }

    fun getAverageTicket(): Double {
        val conn = getConn() ?: return 0.0
        return try {
            val rs = conn.createStatement().executeQuery(
                """
                SELECT COALESCE(AVG(total), 0.0) as avg_ticket 
                FROM sale 
                WHERE status = 'COMPLETED'
                """
            )
            if (rs.next()) rs.getDouble("avg_ticket") else 0.0
        } catch (e: SQLException) {
            println("✗ Erro ao obter ticket médio: ${e.message}")
            0.0
        }
    }

    fun findAll(): List<Sale> {
        val conn = getConn() ?: return emptyList()
        return try {
            val sales = mutableListOf<Sale>()
            val rs = conn.createStatement().executeQuery(
                "SELECT id, date_time, total, subtotal, discount, payment_method, status, operator_name FROM sale ORDER BY date_time DESC"
            )
            while (rs.next()) {
                sales.add(createSaleFromResultSet(rs))
            }
            sales
        } catch (e: SQLException) {
            println("✗ Erro ao buscar vendas: ${e.message}")
            emptyList()
        }
    }

    fun findToday(): List<Sale> {
        val conn = getConn() ?: return emptyList()
        return try {
            val sales = mutableListOf<Sale>()
            val today = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val stmt = conn.prepareStatement(
                "SELECT id, date_time, total, subtotal, discount, payment_method, status, operator_name FROM sale WHERE date_time LIKE ? ORDER BY date_time DESC"
            )
            stmt.setString(1, "$today%")
            val rs = stmt.executeQuery()
            while (rs.next()) {
                sales.add(createSaleFromResultSet(rs))
            }
            sales
        } catch (e: SQLException) {
            println("✗ Erro ao buscar vendas de hoje: ${e.message}")
            emptyList()
        }
    }

    fun findByPeriod(startDate: String, endDate: String): List<Sale> {
        val conn = getConn() ?: return emptyList()
        return try {
            val sales = mutableListOf<Sale>()
            val stmt = conn.prepareStatement(
                """
                SELECT id, date_time, total, subtotal, discount, payment_method, status, operator_name 
                FROM sale 
                WHERE date(date_time) >= date(?) AND date(date_time) <= date(?)
                ORDER BY date_time DESC
                """
            )
            stmt.setString(1, startDate)
            stmt.setString(2, endDate)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                sales.add(createSaleFromResultSet(rs))
            }
            println("📊 Encontradas ${sales.size} vendas no período $startDate a $endDate")
            sales
        } catch (e: SQLException) {
            println("✗ Erro ao buscar vendas por período: ${e.message}")
            emptyList()
        }
    }

    fun getTotalByPeriod(startDate: String, endDate: String): Double {
        val conn = getConn() ?: return 0.0
        return try {
            val stmt = conn.prepareStatement(
                """
                SELECT COALESCE(SUM(total), 0.0) as total 
                FROM sale 
                WHERE date(date_time) >= date(?) AND date(date_time) <= date(?) AND status = 'COMPLETED'
                """
            )
            stmt.setString(1, startDate)
            stmt.setString(2, endDate)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getDouble("total") else 0.0
        } catch (e: SQLException) {
            println("✗ Erro ao obter total por período: ${e.message}")
            0.0
        }
    }

    fun getCountByPeriod(startDate: String, endDate: String): Int {
        val conn = getConn() ?: return 0
        return try {
            val stmt = conn.prepareStatement(
                """
                SELECT COUNT(*) as count 
                FROM sale 
                WHERE date(date_time) >= date(?) AND date(date_time) <= date(?) AND status = 'COMPLETED'
                """
            )
            stmt.setString(1, startDate)
            stmt.setString(2, endDate)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getInt("count") else 0
        } catch (e: SQLException) {
            println("✗ Erro ao obter contagem por período: ${e.message}")
            0
        }
    }

    private fun createSaleFromResultSet(rs: java.sql.ResultSet): Sale {
        return Sale(
            id = rs.getLong("id"),
            dateTime = rs.getString("date_time"),
            items = emptyList(),
            discount = rs.getDouble("discount"),
            paymentMethod = rs.getString("payment_method"),
            status = rs.getString("status"),
            operatorName = rs.getString("operator_name"),
            _total = rs.getDouble("total"),
            _subtotal = rs.getDouble("subtotal")
        )
    }
}
