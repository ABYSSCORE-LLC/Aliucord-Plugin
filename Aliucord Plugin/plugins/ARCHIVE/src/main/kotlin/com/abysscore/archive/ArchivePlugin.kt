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
    private lateinit var store: BackupStore

    override fun start(context: Context) {
        store = BackupStore(File(context.filesDir, "archive/backups"))
        registerBackupCommand()
    }

    private fun registerBackupCommand() {
        val createOptions = listOf(
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "name",
                "A unique name for this backup",
                required = true,
            ),
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "scope",
                "Label this snapshot: server, channel, or both",
                required = true,
            ),
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "json",
                "JSON object containing the snapshot data",
                required = true,
            ),
        )

        val subcommands = listOf(
            Utils.createCommandOption(
                ApplicationCommandType.SUB_COMMAND,
                "create",
                "Save a JSON snapshot to this device",
                subCommandOptions = createOptions,
            ),
            Utils.createCommandOption(
                ApplicationCommandType.SUB_COMMAND,
                "list",
                "List backups stored on this device",
            ),
            Utils.createCommandOption(
                ApplicationCommandType.SUB_COMMAND,
                "info",
                "Show metadata for a saved backup",
                subCommandOptions = listOf(nameOption()),
            ),
            Utils.createCommandOption(
                ApplicationCommandType.SUB_COMMAND,
                "load",
                "Load and preview a saved JSON snapshot",
                subCommandOptions = listOf(nameOption()),
            ),
            Utils.createCommandOption(
                ApplicationCommandType.SUB_COMMAND,
                "delete",
                "Delete a saved backup from this device",
                subCommandOptions = listOf(nameOption()),
            ),
        )

        commands.registerCommand(
            "back-up",
            "Create and manage local ARCHIVE snapshots",
            subcommands,
        ) { ctx ->
            try {
                when {
                    ctx.getSubCommandArgs("create") != null -> {
                        val args = ctx.getRequiredSubCommandArgs("create")
                        val name = (args["name"] as? String).orEmpty().trim()
                        val scope = (args["scope"] as? String).orEmpty().trim().lowercase(Locale.ROOT)
                        val jsonText = (args["json"] as? String).orEmpty().trim()

                        require(name.isNotEmpty()) { "Enter a backup name." }
                        require(scope in VALID_SCOPES) {
                            "Scope must be one of: server, channel, or both."
                        }
                        require(jsonText.isNotEmpty()) { "Provide a JSON object to save." }
                        require(jsonText.length <= MAX_JSON_CHARS) {
                            "The JSON input is too large. Limit it to $MAX_JSON_CHARS characters."
                        }

                        val payload = JSONObject(jsonText)
                        val record = store.create(name, scope, payload)
                        privateResult(
                            "Created local backup **${record.name}** (ID: `${record.id}`).\n" +
                                "Scope label: **${record.scope}**. Stored on this device only.\n" +
                                "This command saves the JSON you supplied; it does not crawl Discord or change a server."
                        )
                    }

                    ctx.getSubCommandArgs("list") != null -> {
                        val records = store.list()
                        val message = if (records.isEmpty()) {
                            "No ARCHIVE backups have been saved on this device yet."
                        } else {
                            records.take(MAX_LIST_ITEMS).joinToString("\n") { record ->
                                "**${record.name}** [${record.scope}] — ID `${record.id}` — ${record.createdAt}"
                            } + if (records.size > MAX_LIST_ITEMS) {
                                "\nShowing the newest $MAX_LIST_ITEMS of ${records.size} backups."
                            } else {
                                ""
                            }
                        }
                        privateResult(message)
                    }

                    ctx.getSubCommandArgs("info") != null -> {
                        val key = requiredName(ctx.getRequiredSubCommandArgs("info"))
                        val record = store.find(key)
                            ?: error("No backup named or identified as '$key' was found.")
                        privateResult(
                            "**${record.name}**\n" +
                                "ID: `${record.id}`\nScope: **${record.scope}**\n" +
                                "Created: ${record.createdAt}\n" +
                                "Storage: this device's private Aliucord files directory."
                        )
                    }

                    ctx.getSubCommandArgs("load") != null -> {
                        val key = requiredName(ctx.getRequiredSubCommandArgs("load"))
                        val record = store.find(key)
                            ?: error("No backup named or identified as '$key' was found.")
                        val payload = store.read(record)
                        val formatted = payload.toString(2)
                        val preview = formatted.take(MAX_PREVIEW_CHARS)
                        val suffix = if (formatted.length > MAX_PREVIEW_CHARS) {
                            "\n\nPreview shortened. The complete snapshot remains in local storage."
                        } else {
                            ""
                        }
                        privateResult(
                            "Loaded **${record.name}** [${record.scope}] from local storage.\n" +
                                "```json\n$preview\n```$suffix\n" +
                                "This is a data preview, not a live Discord server restore."
                        )
                    }

                    ctx.getSubCommandArgs("delete") != null -> {
                        val key = requiredName(ctx.getRequiredSubCommandArgs("delete"))
                        val record = store.find(key)
                            ?: error("No backup named or identified as '$key' was found.")
                        store.delete(record)
                        privateResult("Deleted local backup **${record.name}** (`${record.id}`).")
                    }

                    else -> privateResult(
                        "Choose a subcommand: `create`, `list`, `info`, `load`, or `delete`."
                    )
                }
            } catch (error: Exception) {
                privateResult("ARCHIVE: ${error.message ?: "The operation failed."}")
            }
        }
    }

    private fun nameOption() = Utils.createCommandOption(
        ApplicationCommandType.STRING,
        "name",
        "Backup name or ID",
        required = true,
    )

    private fun requiredName(args: Map<String, Any>): String {
        val value = (args["name"] as? String).orEmpty().trim()
        require(value.isNotEmpty()) { "Enter a backup name or ID." }
        return value
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
        const val MAX_PREVIEW_CHARS = 1_350
        const val MAX_LIST_ITEMS = 12
        val VALID_SCOPES = setOf("server", "channel", "both")
    }
}

