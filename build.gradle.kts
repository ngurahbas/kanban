plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("gg.jte.gradle") version "3.2.1"
}

group = "app"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}


dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:2.0.2")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("gg.jte:jte-spring-boot-starter-3:3.2.1")
    implementation("gg.jte:jte-kotlin:3.2.1")

    // Redis/Valkey session storage
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("processResources") {
    dependsOn("tailwindCompileCss", "npmCompileJs")
}

/**
 * compile CSS
 */
tasks.register<Exec>("tailwindCompileCss") {
    group = "npm"
    description = "Compile CSS"
    workingDir = file("${project.projectDir}")
    commandLine = listOf("${project.projectDir}/node_modules/.bin/tailwindcss", "-i", "src/main/css/main.css", "-o", "src/main/resources/static/css/main.css")
}

/**
 * compile js
 */
tasks.register<Exec>("npmCompileJs") {
    group = "npm"
    description = "Compile JS"
    commandLine = listOf("${project.projectDir}/node_modules/.bin/esbuild", "--bundle", "--minify", "--outfile=./src/main/resources/static/js/main.js", "./src/main/js/main")
}

jte {
    precompile()
    sourceDirectory.set(file("src/main/jte").toPath())
    targetDirectory.set(file("build/jte-classes").toPath())
    contentType.set(gg.jte.ContentType.Html)
    kotlinCompileArgs.set(arrayOf("-jvm-target", "21"))
    generate()
}

tasks.named("bootJar") {
    dependsOn("precompileJte")
}

tasks.named<Delete>("clean") {
    delete(
        file("${project.projectDir}/src/main/resources/static/css/main.css"),
        file("${project.projectDir}/src/main/resources/static/js/main.js")
    )
}
