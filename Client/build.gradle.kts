import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project


plugins {
    kotlin("jvm") version "1.7.21"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    // rabbitmq
    implementation ("com.rabbitmq:amqp-client:5.9.0")
    // printing tables
    implementation("de.m3y.kformat:kformat:0.9")
    // serialisation
    implementation("com.google.code.gson:gson:2.7")
    // for communicating with manager
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-gson:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")


    // to remove logger warnings
    implementation("org.slf4j:slf4j-simple:1.6.1")
    implementation("org.slf4j:slf4j-api")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

tasks.jar {
    archiveFileName.set("Client.jar")

    manifest {
        attributes["Main-Class"] = "ClientMainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}