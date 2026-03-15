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
                INSERT INTO sale (date_time, total, subtotal, discount, payment_method, status, operator_name, client_id, client_discount) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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

                // client info
                if (sale.clientId == null) {
                    saleStmt.setNull(8, java.sql.Types.INTEGER)
                } else {
                    saleStmt.setLong(8, sale.clientId)
                }
                saleStmt.setDouble(9, sale.clientDiscount)

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

                // Se a venda trouxe partes de pagamento, persistir na mesma transação
                try {
                    if (sale.paymentParts.isNotEmpty()) {
                        val partStmt = conn.prepareStatement("INSERT INTO payment_part (sale_id, method, amount, auth_code, created_at) VALUES (?, ?, ?, ?, datetime('now'))")
                        sale.paymentParts.forEach { p ->
                            partStmt.setLong(1, saleId)
                            partStmt.setString(2, p.first)
                            partStmt.setDouble(3, p.second)
                            val code = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
                            partStmt.setString(4, code)
                            partStmt.executeUpdate()
                        }
                        println("→ Partes de pagamento salvas: ${sale.paymentParts.size}")
                    }
                } catch (e: Exception) {
                    println("✗ Falha ao salvar partes de pagamento dentro da venda: ${e.message}")
                }

                // Criar dívida do cliente:
                // - chargeToAccount=true: toda a venda vai na conta (dívida = total, amountPaid = que foi pago agora)
                // - chargeToAccount=false: apenas o que não foi coberto pelos pagamentos vira dívida
                try {
                    val paid = if (sale.paymentParts.isNotEmpty()) sale.paymentParts.sumOf { it.second } else 0.0
                    if (sale.clientId != null && sale.chargeToAccount) {
                        // Lançar venda inteira na conta do cliente
                        val amountDue  = sale.total          // valor total da venda
                        val amountPaid = paid.coerceAtMost(amountDue) // já pago agora
                        val status     = if (amountPaid >= amountDue) "CLOSED" else "OPEN"
                        val debtStmt = conn.prepareStatement(
                            "INSERT INTO client_debt (client_id, sale_id, amount_due, amount_paid, description, status) VALUES (?, ?, ?, ?, ?, ?)"
                        )
                        debtStmt.setLong(1, sale.clientId)
                        debtStmt.setLong(2, saleId)
                        debtStmt.setDouble(3, amountDue)
                        debtStmt.setDouble(4, amountPaid)
                        debtStmt.setString(5, "Venda #$saleId fiado")
                        debtStmt.setString(6, status)
                        debtStmt.executeUpdate()
                        println("→ Venda lançada na conta do cliente ${sale.clientId}: total R$ $amountDue, pago R$ $amountPaid, status=$status")
                    } else if (sale.clientId != null) {
                        // Criar dívida apenas pelo valor não coberto
                        val outstanding = (sale.total - paid).coerceAtLeast(0.0)
                        if (outstanding > 0.0) {
                            val debtStmt = conn.prepareStatement(
                                "INSERT INTO client_debt (client_id, sale_id, amount_due, amount_paid, description, status) VALUES (?, ?, ?, ?, ?, ?)"
                            )
                            debtStmt.setLong(1, sale.clientId)
                            debtStmt.setLong(2, saleId)
                            debtStmt.setDouble(3, outstanding)
                            debtStmt.setDouble(4, 0.0)
                            debtStmt.setString(5, "Venda #$saleId - saldo aberto")
                            debtStmt.setString(6, "OPEN")
                            debtStmt.executeUpdate()
                            println("→ Dívida criada para cliente ${sale.clientId}, R$ $outstanding (venda $saleId)")
                        }
                    }
                } catch (e: Exception) {
                    println("✗ Falha ao criar registro de dívida do cliente: ${e.message}")
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

                        // se tiver clientId, incluir informações do cliente no payload
                        try {
                            if (sale.clientId != null) {
                                val clientDao = ClientDao()
                                val c = clientDao.findById(sale.clientId)
                                if (c != null) {
                                    val clientObj = JSONObject()
                                    clientObj.put("id", c.id)
                                    clientObj.put("name", c.name)
                                    clientObj.put("document", c.document)
                                    clientObj.put("phone", c.phone)
                                    clientObj.put("email", c.email)
                                    clientObj.put("default_discount_percent", c.defaultDiscountPercent)
                                    payload.put("client", clientObj)
                                }
                            }
                        } catch (e: Exception) {
                            println("✗ Falha ao anexar cliente ao payload: ${e.message}")
                        }
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
        val saleId = rs.getLong("id")
        // carregar partes de pagamento
        val partDao = PaymentPartDao()
        val parts = try {
            partDao.findBySaleId(saleId).map { it.method to it.amount }
        } catch (e: Exception) {
            emptyList()
        }

        // try to read client info if present
        val clientId = try { rs.getLong("client_id") } catch (_: Exception) { 0L }
        val clientDiscount = try { rs.getDouble("client_discount") } catch (_: Exception) { 0.0 }

        return Sale(
            id = saleId,
            dateTime = rs.getString("date_time"),
            items = emptyList(),
            discount = rs.getDouble("discount"),
            paymentMethod = rs.getString("payment_method"),
            status = rs.getString("status"),
            operatorName = rs.getString("operator_name"),
            paymentParts = parts,
            _total = rs.getDouble("total"),
            _subtotal = rs.getDouble("subtotal"),
            clientId = if (clientId == 0L) null else clientId,
            clientDiscount = clientDiscount
        )
    }
}
