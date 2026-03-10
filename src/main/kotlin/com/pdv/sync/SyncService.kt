package com.pdv.sync

import com.pdv.data.OutboxDao
import com.pdv.data.Config
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object SyncService {
    private val outbox = OutboxDao()
    @Volatile
    private var running = false

    // Max attempts before marking FAILED
    private const val MAX_ATTEMPTS = 5

    fun start(intervalMs: Long = 30_000L) {
        if (running) return
        running = true
        thread(start = true, isDaemon = true) {
            var backoffBase = 1000L
            while (running) {
                try {
                    val pending = outbox.listPending(50)
                    if (pending.isNotEmpty() && !Config.remoteUrl.isBlank()) {
                        pending.forEach { item ->
                            try {
                                val id = (item["id"] as? Long) ?: 0L
                                val payload = item["payload"] as? String ?: ""
                                val attempts = (item["attempts"] as? Int) ?: 0

                                // If attempts exceeded max, mark failed and skip
                                if (attempts >= MAX_ATTEMPTS) {
                                    outbox.markFailed(id, "max attempts reached")
                                    println("✗ Outbox id=$id marcado FAILED (max attempts)")
                                    return@forEach
                                }

                                val success = postToRemote(payload)
                                if (success) {
                                    outbox.markSynced(id)
                                    println("✓ Outbox id=$id enviado com sucesso")
                                } else {
                                    val errMsg = "HTTP error or connection failure"
                                    outbox.incrementAttempt(id, errMsg)
                                    println("✗ Falha ao enviar outbox id=$id, tentativa ${attempts + 1}")
                                    // small sleep proportional to attempts
                                    Thread.sleep(backoffBase * (attempts + 1))
                                }
                            } catch (e: Exception) {
                                println("✗ Erro ao processar outbox item: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("✗ SyncService erro: ${e.message}")
                }
                Thread.sleep(intervalMs)
            }
        }
    }

    fun stop() { running = false }

    private fun postToRemote(jsonPayload: String): Boolean {
        try {
            val url = URL(Config.remoteUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            val token = Config.remoteToken
            if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token")
            conn.outputStream.use { os -> os.write(jsonPayload.toByteArray()) }
            val code = conn.responseCode
            // consume stream to allow connection reuse
            try { conn.inputStream.close() } catch (_: Exception) {}
            return code in 200..299
        } catch (e: Exception) {
            println("✗ Falha ao postar para remoto: ${e.message}")
            return false
        }
    }
}
