plugins {
    kotlin("jvm") version "2.0.10"
    application
}

group = "mx.edu.uttt"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.16") //logs
    implementation("io.github.java-native:jssc:2.9.6") //serial
    implementation("io.javalin:javalin:6.3.0") //framework
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0-rc1") //json
    implementation("com.zaxxer:HikariCP:6.0.0") //pool de conexiones
    implementation("org.firebirdsql.jdbc:jaybird:5.0.5.java11") //driver firebird
    implementation("com.github.seratch:kotliquery:1.9.0") //sql en kotlin
}

kotlin {
    jvmToolchain(21)
}
/* Build a bundle Jar file */
tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to "mx.edu.uttt.MainKt")
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.runtimeClasspath.get()
        .onEach { println("add from dependencies: ${it.name}") }
        .filter { it.name.endsWith("jar") }
        .map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    sourceSets.main.get()
        .allSource.forEach { println("add from sources: ${it.canonicalPath}") }
}

application {
    mainClass.set("mx.edu.uttt.MainKt")
}