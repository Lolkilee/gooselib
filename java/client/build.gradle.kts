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
    implementation("org.slf4j:slf4j-simple:2.0.10")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("io.javalin:javalin:6.1.6")
    implementation("org.apache.commons:commons-compress:1.21")
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
