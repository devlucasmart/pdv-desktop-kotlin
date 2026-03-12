package com.pdv.data

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object Database {
    private var connection: Connection? = null

    // Usar caminho simples relativo ao diretório de trabalho
    private const val DB_URL = "jdbc:sqlite:pdv_data.db"

    @Volatile
    private var initialized = false

    @Synchronized
    fun initialize() {
        if (initialized && connection != null && connection?.isClosed == false) {
            println("✓ Banco de dados já inicializado")
            return
        }

        try {
            println("=== Inicializando banco de dados ===")
            println("📁 Diretório de trabalho: ${System.getProperty("user.dir")}")

            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection(DB_URL)
            connection?.autoCommit = true

            println("✓ Conexão estabelecida: $DB_URL")

            // Tentar migrar esquema antigo para o novo (se necessário)
            performMigrations()

            createTables()
            insertSampleData()

            initialized = true
            println("✓ Banco de dados pronto para uso")

        } catch (e: SQLException) {
            println("✗ Erro SQL: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Falha ao inicializar banco de dados", e)
        } catch (e: ClassNotFoundException) {
            println("✗ Driver JDBC SQLite não encontrado")
            e.printStackTrace()
            throw RuntimeException("Driver SQLite não encontrado", e)
        } catch (e: Exception) {
            println("✗ Erro geral: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Erro ao inicializar banco", e)
        }
    }

    @Synchronized
    fun getConnection(): Connection? {
        try {
            if (connection == null || connection?.isClosed == true) {
                println("⚠ Conexão perdida. Reconectando...")
                Class.forName("org.sqlite.JDBC")
                connection = DriverManager.getConnection(DB_URL)
                connection?.autoCommit = true
                println("✓ Reconexão bem sucedida")
            }
        } catch (e: Exception) {
            println("✗ Erro ao obter conexão: ${e.message}")
            e.printStackTrace()
        }
        return connection
    }

    @Synchronized
    fun close() {
        try {
            connection?.let {
                if (!it.isClosed) {
                    it.close()
                    println("✓ Conexão com o banco de dados fechada")
                }
            }
        } catch (e: Exception) {
            println("✗ Erro ao fechar a conexão: ${e.message}")
        } finally {
            connection = null
            initialized = false
        }
    }

    private fun createTables() {
        val conn = connection ?: return

        // Tabela de produtos (usar stock_quantity REAL para permitir frações e campo unit)
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS product (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sku TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                price REAL NOT NULL CHECK(price >= 0),
                stock_quantity REAL NOT NULL DEFAULT 0,
                unit TEXT NOT NULL DEFAULT 'un',
                category TEXT,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL DEFAULT (datetime('now')),
                updated_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """)

        // Garantir coluna unit em esquemas antigos (se já existir sem a coluna)
        try {
            val rs = conn.createStatement().executeQuery("PRAGMA table_info(product)")
            var hasUnit = false
            while (rs.next()) {
                if (rs.getString("name") == "unit") {
                    hasUnit = true
                    break
                }
            }
            if (!hasUnit) {
                try {
                    conn.createStatement().executeUpdate("ALTER TABLE product ADD COLUMN unit TEXT DEFAULT 'un'")
                    println("→ Coluna 'unit' adicionada à tabela product")
                } catch (e: Exception) {
                    println("✗ Falha ao adicionar coluna 'unit' (pode já existir): ${e.message}")
                }
            }
        } catch (e: Exception) {
            // silencioso - PRAGMA pode falhar em algumas plataformas
        }

        // Tabela de vendas
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS sale (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date_time TEXT NOT NULL,
                total REAL NOT NULL CHECK(total >= 0),
                subtotal REAL NOT NULL CHECK(subtotal >= 0),
                discount REAL NOT NULL DEFAULT 0 CHECK(discount >= 0),
                payment_method TEXT,
                status TEXT NOT NULL DEFAULT 'COMPLETED',
                operator_name TEXT,
                created_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """)

        // Tabela de itens de venda (quantity como REAL para suportar medidas fracionárias)
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS sale_item (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                quantity REAL NOT NULL CHECK(quantity > 0),
                unit_price REAL NOT NULL CHECK(unit_price >= 0),
                total_price REAL NOT NULL CHECK(total_price >= 0),
                discount REAL NOT NULL DEFAULT 0 CHECK(discount >= 0),
                FOREIGN KEY (sale_id) REFERENCES sale(id) ON DELETE CASCADE,
                FOREIGN KEY (product_id) REFERENCES product(id)
            )
        """)

        // Índices para melhor performance
        conn.createStatement().executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_product_sku ON product(sku)
        """)

        conn.createStatement().executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_sale_date ON sale(date_time)
        """)

        conn.createStatement().executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_sale_item_sale_id ON sale_item(sale_id)
        """)

        // Tabela de usuários
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS user (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                full_name TEXT NOT NULL,
                role TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """)

        // Índice para usuários
        conn.createStatement().executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_user_username ON user(username)
        """)

        // Tabela de sessões do caixa (caixa aberto/fechado)
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS cash_register (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                opened_by TEXT,
                opened_at TEXT NOT NULL,
                initial_amount REAL NOT NULL DEFAULT 0,
                closed_at TEXT,
                closing_amount REAL,
                status TEXT NOT NULL DEFAULT 'OPEN'
            )
        """)

        // Movimentos de caixa (entradas/saídas) ligados à sessão
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS cash_movement (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                amount REAL NOT NULL,
                description TEXT,
                created_at TEXT NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (session_id) REFERENCES cash_register(id) ON DELETE CASCADE
            )
        """)

        // Partes de pagamento (para vendas com pagamento dividido)
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS payment_part (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                method TEXT NOT NULL,
                amount REAL NOT NULL CHECK(amount >= 0),
                created_at TEXT NOT NULL DEFAULT (datetime('now')),
                FOREIGN KEY (sale_id) REFERENCES sale(id) ON DELETE CASCADE
            )
        """)

        // Garantir coluna auth_code (adicionada posteriormente)
        try {
            val rs = conn.createStatement().executeQuery("PRAGMA table_info(payment_part)")
            val cols = mutableSetOf<String>()
            while (rs.next()) {
                cols.add(rs.getString("name"))
            }
            if (!cols.contains("auth_code")) {
                try {
                    conn.createStatement().executeUpdate("ALTER TABLE payment_part ADD COLUMN auth_code TEXT")
                    println("→ Coluna 'auth_code' adicionada à tabela payment_part")
                } catch (e: Exception) {
                    println("✗ Falha ao adicionar coluna 'auth_code' em payment_part: ${e.message}")
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        // Tabela outbox para sincronização com servidor remoto (vendas pendentes)
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS outbox_sale (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                client_uuid TEXT NOT NULL,
                payload TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'PENDING',
                created_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """)

        conn.createStatement().executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_sale(status)
        """)

        // Nova tabela de clientes
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS client (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                document TEXT,
                phone TEXT,
                email TEXT,
                address TEXT,
                default_discount_percent REAL NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """)

        // Índice para clients
        conn.createStatement().executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_client_name ON client(name)
        """)

        // Garantir colunas client_id e client_discount na tabela sale
        try {
            val rs = conn.createStatement().executeQuery("PRAGMA table_info(sale)")
            val existingCols = mutableSetOf<String>()
            while (rs.next()) {
                existingCols.add(rs.getString("name"))
            }
            if (!existingCols.contains("client_id")) {
                try {
                    conn.createStatement().executeUpdate("ALTER TABLE sale ADD COLUMN client_id INTEGER")
                    println("→ Coluna 'client_id' adicionada à tabela sale")
                } catch (e: Exception) {
                    println("✗ Falha ao adicionar coluna 'client_id' (pode já existir): ${e.message}")
                }
            }
            if (!existingCols.contains("client_discount")) {
                try {
                    conn.createStatement().executeUpdate("ALTER TABLE sale ADD COLUMN client_discount REAL NOT NULL DEFAULT 0")
                    println("→ Coluna 'client_discount' adicionada à tabela sale")
                } catch (e: Exception) {
                    println("✗ Falha ao adicionar coluna 'client_discount' (pode já existir): ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("✗ Falha ao verificar/adicionar colunas de client em sale: ${e.message}")
        }

        println("✓ Tabelas criadas com sucesso")
    }

    private fun performMigrations() {
        val conn = connection ?: return
        try {
            // Migrar product: garantir stock_quantity REAL e coluna unit
            try {
                val rs = conn.createStatement().executeQuery("PRAGMA table_info(product)")
                var hasStockQuantity = false
                var stockType: String? = null
                var hasUnit = false
                val existingCols = mutableListOf<String>()
                while (rs.next()) {
                    val name = rs.getString("name")
                    existingCols.add(name)
                    if (name == "stock_quantity") {
                        hasStockQuantity = true
                        stockType = rs.getString("type")
                    }
                    if (name == "unit") hasUnit = true
                }

                val needsProductRewrite = (!hasStockQuantity) || (stockType != null && !stockType.equals("REAL", true))

                if (needsProductRewrite) {
                    println("→ Migrando tabela 'product' para novo esquema (stock_quantity REAL)")
                    conn.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS product_new (id INTEGER PRIMARY KEY AUTOINCREMENT, sku TEXT UNIQUE NOT NULL, name TEXT NOT NULL, price REAL NOT NULL CHECK(price >= 0), stock_quantity REAL NOT NULL DEFAULT 0, unit TEXT NOT NULL DEFAULT 'un', category TEXT, active INTEGER NOT NULL DEFAULT 1, created_at TEXT NOT NULL DEFAULT (datetime('now')), updated_at TEXT NOT NULL DEFAULT (datetime('now')))")

                    // Copiar dados existentes (preservar colunas que existem)
                    val selectCols = mutableListOf<String>()
                    if (existingCols.contains("id")) selectCols.add("id")
                    if (existingCols.contains("sku")) selectCols.add("sku")
                    if (existingCols.contains("name")) selectCols.add("name")
                    if (existingCols.contains("price")) selectCols.add("price")
                    if (existingCols.contains("stock_quantity")) selectCols.add("stock_quantity")
                    // unit pode não existir; usar default
                    if (existingCols.contains("category")) selectCols.add("category")
                    if (existingCols.contains("active")) selectCols.add("active")
                    if (existingCols.contains("created_at")) selectCols.add("created_at")
                    if (existingCols.contains("updated_at")) selectCols.add("updated_at")

                    val selectList = selectCols.joinToString(", ")
                    val insertList = listOf("id", "sku", "name", "price", "stock_quantity", "unit", "category", "active", "created_at", "updated_at")
                        .joinToString(", ")

                    val copySql = "INSERT INTO product_new (${insertList}) SELECT " +
                            (if (selectList.isBlank()) "null, '' , '' , 0.0, 0.0, 'un', null, 1, datetime('now'), datetime('now')" else {
                                // montar SELECT com fallback para unit
                                val parts = mutableListOf<String>()
                                parts.add(if (selectCols.contains("id")) "id" else "null")
                                parts.add(if (selectCols.contains("sku")) "sku" else "''")
                                parts.add(if (selectCols.contains("name")) "name" else "''")
                                parts.add(if (selectCols.contains("price")) "price" else "0.0")
                                parts.add(if (selectCols.contains("stock_quantity")) "stock_quantity" else "0.0")
                                // unit fallback
                                parts.add(if (hasUnit) "unit" else "'un'")
                                parts.add(if (selectCols.contains("category")) "category" else "null")
                                parts.add(if (selectCols.contains("active")) "active" else "1")
                                parts.add(if (selectCols.contains("created_at")) "created_at" else "datetime('now')")
                                parts.add(if (selectCols.contains("updated_at")) "updated_at" else "datetime('now')")
                                parts.joinToString(", ")
                            })

                    conn.createStatement().executeUpdate(copySql)
                    conn.createStatement().executeUpdate("DROP TABLE IF EXISTS product")
                    conn.createStatement().executeUpdate("ALTER TABLE product_new RENAME TO product")
                    conn.createStatement().executeUpdate("CREATE INDEX IF NOT EXISTS idx_product_sku ON product(sku)")

                    println("→ Migração product concluída")
                } else if (!hasUnit) {
                    // apenas adicionar coluna unit se necessário
                    try {
                        conn.createStatement().executeUpdate("ALTER TABLE product ADD COLUMN unit TEXT DEFAULT 'un'")
                        println("→ Coluna 'unit' adicionada à tabela product")
                    } catch (e: Exception) {
                        println("✗ Falha ao adicionar coluna 'unit' durante migração: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                // ignorar problemas menores de migração
                println("✗ Verificação/migração product falhou: ${e.message}")
            }

            // Migrar sale_item.quantity para REAL
            try {
                val rs2 = conn.createStatement().executeQuery("PRAGMA table_info(sale_item)")
                var hasQuantity = false
                var qtyType: String? = null
                val existingCols = mutableListOf<String>()
                while (rs2.next()) {
                    val name = rs2.getString("name")
                    existingCols.add(name)
                    if (name == "quantity") {
                        hasQuantity = true
                        qtyType = rs2.getString("type")
                    }
                }
                if (!hasQuantity || (qtyType != null && !qtyType.equals("REAL", true))) {
                    println("→ Migrando tabela 'sale_item' para usar quantity REAL")
                    conn.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS sale_item_new (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER NOT NULL, product_id INTEGER NOT NULL, quantity REAL NOT NULL CHECK(quantity > 0), unit_price REAL NOT NULL CHECK(unit_price >= 0), total_price REAL NOT NULL CHECK(total_price >= 0), discount REAL NOT NULL DEFAULT 0 CHECK(discount >= 0), FOREIGN KEY (sale_id) REFERENCES sale(id) ON DELETE CASCADE, FOREIGN KEY (product_id) REFERENCES product(id))")

                    // copiar dados existentes com fallback
                    val copySql = "INSERT INTO sale_item_new (id, sale_id, product_id, quantity, unit_price, total_price, discount) SELECT id, sale_id, product_id, quantity, unit_price, total_price, discount FROM sale_item"
                    try {
                        conn.createStatement().executeUpdate(copySql)
                    } catch (e: Exception) {
                        // se tabela original não existe, ignore
                    }

                    conn.createStatement().executeUpdate("DROP TABLE IF EXISTS sale_item")
                    conn.createStatement().executeUpdate("ALTER TABLE sale_item_new RENAME TO sale_item")
                    conn.createStatement().executeUpdate("CREATE INDEX IF NOT EXISTS idx_sale_item_sale_id ON sale_item(sale_id)")

                    println("→ Migração sale_item concluída")
                }
            } catch (e: Exception) {
                println("✗ Verificação/migração sale_item falhou: ${e.message}")
            }

            // Migrar sale.total/subtotal tipos para REAL (recriar tabela se necessário)
            try {
                val rs3 = conn.createStatement().executeQuery("PRAGMA table_info(sale)")
                val colTypes = mutableMapOf<String, String>()
                val existingCols = mutableListOf<String>()
                while (rs3.next()) {
                    val name = rs3.getString("name")
                    val type = rs3.getString("type")
                    existingCols.add(name)
                    colTypes[name] = type
                }
                val needsSaleRewrite = (colTypes["total"]?.equals("REAL", true) != true) || (colTypes["subtotal"]?.equals("REAL", true) != true)
                if (needsSaleRewrite) {
                    println("→ Migrando tabela 'sale' para novo esquema (total/subtotal REAL)")
                    conn.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS sale_new (id INTEGER PRIMARY KEY AUTOINCREMENT, date_time TEXT NOT NULL, total REAL NOT NULL CHECK(total >= 0), subtotal REAL NOT NULL CHECK(subtotal >= 0), discount REAL NOT NULL DEFAULT 0 CHECK(discount >= 0), payment_method TEXT, status TEXT NOT NULL DEFAULT 'COMPLETED', operator_name TEXT, created_at TEXT NOT NULL DEFAULT (datetime('now')))" )

                    // copiar dados
                    val copySql = StringBuilder()
                    copySql.append("INSERT INTO sale_new (id, date_time, total, subtotal, discount, payment_method, status, operator_name, created_at) SELECT ")
                    copySql.append("id, ")
                    copySql.append("date_time, ")
                    copySql.append(if (existingCols.contains("total")) "total" else "0.0")
                    copySql.append(", ")
                    copySql.append(if (existingCols.contains("subtotal")) "subtotal" else "0.0")
                    copySql.append(", ")
                    copySql.append(if (existingCols.contains("discount")) "discount" else "0.0")
                    copySql.append(", ")
                    copySql.append(if (existingCols.contains("payment_method")) "payment_method" else "null")
                    copySql.append(", ")
                    copySql.append(if (existingCols.contains("status")) "status" else "'COMPLETED'")
                    copySql.append(", ")
                    copySql.append(if (existingCols.contains("operator_name")) "operator_name" else "null")
                    copySql.append(", ")
                    copySql.append(if (existingCols.contains("created_at")) "created_at" else "datetime('now')")
                    copySql.append(" FROM sale")

                    try {
                        conn.createStatement().executeUpdate(copySql.toString())
                    } catch (e: Exception) {
                        // ignore if original table not present
                    }

                    conn.createStatement().executeUpdate("DROP TABLE IF EXISTS sale")
                    conn.createStatement().executeUpdate("ALTER TABLE sale_new RENAME TO sale")
                    conn.createStatement().executeUpdate("CREATE INDEX IF NOT EXISTS idx_sale_date ON sale(date_time)")

                    println("→ Migração sale concluída")
                }
            } catch (e: Exception) {
                println("✗ Verificação/migração sale falhou: ${e.message}")
            }

        } catch (e: Exception) {
            println("✗ Erro durante migrações: ${e.message}")
        }
    }

    private fun insertSampleData() {
        val conn = connection ?: return

        // Verifica se já existem produtos
        val countStmt = conn.createStatement()
        val rs = countStmt.executeQuery("SELECT COUNT(*) as count FROM product")
        rs.next()
        val count = rs.getInt("count")

        if (count == 0) {
            println("✓ Inserindo dados de exemplo...")

            val stmt = conn.prepareStatement("""
                INSERT INTO product (sku, name, price, stock_quantity, unit, category) 
                VALUES (?, ?, ?, ?, ?, ?)
             """)

            val sampleProducts = listOf(
                listOf("001", "Coca-Cola 2L", 8.50, 50.0, "un", "Bebidas"),
                listOf("002", "Pão Francês (kg)", 12.00, 100.0, "kg", "Padaria"),
                listOf("003", "Arroz Tipo 1 5kg", 25.90, 30.0, "un", "Alimentos"),
                listOf("004", "Feijão Preto 1kg", 7.80, 40.0, "un", "Alimentos"),
                listOf("005", "Café Torrado 500g", 15.50, 25.0, "un", "Bebidas"),
                listOf("006", "Açúcar Cristal 1kg", 4.20, 60.0, "un", "Alimentos"),
                listOf("007", "Leite Integral 1L", 5.80, 80.0, "un", "Laticínios"),
                listOf("008", "Manteiga 500g", 18.90, 20.0, "un", "Laticínios"),
                listOf("009", "Óleo de Soja 900ml", 7.50, 45.0, "un", "Alimentos"),
                listOf("010", "Macarrão Espaguete 500g", 4.50, 70.0, "un", "Alimentos")
            )

            sampleProducts.forEach { product ->
                stmt.setString(1, product[0] as String)
                stmt.setString(2, product[1] as String)
                stmt.setDouble(3, product[2] as Double)
                stmt.setDouble(4, product[3] as Double)
                stmt.setString(5, product[4] as String)
                stmt.setString(6, product[5] as String)
                stmt.executeUpdate()
            }

            println("✓ ${sampleProducts.size} produtos de exemplo inseridos")
        }

        // Verifica se já existem usuários
        val userCountStmt = conn.createStatement()
        val userRs = userCountStmt.executeQuery("SELECT COUNT(*) as count FROM user")
        userRs.next()
        val userCount = userRs.getInt("count")

        if (userCount == 0) {
            println("✓ Inserindo usuários de exemplo...")

            val userStmt = conn.prepareStatement("""
                INSERT INTO user (username, password, full_name, role) 
                VALUES (?, ?, ?, ?)
            """)

            val sampleUsers = listOf(
                listOf("admin", "admin123", "Administrador do Sistema", "ADMIN"),
                listOf("gerente", "gerente123", "João Silva - Gerente", "MANAGER"),
                listOf("caixa1", "caixa123", "Maria Santos - Caixa 1", "CASHIER"),
                listOf("caixa2", "caixa123", "Pedro Oliveira - Caixa 2", "CASHIER"),
                listOf("estoque", "estoque123", "Carlos Ferreira - Estoquista", "STOCK")
            )

            sampleUsers.forEach { user ->
                userStmt.setString(1, user[0] as String)
                userStmt.setString(2, user[1] as String)
                userStmt.setString(3, user[2] as String)
                userStmt.setString(4, user[3] as String)
                userStmt.executeUpdate()
            }

            println("✓ ${sampleUsers.size} usuários de exemplo inseridos")
            println("  → admin/admin123 (Administrador)")
            println("  → gerente/gerente123 (Gerente)")
            println("  → caixa1/caixa123 (Caixa)")
            println("  → estoque/estoque123 (Estoquista)")
        }

        // Verifica se já existem clientes
        val clientCountStmt = conn.createStatement()
        val clientRs = clientCountStmt.executeQuery("SELECT COUNT(*) as count FROM client")
        clientRs.next()
        val clientCount = clientRs.getInt("count")

        if (clientCount == 0) {
            println("✓ Inserindo clientes de exemplo...")

            val clientStmt = conn.prepareStatement("""
                INSERT INTO client (name, document, phone, email, address, default_discount_percent) 
                VALUES (?, ?, ?, ?, ?, ?)
            """)

            val sampleClients = listOf(
                listOf("Cliente A", "12345678901", "1111-2222", "clientea@email.com", "Endereço A", 10.0),
                listOf("Cliente B", "10987654321", "3333-4444", "clienteb@email.com", "Endereço B", 15.0),
                listOf("Cliente C", "12312312312", "5555-6666", "clientec@email.com", "Endereço C", 5.0),
                listOf("Cliente D", "32132132132", "7777-8888", "cliented@email.com", "Endereço D", 20.0),
                listOf("Cliente E", "45645645645", "9999-0000", "clientee@email.com", "Endereço E", 0.0)
            )

            sampleClients.forEach { client ->
                clientStmt.setString(1, client[0] as String)
                clientStmt.setString(2, client[1] as String)
                clientStmt.setString(3, client[2] as String)
                clientStmt.setString(4, client[3] as String)
                clientStmt.setString(5, client[4] as String)
                clientStmt.setDouble(6, client[5] as Double)
                clientStmt.executeUpdate()
            }

            println("✓ ${sampleClients.size} clientes de exemplo inseridos")
        }

        println("✓ Dados de amostra verificados")
    }
}
