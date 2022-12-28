import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "dev.mr3n"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
    maven("https://libraries.minecraft.net")
    maven("https://maven.citizensnpcs.co/repo")
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("org.spigotmc:spigot:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0-SNAPSHOT")
    compileOnly("net.citizensnpcs:citizens-main:2.0.30-SNAPSHOT") {
        exclude(group="*",module="*")
    }
    implementation("com.github.moruch4nn:MinePie:4406de1615")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("build") {
    dependsOn("shadowJar")
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}