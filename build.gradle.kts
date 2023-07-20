plugins {
    id("java")
}

group = "org.dooq"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    compileOnly("org.jetbrains:annotations:24.0.1")

    implementation("software.amazon.awssdk:dynamodb:2.20.22")
    implementation("org.ow2.asm:asm:9.5")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("org.openjdk.jmh:jmh-core:1.36")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.36")
}

tasks.test {
    useJUnitPlatform()
}