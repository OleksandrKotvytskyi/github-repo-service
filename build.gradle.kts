import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
	kotlin("jvm") version "2.4.0"
	kotlin("plugin.spring") version "2.4.0"
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

extra["springdocVersion"] = "2.6.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")

	// OpenAPI / Swagger UI generation
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${property("springdocVersion")}")

	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.mockito", module = "mockito-core")
	}
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("com.squareup.okhttp3:mockwebserver:5.5.0")

	// Mockito Kotlin flavour
	testImplementation("org.mockito:mockito-core:5.23.0")
	testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
	testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")

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
