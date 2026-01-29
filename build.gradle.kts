import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// 1. 플러그인 정의: subprojects 없이 루트에 바로 적용
plugins {
    val kotlinVersion = "1.9.23"
    id("org.springframework.boot") version "3.2.5" // 예시: Java 17 최적화 버전
    id("io.spring.dependency-management") version "1.1.4"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("kapt") version kotlinVersion
}

// 2. 프로젝트 기본 정보 설정 (gradle.properties 기반 혹은 직접 입력)
group = project.property("projectGroup").toString()
version = System.getenv("VERSION") ?: project.property("projectVersion").toString()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral() // 보안 및 속도 측면에서 mavenLocal()보다 우선 권장
    maven { url = uri("https://oss.sonatype.org/content/groups/public") }
}

// 3. 의존성 관리 (BOM 설정)
dependencyManagement {
    imports {
        // 1. Spring Boot BOM 추가 (플러그인 버전과 맞춤)
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.5")
        // 2. Spring Cloud BOM
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    }
}

dependencies {
    // 1. Spring Boot Configuration Processor (KAPT 전용으로 설정)
    // 이 부분이 제대로 안 잡히면 @ConfigurationProperties 사용 시 NonExistentClass 에러가 납니다.
    kapt("org.springframework.boot:spring-boot-dependencies:3.2.5") // BOM 재참조 (필요시)
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // 2. javax.validation -> jakarta.validation 교체
    // 기존의 javax.validation:validation-api 대신 starter-validation을 사용하세요.
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // 3. Servlet API (Boot 3.x에서는 Jakarta Servlet 6.0 사용)
    // 기존 javax.servlet-api:3.1.0은 Boot 3.x와 호환되지 않습니다.
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    // Kotlin & Spring Starters
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Annotation Processors
    kapt("org.springframework.boot:spring-boot-configuration-processor") // annotationProcessor 대신 kapt 사용 권장

    // Spring Cloud & Tracing
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    // 이제 BOM 덕분에 버전 명시 없이 해결됩니다.
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave") // Sleuth 기능을 완전 대체하려면 필요

    // Utils
    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.commons:commons-pool2")
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Logging
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
        // languageVersion을 1.6으로 제한하면 최신 문법 사용이 불가하므로 제거하거나 최신화 권장
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}