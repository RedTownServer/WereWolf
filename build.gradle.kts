import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "dev.mr3n"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    // for spigot
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://libraries.minecraft.net")
    // for protocollib
    maven("https://repo.dmulloy2.net/repository/public/")
    // for minepie
    maven("https://jitpack.io")
    // for plugmanx
    maven("https://raw.githubusercontent.com/TheBlackEntity/PlugMan/repository/")
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("org.spigotmc:spigot:1.19.3-R0.1-SNAPSHOT")
    compileOnly(files("libs/ProtocolLib.jar"))
    compileOnly("com.rylinaux:PlugMan:2.2.9")
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