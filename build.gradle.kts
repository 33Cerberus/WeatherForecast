plugins {
    kotlin("jvm") version "2.4.10"
    application
}

group = "io.github.cerberus33"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.github.cerberus33.weather.MainKt")
}

tasks.test {
    useJUnitPlatform()
}