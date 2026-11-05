#!/usr/bin/env bash
set -euo pipefail
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
version="8.13"
cache_root="${GRADLE_USER_HOME:-$HOME/.gradle}/archive-bootstrap"
dist="$cache_root/gradle-$version"
if [[ ! -x "$dist/gradle-$version/bin/gradle" ]]; then
  mkdir -p "$cache_root"
  archive="$cache_root/gradle-$version-bin.zip"
  curl -fL "https://services.gradle.org/distributions/gradle-$version-bin.zip" -o "$archive"
  unzip -q -o "$archive" -d "$dist"
  rm -f "$archive"
fi
exec "$dist/gradle-$version/bin/gradle" "$@"
