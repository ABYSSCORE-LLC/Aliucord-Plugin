#!/usr/bin/env python3
"""Offline structural checks for the ARCHIVE source project."""
import json
import pathlib
import sys
import zipfile

root = pathlib.Path(__file__).resolve().parent
required = [
    root / "manifest.json",
    root / "settings.gradle.kts",
    root / "build.gradle.kts",
    root / "gradle.properties",
    root / "plugins/build.gradle.kts",
    root / "plugins/ARCHIVE/build.gradle.kts",
    root / "plugins/ARCHIVE/manifest.json",
    root / "plugins/ARCHIVE/src/main/AndroidManifest.xml",
    root / "plugins/ARCHIVE/src/main/kotlin/com/abysscore/archive/ArchivePlugin.kt",
]
missing = [str(p.relative_to(root)) for p in required if not p.is_file()]
if missing:
    print("FAIL missing files:", ", ".join(missing))
    sys.exit(1)

for manifest in (root / "manifest.json", root / "plugins/ARCHIVE/manifest.json", root / "plugins/ARCHIVE/src/main/assets/manifest.json"):
    data = json.loads(manifest.read_text(encoding="utf-8"))
    assert data["id"] == "com.abysscore.archive"
    assert data["version"] == "0.3.0"
    assert data["main"] == "com.abysscore.archive.ArchivePlugin"

source = required[-1].read_text(encoding="utf-8")
for token in (
    "@AliucordPlugin",
    'commands.registerCommand(',
    'commands.unregisterCommand("back-up")',
    'File(context.filesDir, "archive/backups")',
    'ApplicationCommandType.SUB_COMMAND',
    'getRequiredSubCommandArgs',
):
    assert token in source, f"missing expected source marker: {token}"

print("PASS: required files and all three JSON manifests are valid")
print("PASS: Aliucord entry point, slash-command registration, and local storage markers found")
print("NOTE: this is a structural check, not a Kotlin/Gradle compile")
