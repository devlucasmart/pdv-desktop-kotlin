package com.pdv.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

object StoreConfig {
    private const val DEFAULT_NAME = "NOME FANTASIA LTDA"
    private const val DEFAULT_ADDRESS = "RUA EXEMPLO, 123 - BAIRRO - CIDADE - UF"
    private const val DEFAULT_DOCUMENT = "00.000.000/0000-00"

    private val props = Properties()
    private val configFile: File by lazy {
        val dir = File(System.getProperty("user.home"), ".pdv")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "pdv.properties")
    }

    init {
        load()
    }

    private fun load() {
        try {
            if (configFile.exists()) {
                FileInputStream(configFile).use { fis -> props.load(fis) }
            } else {
                // try loading default from resources if present
                val r = javaClass.getResourceAsStream("/pdv.properties")
                if (r != null) {
                    r.use { props.load(it) }
                }
            }
        } catch (e: Exception) {
            println("⚠️ Não foi possível carregar StoreConfig: ${e.message}")
        }
    }

    fun save(name: String?, address: String?, document: String?) {
        if (name != null) props.setProperty("store.name", name)
        if (address != null) props.setProperty("store.address", address)
        if (document != null) props.setProperty("store.document", document)
        try {
            FileOutputStream(configFile).use { fos -> props.store(fos, "PDV Store Configuration") }
        } catch (e: Exception) {
            println("⚠️ Falha ao salvar StoreConfig: ${e.message}")
        }
    }

    fun getName(): String = props.getProperty("store.name") ?: DEFAULT_NAME
    fun getAddress(): String = props.getProperty("store.address") ?: DEFAULT_ADDRESS
    fun getDocument(): String = props.getProperty("store.document") ?: DEFAULT_DOCUMENT
}

