package com.pdv.server

import com.pdv.data.SaleDao
import com.pdv.data.CashRegisterDao
import com.pdv.data.Sale
import com.pdv.data.SaleItem
import com.pdv.data.Product
import com.pdv.data.UserDao
import com.pdv.data.User
import com.pdv.data.Config
import java.io.InputStreamReader
import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject
import com.sun.net.httpserver.HttpsServer
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

object EmbeddedServer {
    private var server: HttpServer? = null
    private val saleDao = SaleDao()
    private val cashDao = CashRegisterDao()
    private val userDao = UserDao()

    fun start(port: Int = 8080, bind: String = "127.0.0.1") {
        if (server != null) return
        try {
            if (Config.serverSslEnabled && Config.serverKeystorePath.isNotBlank()) {
                // try to start HTTPS server
                val ksStream = FileInputStream(Config.serverKeystorePath)
                val ks = KeyStore.getInstance(KeyStore.getDefaultType())
                ks.load(ksStream, Config.serverKeystorePassword.toCharArray())
                val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                kmf.init(ks, Config.serverKeystorePassword.toCharArray())
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(kmf.keyManagers, null, null)

                val https = HttpsServer.create(InetSocketAddress(bind, port), 0)
                https.httpsConfigurator = com.sun.net.httpserver.HttpsConfigurator(sslContext)
                https.executor = Executors.newFixedThreadPool(4)

                https.createContext("/health", HealthHandler())
                https.createContext("/api/auth", AuthHandler())
                https.createContext("/api/sales", SalesHandler())
                https.createContext("/api/cash/movements", CashMovementsHandler())
                https.createContext("/api/cash/withdraw", CashWithdrawHandler())
                https.start()
                server = https
                println("✓ Embedded HTTPS Server iniciado em $bind:$port")
                return
            }
        } catch (e: Exception) {
            println("✗ Falha ao iniciar HTTPS (fallback para HTTP): ${e.message}")
        }

        server = HttpServer.create(InetSocketAddress(bind, port), 0)
        server?.executor = Executors.newFixedThreadPool(4)

        server?.createContext("/health", HealthHandler())
        server?.createContext("/api/auth", AuthHandler())
        server?.createContext("/api/sales", SalesHandler())
        server?.createContext("/api/cash/movements", CashMovementsHandler())
        server?.createContext("/api/cash/withdraw", CashWithdrawHandler())

        server?.start()
        println("✓ EmbeddedServer iniciado em $bind:$port")
    }

    fun startFromConfig() {
        val enabled = Config.serverEnabled
        if (!enabled) return
        start(Config.serverPort, Config.serverBind)
    }

    fun stop() {
        server?.stop(1)
        server = null
        println("✓ EmbeddedServer parado")
    }

    private fun checkAuth(exchange: HttpExchange): Boolean {
        val token = Config.serverToken
        if (token.isNullOrBlank()) return true // auth disabled
        val auth = exchange.requestHeaders.getFirst("Authorization") ?: return false
        return auth == "Bearer $token"
    }

