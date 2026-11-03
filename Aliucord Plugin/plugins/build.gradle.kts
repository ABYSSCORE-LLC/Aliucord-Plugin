@file:Suppress("UnstableApiUsage")

import com.aliucord.gradle.AliucordExtension
import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

subprojects {
    val libs = rootProject.libs

    apply(plugin = libs.plugins.android.library.get().pluginId)
    apply(plugin = libs.plugins.aliucord.plugin.get().pluginId)
    apply(plugin = libs.plugins.kotlin.android.get().pluginId)
    apply(plugin = libs.plugins.ktlint.get().pluginId)

    configure<LibraryExtension> {
        namespace = "com.abysscore.archive"
        compileSdk = 36
        defaultConfig {
            minSdk = 24
        }
        buildFeatures {
            buildConfig = true
            resValues = true
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    configure<AliucordExtension> {
        author("ABYSSCORE-LLC", 0L, hyperlink = true)
        github("https://github.com/ABYSSCORE-LLC")
    }

    configure<KtlintExtension> {
        version.set(libs.versions.ktlint.asProvider())
        coloredOutput.set(true)
        ignoreFailures.set(true)
    }

    configure<KotlinAndroidExtension> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            optIn.add("kotlin.RequiresOptIn")
        }
    }

    dependencies {
        val compileOnly by configurations
        compileOnly(libs.discord)
        compileOnly(libs.aliucord)
        compileOnly(libs.kotlin.stdlib)
    }
}
