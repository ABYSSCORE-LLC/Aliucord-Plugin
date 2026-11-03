# ARCHIVE — Aliucord local backup manager

**Status: source MVP, not a verified installable release.**

ARCHIVE is a Kotlin Aliucord plugin project that registers one slash command:

`/archive action:<create|list|load|delete>`

It stores JSON snapshots under the plugin app's private files directory. It does not upload data, request Discord credentials, automate a user account through private endpoints, or bypass server permissions.

## Current scope and honest limitations

This initial project implements local JSON backup storage and management. The `create` action accepts JSON supplied in the command option; `load` prints the saved JSON. It does **not** yet read a live guild's channels/roles/settings or apply a restore to a destination guild. Those operations require carefully verified Aliucord Discord model/REST integration and permission checks; they are intentionally not faked.

The same local backup file can be used as an input for a future authorized import/restore workflow. Copying another server's configuration is only appropriate where you have permission and where Discord's supported operations allow it. Discord prohibits automating ordinary user accounts as self-bots; this plugin does not attempt that.

## Build requirements

- Android Studio
- Git
- JDK 21+
- Android SDK platform 36
- Network access to the Aliucord Maven repository on first Gradle sync
- An Aliucord installation for device testing

The official Aliucord template currently documents JDK 21+, Kotlin plugin development, `make` and `deployWithAdb` tasks, and the output ZIP location. The official template is the best baseline if the current Gradle plugin version changes:
https://github.com/Aliucord/plugins-template

## Build on Windows

Open this directory in Android Studio and allow Gradle sync. Or, with Gradle installed:

```powershell
gradle :ARCHIVE:make
```

The Aliucord Gradle plugin emits a plugin ZIP under the plugin's `builds/outputs` directory.

For a connected test device with USB debugging authorized:

```powershell
gradle :ARCHIVE:deployWithAdb
```

## Build on Linux/macOS

```bash
gradle :ARCHIVE:make
gradle :ARCHIVE:deployWithAdb
```

## Command usage

- `/archive action:list`
- `/archive action:create name:my-backup json:{"guild":"example","channels":[],"roles":[]}`
- `/archive action:load name:my-backup`
- `/archive action:delete name:my-backup`

Discord limits the size of individual slash-command string options. Large snapshots should be imported from a file using a future file-picker workflow rather than pasted into the command.

## Storage and safety

- Backup files are JSON.
- Data remains in the plugin's private app storage.
- Deleting a backup permanently deletes its local JSON file.
- The current MVP has no encryption-at-rest. Protect the Android device and its app data accordingly.
- `load` is a preview/export-like read operation, not a server restore.

## Compatibility note

Aliucord's template and Gradle plugin evolve. This project follows the currently documented template dependency versions and command registration example, but it has not been compiled against a live Aliucord checkout in this environment. Run a Gradle build before installing. If Aliucord's current Discord version or Gradle plugin API has changed, align versions with:
https://github.com/Aliucord/gradle-plugin
https://github.com/Aliucord/plugins-template
