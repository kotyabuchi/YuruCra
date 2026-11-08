import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    kotlin("jvm") version "2.4.21"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "com.github.kotyabuchi"
version = "0.1.0-SNAPSHOT"

// 対象サーバーのバージョン。上げるときはこの2つを合わせて変更する
val minecraftVersion = "26.3"
val paperApiVersion = "26.3.build.244-stable"
val sqliteVersion = "3.54.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    // 以下はjarに同梱せず、plugin.yml の libraries でサーバーにダウンロードさせる
    compileOnly(kotlin("stdlib"))
    compileOnly("org.xerial:sqlite-jdbc:$sqliteVersion")
}

kotlin {
    jvmToolchain(25)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "apiVersion" to minecraftVersion,
        "kotlinVersion" to project.getKotlinPluginVersion(),
        "sqliteVersion" to sqliteVersion,
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.runServer {
    minecraftVersion(minecraftVersion)
}
