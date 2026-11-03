package com.abysscore.archive

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.discord.api.commands.ApplicationCommandType
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@AliucordPlugin(requiresRestart = false)
@Suppress("unused")
class ArchivePlugin : Plugin() {
    private lateinit var backupStore: BackupStore

    override fun start(context: Context) {
        backupStore = BackupStore(File(context.filesDir, "archive/backups"))
        registerBackupCommand()
    }

    private fun registerBackupCommand() {
        val createOptions = listOf(
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "name",
                "A short name for this backup",
                required = true,
            ),
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "json",
                "JSON object to store as the backup snapshot",
                required = true,
            ),
        )

        val loadOptions = listOf(
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "name",
                "Backup name or backup ID",
                required = true,
            ),
        )

        val subcommands = listOf(
            Utils.createCommandOption(
                ApplicationCommandType.SUB_COMMAND,
                "create",
                "Create a local JSON backup",
                subCommandOptions = createOptions,
            ),
            Utils.createCommandOption(
                ApplicationCommandType.SUB_COMMAND,
                "list",
                "List locally saved backups",
            ),
            Utils.createCommandOption(
                ApplicationCommandType.SUB_COMMAND,
                "load",
                "Load and preview a local backup",
                subCommandOptions = loadOptions,
            ),
            Utils.createCommandOption(
                ApplicationCommandType.SUB_COMMAND,
                "delete",
                "Delete a local backup",
                subCommandOptions = loadOptions,
            ),
        )

        commands.registerCommand(
            "back-up",
            "Create and manage local ARCHIVE backups",
            subcommands,
        ) { ctx ->
            try {
                when {
                    ctx.getSubCommandArgs("create") != null -> {
                        val args = ctx.getRequiredSubCommandArgs("create")
                        val name = (args["name"] as? String).orEmpty().trim()
                        val jsonText = (args["json"] as? String).orEmpty().trim()
                        require(name.isNotEmpty()) { "Enter a backup name." }
                        require(jsonText.isNotEmpty()) { "Provide a JSON object to save." }
                        require(jsonText.length <= MAX_JSON_CHARS) {
                            "The JSON input is too large. Limit it to $MAX_JSON_CHARS characters."
                        }
                        val payload = JSONObject(jsonText)
                        val record = backupStore.create(name, payload)
                        privateResult(
                            "Created local backup **${record.name}** (ID: `${record.id}`).\n" +
                                "Saved on this device only. ARCHIVE did not scan or modify a Discord server."
                        )
                    }

                    ctx.getSubCommandArgs("list") != null -> {
                        val records = backupStore.list()
                        val message = if (records.isEmpty()) {
                            "No ARCHIVE backups have been saved on this device yet."
                        } else {
                            records.joinToString("\n") { record ->
                                "**${record.name}** — ID `${record.id}` — ${record.createdAt}"
                            }
                        }
                        privateResult(message)
                    }

                    ctx.getSubCommandArgs("load") != null -> {
                        val args = ctx.getRequiredSubCommandArgs("load")
                        val key = (args["name"] as? String).orEmpty().trim()
                        require(key.isNotEmpty()) { "Enter a backup name or ID." }
                        val record = backupStore.find(key)
                            ?: error("No backup named or identified as '$key' was found.")
                        val payload = backupStore.read(record)
                        val preview = payload.toString(2).take(MAX_PREVIEW_CHARS)
                        val suffix = if (payload.toString().length > MAX_PREVIEW_CHARS) {
                            "\n\nPreview shortened. The complete JSON remains in local storage."
                        } else {
                            ""
                        }
                        privateResult(
                            "Loaded **${record.name}** from local storage.\n" +
                                "```json\n$preview\n```$suffix\n" +
                                "This is a local preview, not a Discord server restore."
                        )
                    }

                    ctx.getSubCommandArgs("delete") != null -> {
                        val args = ctx.getRequiredSubCommandArgs("delete")
                        val key = (args["name"] as? String).orEmpty().trim()
                        require(key.isNotEmpty()) { "Enter a backup name or ID." }
                        val record = backupStore.find(key)
                            ?: error("No backup named or identified as '$key' was found.")
                        backupStore.delete(record)
                        privateResult("Deleted local backup **${record.name}** (`${record.id}`).")
                    }

                    else -> privateResult(
                        "Choose a subcommand: `create`, `list`, `load`, or `delete`."
                    )
                }
            } catch (error: Exception) {
                privateResult("ARCHIVE: ${error.message ?: "The operation failed."}")
            }
        }
    }

    private fun privateResult(message: String) = CommandsAPI.CommandResult(
        message,
        null,
        false,
    )

    override fun stop(context: Context) {
        commands.unregisterCommand("back-up")
        patcher.unpatchAll()
    }

    private companion object {
        const val MAX_JSON_CHARS = 5_500
        const val MAX_PREVIEW_CHARS = 1_700
    }
}

