plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.aliucord.plugin")
}

version = "0.1.0"
description = "Local-first server configuration archive manager"

android {
    namespace = "com.abysscore.archive"
    compileSdk = 36
    defaultConfig { minSdk = 23 }
}

dependencies {
    compileOnly("com.discord:discord:126021")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
}

aliucord {
    changelog.set("""
        # 0.1.0
        * Initial local archive manager.
        * JSON backup create/list/load/delete commands.
    """.trimIndent())
    deploy.set(false)
}
