plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.node-gradle.node") version "7.1.0"
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

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("gg.jte:jte-spring-boot-starter-3:3.1.16")
    implementation("gg.jte:jte-kotlin:3.1.16")

    // Redis/Valkey session storage
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
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

node {
    version.set("20.11.1")
    npmVersion.set("10.2.4")
    download.set(true)
    nodeProjectDir.set(file("${project.projectDir}"))
}

/**
 * initialize package.json
 */
tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmInit") {
    group = "npm"
    description = "Initialize npm project with package.json"
    dependsOn("npmSetup")

    args.set(listOf("init", "-y"))
    workingDir.set(file("${project.projectDir}"))
}

/**
 * install or updates npm packages
 */
tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmInstallPackages") {
    group = "npm"
    description = "Install npm packages"
    dependsOn("npmSetup")

    args.set(listOf("install", "--save-dev", "@tailwindcss/cli", "tailwindcss", "daisyui", "alpinejs", "@alpinejs/persist", "esbuild", "htmx.org"))
    workingDir.set(file("${project.projectDir}"))
}

/**
 * compile CSS
 */
tasks.register<Exec>("tailwindCompileCss") {
    group = "npm"
    description = "Compile CSS"
    dependsOn("npmInstallPackages")

    workingDir = file("${project.projectDir}")
    commandLine = listOf("${project.projectDir}/node_modules/.bin/tailwindcss", "-i", "src/main/css/main.css", "-o", "src/main/resources/static/css/main.css")
}

/**
 * compile js
 */
tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmCompileJs") {
    group = "npm"
    description = "Compile JS"
    dependsOn("npmInstallPackages")

    args.set(listOf("exec", "npx", "esbuild", "--", "src/main/js/main.js", "--bundle", "--outfile=src/main/resources/static/js/main.js"))
    workingDir.set(file("${project.projectDir}"))
}

tasks.named<Delete>("clean") {
    delete(
        file("${project.projectDir}/node_modules"),
        file("${project.projectDir}/package.json"),
        file("${project.projectDir}/package-lock.json"),
        file("${project.projectDir}/src/main/resources/static/css/main.css"),
        file("${project.projectDir}/src/main/resources/static/js/main.js")
    )
}
