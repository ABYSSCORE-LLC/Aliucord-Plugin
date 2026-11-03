# ARCHIVE 0.2.0 — Aliucord plugin project

**Creator credit:** ABYSSCORE-LLC and ultrajbs.

## Important: choose the correct ZIP

This archive is the **source project**. It is not a compiled Aliucord plugin binary. The project includes a root `manifest.json` (and a copy in the plugin directory) so tools that inspect imported archives can identify the project. The official Aliucord build system still compiles the plugin through its Gradle `make` task; the project manifest does not replace compilation.

The official Aliucord template documents that `make` produces the installable plugin ZIP under the plugin's `builds/outputs` directory. A compiled plugin cannot honestly be supplied from this environment because the Gradle/Android/Aliucord dependencies could not be downloaded or compiled here.

## Build on Windows

Requirements: JDK 21+, Android SDK platform 36, and network access for the first build.

From this folder, open PowerShell or Command Prompt and run:

```bat
.\gradlew.bat :plugins:ARCHIVE:make
```

Or open the folder in Android Studio, sync Gradle, then run the `make` task for `plugins:ARCHIVE`.

The official Aliucord Gradle plugin will print the location of the generated installable ZIP. Use the ZIP it builds, not this source-project ZIP, when installing into Aliucord. Do not rename the compiled plugin ZIP or extract it before placing it in `Aliucord/plugins`.

For a device connected through authorized ADB:

```bat
.\gradlew.bat :plugins:ARCHIVE:deployWithAdb
```

## Commands in the plugin

- `/back-up create name:<name> json:<json-object>` — stores the JSON object locally.
- `/back-up list` — lists local backups.
- `/back-up load name:<name-or-id>` — displays a preview of the stored JSON.
- `/back-up delete name:<name-or-id>` — removes the matching local backup.

All replies are private. Backup files are stored only in Aliucord's private app files directory. The plugin makes no network requests and does not collect a Discord token.

## Scope — not a Xenon replacement yet

This build is a local JSON archive manager. It does **not** currently inspect a live server's channels, roles, permissions, categories, emojis, or settings; it does not copy server content; and `load` is a preview, not a server restore. No server data is scraped or altered. A true authorized server backup/restore feature requires a separate integration with Aliucord's supported Discord models and operations, including permission checks.

## Compatibility and validation

The Gradle layout and dependency versions are aligned with the current official Aliucord plugin template and Gradle plugin documentation at the time this archive was prepared. The project was statically checked for its expected entry point, JSON manifest, Gradle project files, and ZIP integrity. A full Gradle compile was **not** run in this environment, so this must not be represented as a compiled or tested plugin binary.

References:

- https://github.com/Aliucord/plugins-template
- https://github.com/Aliucord/gradle-plugin
- https://github.com/Aliucord/documentation/tree/main/plugin-dev