    class HealthHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val response = "{\"status\": \"ok\"}"
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { os ->
                os.write(response.toByteArray())
            }
        }
    }

    // Autenticação via API (POST /api/auth {username,password})
    class AuthHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (exchange.requestMethod != "POST") {
                    exchange.sendResponseHeaders(405, -1)
                    return
                }
                val body = InputStreamReader(exchange.requestBody).readText()
                val json = JSONObject(body)
                val username = json.optString("username", "").trim()
                val password = json.optString("password", "")

                val user = userDao.authenticate(username, password)
                if (user == null) {
                    exchange.sendResponseHeaders(401, -1)
                    return
                }

                val resp = JSONObject()
                resp.put("id", user.id)
                resp.put("username", user.username)
                resp.put("fullName", user.fullName)
                resp.put("role", user.role.name)
                val respStr = resp.toString()
                exchange.sendResponseHeaders(200, respStr.toByteArray().size.toLong())
                exchange.responseBody.use { os -> os.write(respStr.toByteArray()) }
            } catch (e: Exception) {
                val resp = JSONObject().put("error", e.message ?: "erro").toString()
                exchange.sendResponseHeaders(500, resp.toByteArray().size.toLong())
                exchange.responseBody.use { os -> os.write(resp.toByteArray()) }
            }
        }
    }

    class SalesHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (!checkAuth(exchange)) {
                    exchange.sendResponseHeaders(401, -1)
                    return
                }

                if (exchange.requestMethod == "POST") {
                    val body = InputStreamReader(exchange.requestBody).readText()
                    val json = JSONObject(body)

                    val itemsJson = json.optJSONArray("items") ?: JSONArray()
                    val items = mutableListOf<SaleItem>()
                    for (i in 0 until itemsJson.length()) {
                        val it = itemsJson.getJSONObject(i)
                        val prod = Product(
                            id = it.optLong("product_id", 0L),
                            sku = it.optString("sku", ""),
                            name = it.optString("name", ""),
                            price = it.optDouble("unit_price", 0.0)
                        )
                        val saleItem = SaleItem(product = prod, quantity = it.optDouble("quantity", 1.0), discount = it.optDouble("discount", 0.0))
                        items.add(saleItem)
                    }

                    val sale = Sale(
                        items = items,
                        discount = json.optDouble("discount", 0.0),
                        paymentMethod = json.optString("payment_method", null),
                        operatorName = json.optString("operator_name", null)
                    )

                    val saleId = saleDao.save(sale)
                    val resp = JSONObject().put("saleId", saleId).toString()
                    exchange.sendResponseHeaders(201, resp.toByteArray().size.toLong())
                    exchange.responseBody.use { os -> os.write(resp.toByteArray()) }
                    return
                } else if (exchange.requestMethod == "GET") {
                    val query = exchange.requestURI.query ?: ""
                    val params = query.split("&").mapNotNull {
                        val parts = it.split("=")
                        if (parts.size == 2) parts[0] to parts[1] else null
                    }.toMap()

                    val start = params["start"] ?: "1970-01-01"
                    val end = params["end"] ?: "2100-01-01"

                    val sales = saleDao.findByPeriod(start, end)
                    val arr = JSONArray()
                    sales.forEach { s ->
                        val jo = JSONObject()
                        jo.put("id", s.id)
                        jo.put("date_time", s.dateTime)
                        jo.put("total", s.total)
                        jo.put("subtotal", s.subtotal)
                        jo.put("discount", s.discount)
                        jo.put("payment_method", s.paymentMethod)
                        jo.put("status", s.status)
                        jo.put("operator_name", s.operatorName)
                        arr.put(jo)
                    }
                    val resp = arr.toString()
                    exchange.sendResponseHeaders(200, resp.toByteArray().size.toLong())
                    exchange.responseBody.use { os -> os.write(resp.toByteArray()) }
                    return
                }
                exchange.sendResponseHeaders(405, -1)
            } catch (e: Exception) {
                val resp = JSONObject().put("error", e.message ?: "erro").toString()
                exchange.sendResponseHeaders(500, resp.toByteArray().size.toLong())
                exchange.responseBody.use { os -> os.write(resp.toByteArray()) }
            }
        }
    }

    class CashMovementsHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (!checkAuth(exchange)) {
                    exchange.sendResponseHeaders(401, -1)
                    return
                }
                if (exchange.requestMethod != "GET") {
                    exchange.sendResponseHeaders(405, -1)
                    return
                }
                val query = exchange.requestURI.query ?: ""
                val params = query.split("&").mapNotNull {
                    val parts = it.split("=")
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap()

                val start = params["start"] ?: "1970-01-01"
                val end = params["end"] ?: "2100-01-01"

                val movements = cashDao.findByPeriod(start, end)
                val arr = org.json.JSONArray()
                movements.forEach { m ->
                    val jo = JSONObject()
                    jo.put("id", m["id"].toString())
                    jo.put("session_id", m["session_id"].toString())
                    jo.put("type", m["type"].toString())
                    jo.put("amount", m["amount"].toString())
                    jo.put("description", m["description"].toString())
                    jo.put("created_at", m["created_at"].toString())
                    arr.put(jo)
                }
                val resp = arr.toString()
                exchange.sendResponseHeaders(200, resp.toByteArray().size.toLong())
                exchange.responseBody.use { os -> os.write(resp.toByteArray()) }
            } catch (e: Exception) {
                val resp = JSONObject().put("error", e.message ?: "erro").toString()
                exchange.sendResponseHeaders(500, resp.toByteArray().size.toLong())
                exchange.responseBody.use { os -> os.write(resp.toByteArray()) }
            }
        }
    }

    class CashWithdrawHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                if (!checkAuth(exchange)) {
                    exchange.sendResponseHeaders(401, -1)
                    return
                }
                if (exchange.requestMethod != "POST") {
                    exchange.sendResponseHeaders(405, -1)
                    return
                }
                val body = InputStreamReader(exchange.requestBody).readText()
                val json = JSONObject(body)
                val sessionId = json.optLong("session_id", 0L)
                val amount = json.optDouble("amount", 0.0)
                val description = json.optString("description", "Sangria")
                val operator = json.optString("operator", null)

                val id = cashDao.registerMovement(sessionId, "WITHDRAW", amount, description, operator)
                val resp = JSONObject().put("id", id).toString()
                exchange.sendResponseHeaders(201, resp.toByteArray().size.toLong())
                exchange.responseBody.use { os -> os.write(resp.toByteArray()) }
            } catch (e: Exception) {
                val resp = JSONObject().put("error", e.message ?: "erro").toString()
                exchange.sendResponseHeaders(500, resp.toByteArray().size.toLong())
                exchange.responseBody.use { os -> os.write(resp.toByteArray()) }
            }
        }
    }
}
