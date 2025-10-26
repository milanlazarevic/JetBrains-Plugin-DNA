plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.0"
}
tasks.withType<JavaExec> {
    standardOutput = System.out
    errorOutput = System.err
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("ai.djl:api:0.28.0")
    implementation("ai.djl.huggingface:tokenizers:0.28.0")
    implementation("ai.djl.onnxruntime:onnxruntime-engine:0.28.0")
    runtimeOnly("com.microsoft.onnxruntime:onnxruntime:1.17.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}


