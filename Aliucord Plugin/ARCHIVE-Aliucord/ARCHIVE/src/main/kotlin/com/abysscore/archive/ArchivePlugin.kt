package com.abysscore.archive

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.discord.api.commands.ApplicationCommandType
import org.json.JSONObject
import java.io.File
import java.util.UUID

@AliucordPlugin(requiresRestart = false)
@Suppress("unused")
class ArchivePlugin : Plugin() {
    private lateinit var store: BackupStore

    override fun start(context: Context) {
        store = BackupStore(File(context.filesDir, "archive/backups"))
        registerArchiveCommand()
    }

    private fun registerArchiveCommand() {
        val options = listOf(
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "action",
                "Operation: create, list, load, or delete",
                required = true,
                choices = listOf(
                    Utils.createCommandChoice("create", "create"),
                    Utils.createCommandChoice("list", "list"),
                    Utils.createCommandChoice("load", "load"),
                    Utils.createCommandChoice("delete", "delete")
                )
            ),
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "name",
                "Backup name (required for create/load/delete)",
                required = false
            ),
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "json",
                "JSON snapshot data for create (up to Discord's option limit)",
                required = false
            )
        )

        commands.registerCommand(
            "archive",
            "Manage local ARCHIVE JSON snapshots",
            options
        ) { ctx ->
            try {
                when (ctx.getStringOrDefault("action", "").lowercase()) {
                    "create" -> {
                        val name = ctx.getStringOrDefault("name", "").trim()
                        val raw = ctx.getStringOrDefault("json", "").trim()
                        require(name.isNotBlank()) { "Provide a backup name." }
                        require(raw.isNotBlank()) { "Provide JSON snapshot data in the json option." }
                        val parsed = JSONObject(raw)
                        val record = store.create(name, parsed)
                        CommandsAPI.CommandResult(
                            "Created local backup **${record.name}** (`${record.id}`).\n" +
                                "This stores the JSON supplied to the command; it does not scrape a Discord server.",
                            null,
                            false
                        )
                    }
                    "list" -> {
                        val records = store.list()
                        val body = if (records.isEmpty()) {
                            "No local backups found."
                        } else {
                            records.joinToString("\n") {
                                "• **${it.name}** — `${it.id}` — ${it.createdAt}"
                            }
                        }
                        CommandsAPI.CommandResult(body, null, true)
                    }
                    "load" -> {
                        val idOrName = ctx.getStringOrDefault("name", "").trim()
                        require(idOrName.isNotBlank()) { "Provide a backup name or ID." }
                        val record = store.find(idOrName)
                            ?: error("Backup not found: $idOrName")
                        val json = store.read(record)
                        CommandsAPI.CommandResult(
                            "Backup **${record.name}** loaded from local storage.\n" +
                                "Snapshot JSON:\n```json\n${json.toString(2).take(1700)}\n```\n" +
                                "This MVP displays the saved snapshot; it does not apply changes to Discord.",
                            null,
                            false
                        )
                    }
                    "delete" -> {
                        val idOrName = ctx.getStringOrDefault("name", "").trim()
                        require(idOrName.isNotBlank()) { "Provide a backup name or ID." }
                        val removed = store.delete(idOrName)
                        require(removed) { "Backup not found: $idOrName" }
                        CommandsAPI.CommandResult("Deleted local backup **$idOrName**.", null, true)
                    }
                    else -> CommandsAPI.CommandResult(
                        "Choose an action: `create`, `list`, `load`, or `delete`.",
                        null,
                        false
                    )
                }
            } catch (t: Throwable) {
                CommandsAPI.CommandResult(
                    "ARCHIVE error: ${t.message ?: t.javaClass.simpleName}",
                    null,
                    true
                )
            }
        }
    }

    override fun stop(context: Context) {
        commands.unregisterCommand("archive")
        patcher.unpatchAll()
    }
}

private data class BackupRecord(
    val id: String,
    val name: String,
    val createdAt: String,
    val fileName: String
)

private class BackupStore(private val directory: File) {
    init {
        check(directory.exists() || directory.mkdirs()) {
            "Cannot create local backup directory."
        }
    }

    @Synchronized
    fun create(name: String, payload: JSONObject): BackupRecord {
        val safeName = name.trim().take(64)
        require(safeName.isNotBlank()) { "Backup name cannot be empty." }
        val id = UUID.randomUUID().toString().take(8)
        val now = java.time.Instant.now().toString()
        val fileName = "$id.json"
        val envelope = JSONObject()
            .put("schemaVersion", 1)
            .put("id", id)
            .put("name", safeName)
            .put("createdAt", now)
            .put("payload", payload)
        val target = File(directory, fileName)
        val temp = File(directory, "$fileName.tmp")
        temp.writeText(envelope.toString(2), Charsets.UTF_8)
        check(temp.renameTo(target)) { "Atomic backup write failed." }
        return BackupRecord(id, safeName, now, fileName)
    }

    @Synchronized
    fun list(): List<BackupRecord> =
        directory.listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .mapNotNull { file ->
                runCatching {
                    val obj = JSONObject(file.readText(Charsets.UTF_8))
                    BackupRecord(
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("createdAt"),
                        file.name
                    )
                }.getOrNull()
            }
            .sortedByDescending { it.createdAt }

    @Synchronized
    fun find(idOrName: String): BackupRecord? =
        list().firstOrNull {
            it.id.equals(idOrName, true) || it.name.equals(idOrName, true)
        }

    @Synchronized
    fun read(record: BackupRecord): JSONObject {
        val file = File(directory, record.fileName)
        require(file.canonicalFile.parentFile == directory.canonicalFile) {
            "Invalid backup path."
        }
        val envelope = JSONObject(file.readText(Charsets.UTF_8))
        require(envelope.optInt("schemaVersion") == 1) { "Unsupported backup schema." }
        return envelope.getJSONObject("payload")
    }

    @Synchronized
    fun delete(idOrName: String): Boolean {
        val record = find(idOrName) ?: return false
        val file = File(directory, record.fileName)
        require(file.canonicalFile.parentFile == directory.canonicalFile) {
            "Invalid backup path."
        }
        return file.delete()
    }
}
