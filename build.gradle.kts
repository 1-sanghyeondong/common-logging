import org.gradle.kotlin.dsl.*

val projectGroup: String by project
val projectVersion: String by project
val springBootVersion: String by project
val springCoreVersion: String by project
val springCloudConfigVersion: String by project
val commonIoVersion: String by project
val springCloudVersion: String by project
val logbackVersion: String by project

plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jlleitschuh.gradle.ktlint") apply false // version "11.0.0"

    kotlin("jvm") version "1.9.23" // 또는 2.0.x 로 맞추면 완전 최신
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.jpa") version "1.9.23"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("org.jlleitschuh.gradle:ktlint-gradle:9.3.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
    }
}

allprojects {
    group = projectGroup
    version = System.getenv("VERSION") ?: projectVersion

    repositories {
        mavenLocal()

        maven {
            url = uri("https://oss.sonatype.org/content/groups/public")
        }

        maven {
            url = uri("http://oss.jfrog.org/artifactory/oss-snapshot-local")
            isAllowInsecureProtocol = true
        }
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-kapt")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }

        imports {
            mavenBom("org.springframework.cloud:spring-cloud-config:$springCloudConfigVersion")
        }

        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        }
    }

    dependencies {
        implementation(kotlin("reflect"))
        implementation(kotlin("stdlib-jdk8"))

        compileOnly("org.springframework.boot:spring-boot-configuration-processor")
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
        implementation("org.springframework:spring-core:$springCoreVersion")
        api("org.springframework.boot:spring-boot-starter-parent:$springBootVersion")

        implementation("org.springframework.boot:spring-boot-starter")
        implementation("org.springframework.cloud:spring-cloud-starter-config")
        implementation("org.springframework.cloud:spring-cloud-starter-sleuth")
        implementation("org.apache.commons:commons-lang3")
        implementation("org.apache.commons:commons-pool2")
        compileOnly("commons-io:commons-io:$commonIoVersion")

        implementation("ch.qos.logback:logback-classic:$logbackVersion")
        implementation("ch.qos.logback:logback-core:$logbackVersion")
        implementation("org.slf4j:log4j-over-slf4j:1.7.32")
        implementation("net.logstash.logback:logstash-logback-encoder:6.6")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
            languageVersion = "1.6"
            apiVersion = "1.6"
        }
    }
}
