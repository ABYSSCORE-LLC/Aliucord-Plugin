# ARCHIVE for Aliucord — 0.3.0

**Creator credit: ABYSSCORE-LLC** — https://github.com/ABYSSCORE-LLC

ARCHIVE is a local-first JSON snapshot manager exposed through Aliucord's slash-command API. It stores backup files inside Aliucord's private application files directory. No Discord token is collected and the plugin does not send backup data to a remote server.

## Important: source ZIP versus installable plugin ZIP

This download is the **complete Gradle source project**. It is not a compiled Aliucord plugin binary and must not be copied directly into `Aliucord/plugins`.

To create the installable plugin ZIP, build the `:plugins:ARCHIVE:make` Gradle task. The official Aliucord Gradle plugin prints the exact output path when the task succeeds. Copy that generated plugin ZIP (not this source ZIP) into the device's `Aliucord/plugins` folder, then restart Aliucord.

The root `manifest.json` identifies this source project for archive/import tools. It does not replace the official Aliucord Gradle build or turn the source archive into an installable binary.

## Requirements

- JDK 21 or newer
- Android SDK platform 36 and build tools installed
- Network access for the first Gradle build, so Gradle and Aliucord dependencies can be downloaded
- Android Studio (recommended) or a terminal

## Build on Windows

Open PowerShell or Command Prompt in this directory:

```bat
.\gradlew.bat :plugins:ARCHIVE:make
```

To build and install directly to an authorized connected Android device:

```bat
.\gradlew.bat :plugins:ARCHIVE:deployWithAdb
```

## Build on macOS or Linux

```bash
chmod +x ./gradlew
./gradlew :plugins:ARCHIVE:make
```

For direct ADB deployment:

```bash
./gradlew :plugins:ARCHIVE:deployWithAdb
```

## Commands

The plugin registers the `/back-up` command. Its operations are:

| Command | Behavior |
|---|---|
| `/back-up create` | Saves a supplied JSON object as a named local snapshot. Requires `name`, `scope` (`server`, `channel`, or `both`), and `json`. |
| `/back-up list` | Lists the newest local backups. |
| `/back-up info` | Shows the saved snapshot's metadata. |
| `/back-up load` | Reads and displays a JSON preview. |
| `/back-up delete` | Deletes the selected local snapshot. |

All command responses are private. Backup names are unique and restricted to safe filename-independent characters. JSON input is validated before writing. Files are written through a temporary file and renamed into place. Backup files include a schema version and are checked again when loaded.

## Functional boundary — read before relying on it

This release **does not automatically crawl a live Discord server**. The `scope` field labels the kind of JSON snapshot supplied by the user; it does not trigger server/channel scraping. Likewise, `/back-up load` previews a stored snapshot and does not recreate roles, channels, permissions, messages, emojis, or other live Discord objects.

A real Xenon-style server backup/restore feature requires separate, tested integrations with Aliucord's supported Discord models and authorized write operations. This release intentionally does not claim those features are implemented. It does provide working local JSON backup management once compiled and installed.

## Validation status

The source was checked for manifest JSON validity, expected Aliucord project layout, command API signatures documented by Aliucord, path-safe local storage, and ZIP integrity. A full Gradle/Android compile could not be completed in this environment because external Gradle/Maven dependencies are not reachable here. Therefore this archive is **not represented as a successfully compiled or device-tested plugin**. Run the build command above on a machine with JDK 21, Android SDK 36, and network access before installing it.

## Official references

- Aliucord plugin template: https://github.com/Aliucord/plugins-template
- Aliucord Gradle plugin: https://github.com/Aliucord/gradle-plugin
- Aliucord command development docs: https://github.com/Aliucord/documentation/blob/main/plugin-dev/2_commands.md