private data class BackupRecord(
    val id: String,
    val name: String,
    val createdAt: String,
    val fileName: String,
)

private class BackupStore(private val directory: File) {
    init {
        check(directory.isDirectory || directory.mkdirs()) {
            "Cannot create the local ARCHIVE backup directory."
        }
    }

    @Synchronized
    fun create(name: String, payload: JSONObject): BackupRecord {
        val normalizedName = name.trim()
        require(normalizedName.length in 1..48) {
            "Backup names must contain between 1 and 48 characters."
        }
        require(normalizedName.none { it.isISOControl() }) {
            "Backup names cannot contain control characters."
        }
        require(find(normalizedName) == null) {
            "A backup with that name already exists. Choose another name."
        }

        val id = UUID.randomUUID().toString().replace("-", "").take(10)
        val createdAt = utcTimestamp()
        val fileName = "$id.json"
        val envelope = JSONObject()
            .put("schemaVersion", 1)
            .put("id", id)
            .put("name", normalizedName)
            .put("createdAt", createdAt)
            .put("payload", payload)

        val target = File(directory, fileName)
        val temporary = File(directory, "$fileName.tmp")
        try {
            temporary.writeText(envelope.toString(2), Charsets.UTF_8)
            check(temporary.renameTo(target)) { "Could not finalize the backup file." }
        } finally {
            if (temporary.exists()) temporary.delete()
        }

        return BackupRecord(id, normalizedName, createdAt, fileName)
    }

    @Synchronized
    fun list(): List<BackupRecord> =
        directory.listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .mapNotNull { file ->
                runCatching {
                    val envelope = JSONObject(file.readText(Charsets.UTF_8))
                    BackupRecord(
                        id = envelope.getString("id"),
                        name = envelope.getString("name"),
                        createdAt = envelope.getString("createdAt"),
                        fileName = file.name,
                    )
                }.getOrNull()
            }
            .sortedByDescending { it.createdAt }

    @Synchronized
    fun find(key: String): BackupRecord? = list().firstOrNull {
        it.id.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true)
    }

    @Synchronized
    fun read(record: BackupRecord): JSONObject {
        val file = safeFile(record.fileName)
        check(file.isFile) { "The selected backup file no longer exists." }
        val envelope = JSONObject(file.readText(Charsets.UTF_8))
        require(envelope.optInt("schemaVersion") == 1) {
            "This backup uses an unsupported format version."
        }
        require(envelope.getString("id") == record.id) {
            "The backup metadata does not match its file."
        }
        return envelope.getJSONObject("payload")
    }

    @Synchronized
    fun delete(record: BackupRecord) {
        val file = safeFile(record.fileName)
        check(file.isFile && file.delete()) { "Could not delete the selected backup file." }
    }

    private fun safeFile(fileName: String): File {
        require(fileName.matches(Regex("[A-Fa-f0-9]{10}\\.json"))) {
            "Invalid backup file name."
        }
        val root = directory.canonicalFile
        val file = File(directory, fileName).canonicalFile
        require(file.parentFile == root) { "Backup path escaped the local storage directory." }
        return file
    }

    private fun utcTimestamp(): String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        Locale.US,
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())
}
