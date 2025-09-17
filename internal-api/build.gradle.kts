tasks.getByName("jar") {
    enabled = true
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    setVersion(System.getenv("VERSION") ?: project.version)
    enabled = true
    archiveClassifier.set("boot")
}

val javaxValidationVersion: String by project

repositories {
    mavenCentral()
    maven("https://jitpack.io")

    maven {
        url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlindl/maven")
    }
}

dependencies {
    api(project(":common-api"))

    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("javax.validation:validation-api:$javaxValidationVersion")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
}
