package com.pdv.util

import com.pdv.data.Config
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupService {
    private var scheduler: ScheduledExecutorService? = null
    private val dbFile = File("pdv.db")
    private val dataDbFile = File("pdv_data.db")

    fun start() {
        if (!Config.backupEnabled) {
            println("⚠ Backup automático desativado")
            return
        }

        stop() // Parar qualquer scheduler existente

        val intervalHours = Config.backupIntervalHours.toLong()
        scheduler = Executors.newSingleThreadScheduledExecutor()

        scheduler?.scheduleAtFixedRate({
            try {
                performBackup()
            } catch (e: Exception) {
                println("✗ Erro no backup automático: ${e.message}")
            }
        }, 0, intervalHours, TimeUnit.HOURS)

        println("✓ Backup automático iniciado (intervalo: ${intervalHours}h)")
    }

    fun stop() {
        scheduler?.shutdown()
        scheduler = null
        println("⚠ Backup automático parado")
    }

    fun performBackup(): BackupResult {
        val backupDir = File(Config.backupPath)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val backupFileName = "pdv_backup_$timestamp.zip"
        val backupFile = File(backupDir, backupFileName)

        return try {
            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                // Backup do banco principal
                if (dbFile.exists()) {
                    addToZip(zos, dbFile, "pdv.db")
                }

                // Backup do banco de dados secundário
                if (dataDbFile.exists()) {
                    addToZip(zos, dataDbFile, "pdv_data.db")
                }

                // Backup das configurações
                val configFile = File(System.getProperty("user.home"), ".pdv/pdv.properties")
                if (configFile.exists()) {
                    addToZip(zos, configFile, "pdv.properties")
                }
            }

            // Limpar backups antigos (manter últimos 10)
            cleanOldBackups(backupDir, 10)

            println("✓ Backup realizado: ${backupFile.absolutePath}")
            BackupResult(true, backupFile.absolutePath, null)
        } catch (e: Exception) {
            println("✗ Erro ao realizar backup: ${e.message}")
            BackupResult(false, null, e.message)
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { fis ->
            zos.putNextEntry(ZipEntry(entryName))
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }

    private fun cleanOldBackups(backupDir: File, keepCount: Int) {
        val backups = backupDir.listFiles { f -> f.name.startsWith("pdv_backup_") && f.name.endsWith(".zip") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (backups.size > keepCount) {
            backups.drop(keepCount).forEach { old ->
                old.delete()
                println("⚠ Backup antigo removido: ${old.name}")
            }
        }
    }

    fun listBackups(): List<BackupInfo> {
        val backupDir = File(Config.backupPath)
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles { f -> f.name.startsWith("pdv_backup_") && f.name.endsWith(".zip") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { BackupInfo(it.name, it.absolutePath, it.length(), it.lastModified()) }
            ?: emptyList()
    }

    fun restoreBackup(backupPath: String): BackupResult {
        val backupFile = File(backupPath)
        if (!backupFile.exists()) {
            return BackupResult(false, null, "Arquivo de backup não encontrado")
        }

        return try {
            java.util.zip.ZipFile(backupFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    when (entry.name) {
                        "pdv.db" -> {
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(dbFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        "pdv_data.db" -> {
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(dataDbFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        "pdv.properties" -> {
                            val configFile = File(System.getProperty("user.home"), ".pdv/pdv.properties")
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(configFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }
            println("✓ Backup restaurado: $backupPath")
            BackupResult(true, backupPath, null)
        } catch (e: Exception) {
            println("✗ Erro ao restaurar backup: ${e.message}")
            BackupResult(false, null, e.message)
        }
    }
}

data class BackupResult(
    val success: Boolean,
    val path: String?,
    val error: String?
)

data class BackupInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val timestamp: Long
) {
    val sizeFormatted: String
        get() {
            val kb = sizeBytes / 1024.0
            return if (kb > 1024) {
                String.format("%.2f MB", kb / 1024)
            } else {
                String.format("%.2f KB", kb)
            }
        }
}

