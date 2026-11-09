plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openjfx.javafxplugin") version "0.0.14"
}

javafx {
    version = "25.0.3"
    modules("javafx.controls", "javafx.fxml")
}

group = "com.github.mizo432"
version = "0.0.1-SNAPSHOT"
description = "regDemo"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springModulithVersion"] = "2.1.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-h2console")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.springframework.modulith:spring-modulith-runtime")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    mainClass.set("com.github.mizo432.reg.CoolFxApplication")
    val javafxDependencies = configurations.runtimeClasspath.get().files.filter {
        it.name.contains("javafx")
    }
    val modulePath = javafxDependencies.joinToString(File.pathSeparator)
    doFirst {
        jvmArgs = listOf(
            "--module-path", modulePath,
            "--add-modules", "javafx.controls,javafx.fxml"
        )
    }
}
