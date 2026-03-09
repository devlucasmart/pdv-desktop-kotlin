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

    private fun createTables() {
        val conn = connection ?: return

        // Tabela de produtos
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS product (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sku TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                price REAL NOT NULL CHECK(price >= 0),
                stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK(stock_quantity >= 0),
                category TEXT,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL DEFAULT (datetime('now')),
                updated_at TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """)

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

        // Tabela de itens de venda
        conn.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS sale_item (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL CHECK(quantity > 0),
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

        println("✓ Tabelas criadas com sucesso")
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
                INSERT INTO product (sku, name, price, stock_quantity, category) 
                VALUES (?, ?, ?, ?, ?)
            """)

            val sampleProducts = listOf(
                listOf("001", "Coca-Cola 2L", 8.50, 50, "Bebidas"),
                listOf("002", "Pão Francês (kg)", 12.00, 100, "Padaria"),
                listOf("003", "Arroz Tipo 1 5kg", 25.90, 30, "Alimentos"),
                listOf("004", "Feijão Preto 1kg", 7.80, 40, "Alimentos"),
                listOf("005", "Café Torrado 500g", 15.50, 25, "Bebidas"),
                listOf("006", "Açúcar Cristal 1kg", 4.20, 60, "Alimentos"),
                listOf("007", "Leite Integral 1L", 5.80, 80, "Laticínios"),
                listOf("008", "Manteiga 500g", 18.90, 20, "Laticínios"),
                listOf("009", "Óleo de Soja 900ml", 7.50, 45, "Alimentos"),
                listOf("010", "Macarrão Espaguete 500g", 4.50, 70, "Alimentos")
            )

            sampleProducts.forEach { product ->
                stmt.setString(1, product[0] as String)
                stmt.setString(2, product[1] as String)
                stmt.setDouble(3, product[2] as Double)
                stmt.setInt(4, product[3] as Int)
                stmt.setString(5, product[4] as String)
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
    }

    fun close() {
        try {
            connection?.close()
            println("✓ Conexão com banco de dados fechada")
        } catch (e: SQLException) {
            println("✗ Erro ao fechar conexão: ${e.message}")
        }
    }
}
