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

    // テストはサーバーなしで動く純粋なロジックだけを対象にする
    testImplementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// resourcepack/ を build/resourcepack/PumpkingPack.zip にまとめる（プレイヤーに配るリソースパック）
val resourcePackZip = tasks.register<Zip>("resourcePackZip") {
    group = "build"
    description = "リソースパックをzipにまとめる"
    from("resourcepack")
    archiveFileName.set("PumpkingPack.zip")
    destinationDirectory.set(layout.buildDirectory.dir("resourcepack"))
}

tasks.build {
    dependsOn(resourcePackZip)
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
