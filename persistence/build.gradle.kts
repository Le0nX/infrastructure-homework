plugins {
    kotlin("jvm")
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.plugin.jpa")
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation(project(":businessPeople"))
    implementation(project(":useCasePeople"))

    implementation("javax.persistence:javax.persistence-api:2.2")
    implementation("org.apache.logging.log4j:log4j-core:2.18.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.7.2")

    // kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // tests
    // we don't have any tests right now...
    // testCompile("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}