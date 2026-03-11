package com.pdv.data

import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

class SaleDao {
    private fun getConn() = Database.getConnection()
    private val outbox = OutboxDao()

    fun save(sale: Sale): Long {
        val conn = getConn()
        if (conn == null) {
            println("✗ Erro: Conexão com banco de dados não disponível")
            return 0L
        }

        return try {
            val sql = """
                INSERT INTO sale (date_time, total, subtotal, discount, payment_method, status, operator_name) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """

            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { saleStmt ->
                saleStmt.setString(1, now)
                saleStmt.setDouble(2, sale.total)
                saleStmt.setDouble(3, sale.subtotal)
                saleStmt.setDouble(4, sale.totalDiscount)
                // paymentMethod may be null
                if (sale.paymentMethod == null) {
                    saleStmt.setNull(5, java.sql.Types.VARCHAR)
                } else {
                    saleStmt.setString(5, sale.paymentMethod)
                }
                saleStmt.setString(6, sale.status)
                if (sale.operatorName == null) {
                    saleStmt.setNull(7, java.sql.Types.VARCHAR)
                } else {
                    saleStmt.setString(7, sale.operatorName)
                }

                val rowsAffected = saleStmt.executeUpdate()
                println("✓ INSERT venda executado, rows affected: $rowsAffected")

                if (rowsAffected == 0) {
                    println("✗ Nenhuma linha inserida")
                    return 0L
                }

                val generatedKeys = saleStmt.generatedKeys
                var saleId: Long = 0L
                try {
                    if (generatedKeys == null) {
                        println("→ generatedKeys is null")
                    } else {
                        println("→ generatedKeys available: meta = ${generatedKeys.metaData?.columnCount}")
                        if (generatedKeys.next()) {
                            saleId = generatedKeys.getLong(1)
                            println("→ generatedKeys.next() = true, id = $saleId")
                        } else {
                            println("→ generatedKeys.next() = false")
                        }
                    }
                } catch (e: Exception) {
                    println("✗ Erro lendo generatedKeys: ${e.message}")
                }

                if (saleId == 0L) {
                    // fallback para last_insert_rowid()
                    try {
                        conn.createStatement().use { idStmt ->
                            idStmt.executeQuery("SELECT last_insert_rowid()") .use { idRs ->
                                if (idRs.next()) {
                                    saleId = idRs.getLong(1)
                                    println("→ last_insert_rowid() = $saleId")
                                } else {
                                    println("→ last_insert_rowid() retornou vazio")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("✗ Erro ao executar fallback last_insert_rowid(): ${e.message}")
                    }
                }

                if (saleId == 0L) {
                    println("✗ Erro: Não foi possível obter ID da venda")
                    return 0L
                }

                println("✓ Venda criada com ID: $saleId")

                val itemSql = """
                INSERT INTO sale_item (sale_id, product_id, quantity, unit_price, total_price, discount) 
                VALUES (?, ?, ?, ?, ?, ?)
                """

                conn.prepareStatement(itemSql).use { itemStmt ->
                    sale.items.forEach { item ->
                        itemStmt.setLong(1, saleId)
                        itemStmt.setLong(2, item.product.id)
                        itemStmt.setDouble(3, item.quantity)
                        itemStmt.setDouble(4, item.unitPrice)
                        itemStmt.setDouble(5, item.total)
                        itemStmt.setDouble(6, item.discount)
                        itemStmt.executeUpdate()
                        println("  ✓ Item salvo: ${item.product.name} x${item.quantity}")
                    }
                }

                println("✓ Venda salva com sucesso (ID: $saleId, Total: R$ %.2f)".format(sale.total))

                // Se remote configured, enfileirar payload para sync
                try {
                    if (!Config.remoteUrl.isBlank()) {
                        val payload = JSONObject()
                        payload.put("client_uuid", java.util.UUID.randomUUID().toString())
                        payload.put("sale_id", saleId)
                        payload.put("date_time", now)
                        payload.put("total", sale.total)
                        payload.put("subtotal", sale.subtotal)
                        payload.put("discount", sale.totalDiscount)
                        payload.put("payment_method", sale.paymentMethod)
                        payload.put("operator_name", sale.operatorName)
                        val itemsArr = JSONArray()
                        sale.items.forEach { it2 ->
                            val jo = JSONObject()
                            jo.put("product_id", it2.product.id)
                            jo.put("sku", it2.product.sku)
                            jo.put("name", it2.product.name)
                            jo.put("unit_price", it2.product.price)
                            jo.put("quantity", it2.quantity)
                            jo.put("total", it2.total)
                            itemsArr.put(jo)
                        }
                        payload.put("items", itemsArr)
                        outbox.enqueue(payload.getString("client_uuid"), payload.toString())
                        println("→ Venda enfileirada para sincronização remota (outbox)")
                    }
                } catch (e: Exception) {
                    println("✗ Falha ao enfileirar outbox: ${e.message}")
                }

                saleId
            }
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
