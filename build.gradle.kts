import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.9.23"
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("kapt") version kotlinVersion

//    `maven-publish`
//    signing
}

group = project.property("projectGroup").toString()
version = System.getenv("VERSION") ?: project.property("projectVersion").toString()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/groups/public") }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.5")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    }
}

dependencies {
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.commons:commons-pool2")
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kapt {
    keepJavacAnnotationProcessors = true
    showProcessorStats = true
}

// 5. 컴파일 옵션 설정
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

//publishing {
//    publications {
//        create<MavenPublication>("mavenJava") {
//            from(components["java"])
//
//            groupId = project.group.toString()
//            artifactId = project.name
//            version = project.version.toString()
//
//            pom {
//                name.set("Common Logging Library")
//                description.set("Common logging utilities for internal services")
//            }
//        }
//    }
//
//    repositories {
//        maven {
//            name = "Nexus"
//            val releasesRepoUrl = uri("https://your-nexus-url/repository/maven-releases/")
//            val snapshotsRepoUrl = uri("https://your-nexus-url/repository/maven-snapshots/")
//            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
//
//            credentials {
//                username = project.findProperty("nexusUsername")?.toString() ?: System.getenv("NEXUS_USERNAME")
//                password = project.findProperty("nexusPassword")?.toString() ?: System.getenv("NEXUS_PASSWORD")
//            }
//        }
//    }
//}
//
//signing {
//    val signingKey = project.findProperty("signingKey")?.toString() ?: System.getenv("GPG_SIGNING_KEY")
//    val signingPassword = project.findProperty("signingPassword")?.toString() ?: System.getenv("GPG_PASSPHRASE")
//
//    if (signingKey != null && signingPassword != null) {
//        useInMemoryPgpKeys(signingKey, signingPassword)
//        sign(publishing.publications["mavenJava"])
//    }
//}
//
//java {
//    withSourcesJar()
//    withJavadocJar()
//}