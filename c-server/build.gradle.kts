/*
 * Copyright 2020 Q-Jam B.V.
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.*

val koinVersion = "1.0.1"
val ktorVersion = "1.4.1"

plugins {
    application
    java
    idea

    kotlin("jvm") version "1.4.10"
    kotlin("kapt") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"

    id("com.github.ben-manes.versions") version "0.33.0"
}

group = "com.qjam.c"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/kotlin/exposed")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    implementation("com.github.vjames19.kotlin-futures:kotlin-futures-jdk8:1.2.0")

    implementation("com.google.guava:guava:29.0-jre")

    implementation("com.google.flogger:flogger:0.5.1")
    runtimeOnly("com.google.flogger:flogger-log4j-backend:0.5.1") {
        exclude("com.sun.jmx", "jmxri")
        exclude("com.sun.jdmk", "jmxtools")
        exclude("javax.jms", "jms")
    }
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

    implementation("org.koin:koin-core:2.1.5")
    implementation("com.beust:klaxon:5.4")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")

    implementation("io.netty:netty-all:4.1.29.Final")

    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("com.auth0:java-jwt:3.11.0")

    implementation("org.jetbrains.exposed:exposed:0.17.7")

    testImplementation("org.junit.jupiter", "junit-jupiter", "5.7.0")
    testImplementation("org.koin:koin-test:2.1.5")
}

application {
    mainClassName = "com.qjam.c.ServerKt"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"

    doFirst {
        val versionFolder = "$buildDir/generated/source/kaptKotlin/main/com/qjam/c"
        file(versionFolder).mkdirs()

        val versionFile = file("$versionFolder/Version.kt")
        versionFile.createNewFile()

        val time = SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Date())
        val user = System.getProperty("user.name")
        val hostname = System.getenv("HOST")

        versionFile.writeText(
            "package com.qjam.c\nclass Version{companion object {const val version = \"$version\"\nconst val revision = \"TODO\"\nconst val time = \"$time\"\nconst val by = \"$user@$hostname\"}}"
        )
    }
}