private data class BackupRecord(
    val id: String,
    val name: String,
    val scope: String,
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
    fun create(name: String, scope: String, payload: JSONObject): BackupRecord {
        val normalizedName = name.trim()
        require(normalizedName.length in 1..48) {
            "Backup names must contain between 1 and 48 characters."
        }
        require(normalizedName.matches(Regex("[A-Za-z0-9][A-Za-z0-9 _.-]{0,47}"))) {
            "Use only letters, numbers, spaces, periods, underscores, and hyphens in backup names."
        }
        require(scope in setOf("server", "channel", "both")) {
            "Invalid backup scope."
        }
        require(find(normalizedName) == null) {
            "A backup with that name already exists. Choose another name."
        }

        val id = UUID.randomUUID().toString().replace("-", "").take(10)
        val createdAt = utcTimestamp()
        val fileName = "$id.json"
        val envelope = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("id", id)
            .put("name", normalizedName)
            .put("scope", scope)
            .put("createdAt", createdAt)
            .put("payload", payload)

        val target = safeFile(fileName)
        val temporary = safeFile("$fileName.tmp")
        try {
            temporary.writeText(envelope.toString(2), Charsets.UTF_8)
            check(temporary.renameTo(target)) { "Could not finalize the backup file." }
        } finally {
            if (temporary.exists()) temporary.delete()
        }

        return BackupRecord(id, normalizedName, scope, createdAt, fileName)
    }

    @Synchronized
    fun list(): List<BackupRecord> = directory.listFiles { file ->
        file.isFile && file.extension == "json"
    }.orEmpty().mapNotNull { file ->
        runCatching {
            val envelope = JSONObject(file.readText(Charsets.UTF_8))
            val id = envelope.getString("id")
            val name = envelope.getString("name")
            val scope = envelope.optString("scope", "both")
            val createdAt = envelope.getString("createdAt")
            if (!id.matches(Regex("[A-Fa-f0-9]{10}"))) return@runCatching null
            if (scope !in setOf("server", "channel", "both")) return@runCatching null
            BackupRecord(id, name, scope, createdAt, file.name)
        }.getOrNull()
    }.sortedByDescending { it.createdAt }

    @Synchronized
    fun find(key: String): BackupRecord? = list().firstOrNull {
        it.id.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true)
    }

    @Synchronized
    fun read(record: BackupRecord): JSONObject {
        val file = safeFile(record.fileName)
        check(file.isFile) { "The selected backup file no longer exists." }
        val envelope = JSONObject(file.readText(Charsets.UTF_8))
        require(envelope.optInt("schemaVersion") == SCHEMA_VERSION) {
            "This backup uses an unsupported format version."
        }
        require(envelope.getString("id") == record.id) {
            "The backup metadata does not match its file."
        }
        require(envelope.getString("name") == record.name) {
            "The backup name does not match its file metadata."
        }
        return envelope.getJSONObject("payload")
    }

    @Synchronized
    fun delete(record: BackupRecord) {
        val file = safeFile(record.fileName)
        check(file.isFile && file.delete()) { "Could not delete the selected backup file." }
    }

    private fun safeFile(fileName: String): File {
        require(fileName.matches(Regex("[A-Fa-f0-9]{10}\\.json(?:\\.tmp)?"))) {
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

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}
