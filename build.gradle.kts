import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val sqliteJDBCVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.8.21"
    application
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "com.github.kotyabuchi.YuruCra"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:$sqliteJDBCVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType(ShadowJar::class.java) {
    destinationDirectory.set(File("C:\\Development\\Minecraft\\Servers\\TestServer\\plugins"))
    dependencies {
        exclude(dependency("io.papermc.paper:paper-api"))
    }
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}