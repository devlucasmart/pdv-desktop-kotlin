package com.pdv.data

import java.io.File
import java.util.Properties

object Config {
    private val propsFile = File(System.getProperty("user.home"), ".pdv/pdv.properties")
    private val props = Properties()

    init {
        try {
            if (!propsFile.parentFile.exists()) propsFile.parentFile.mkdirs()
            if (propsFile.exists()) {
                propsFile.inputStream().use { props.load(it) }
            } else {
                // Defaults
                props.setProperty("server.enabled", "false")
                props.setProperty("server.port", "8080")
                props.setProperty("server.bind", "127.0.0.1")
                props.setProperty("server.token", "")
                props.setProperty("remote.url", "")
                props.setProperty("remote.token", "")
                // Client connection defaults
                props.setProperty("client.useRemote", "false")
                props.setProperty("client.host", "localhost")
                props.setProperty("client.port", "8080")
                // TLS/SSL defaults
                props.setProperty("server.ssl.enabled", "false")
                props.setProperty("server.ssl.keystore.path", "")
                props.setProperty("server.ssl.keystore.password", "")
                save()
            }
        } catch (e: Exception) {
            println("✗ Erro ao carregar configurações: ${e.message}")
        }
    }

    fun get(key: String, default: String = ""): String {
        return props.getProperty(key, default)
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return props.getProperty(key)?.toBoolean() ?: default
    }

    fun set(key: String, value: String) {
        props.setProperty(key, value)
        save()
    }

    fun setBoolean(key: String, value: Boolean) {
        props.setProperty(key, value.toString())
        save()
    }

    private fun save() {
        try {
            propsFile.outputStream().use { props.store(it, "PDV Config") }
        } catch (e: Exception) {
            println("✗ Erro ao salvar configurações: ${e.message}")
        }
    }

    val serverEnabled: Boolean get() = getBoolean("server.enabled", false)
    val serverPort: Int get() = get("server.port", "8080").toIntOrNull() ?: 8080
    val serverBind: String get() = get("server.bind", "127.0.0.1")
    val serverToken: String get() = get("server.token", "")
    val remoteUrl: String get() = get("remote.url", "")
    val remoteToken: String get() = get("remote.token", "")

    // Client connection settings (for login/connect)
    var clientUseRemote: Boolean
        get() = getBoolean("client.useRemote", false)
        set(value) = setBoolean("client.useRemote", value)

    var clientHost: String
        get() = get("client.host", "localhost")
        set(value) = set("client.host", value)

    var clientPort: Int
        get() = get("client.port", "8080").toIntOrNull() ?: 8080
        set(value) = set("client.port", value.toString())

    // TLS / HTTPS settings for embedded server
    var serverSslEnabled: Boolean
        get() = getBoolean("server.ssl.enabled", false)
        set(value) = setBoolean("server.ssl.enabled", value)

    var serverKeystorePath: String
        get() = get("server.ssl.keystore.path", "")
        set(value) = set("server.ssl.keystore.path", value)

    var serverKeystorePassword: String
        get() = get("server.ssl.keystore.password", "")
        set(value) = set("server.ssl.keystore.password", value)
}
