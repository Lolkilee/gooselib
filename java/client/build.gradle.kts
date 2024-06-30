plugins {
    application
    id("org.graalvm.buildtools.native") version "0.10.2"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.guava)
    implementation("com.esotericsoftware:kryonet:2.22.0-RC1")
    implementation(project(":shared"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "nl.thomasgoossen.gooselib.client.GLClient"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
