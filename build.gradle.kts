import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.71"
}

group = "net.phedny"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("net.dv8tion:JDA:4.1.1_122")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}