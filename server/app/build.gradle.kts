plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.guava)
    implementation("com.esotericsoftware:kryonet:2.22.0-RC1")
    implementation("org.mapdb:mapdb:3.0.4")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "nl.thomasgoossen.gooselib.server.GLServer"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
