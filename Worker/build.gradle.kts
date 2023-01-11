import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("com.rabbitmq:amqp-client:5.9.0")
    // table printing
    implementation("de.m3y.kformat:kformat:0.9")
    // gson
    implementation("com.google.code.gson:gson:2.7")

    // use reflection for serialisation
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")

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
    archiveFileName.set("Worker.jar")

    manifest {
        attributes["Main-Class"] = "WorkerMainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}