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
    implementation("io.javalin:javalin:6.3.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("io.github.java-native:jssc:2.9.6")
    implementation("org.webjars.npm:vue:3.5.4")
    implementation("org.webjars.npm:vuetify:3.7.0")
    implementation("org.webjars.npm:mdi__font:7.4.47")
    implementation("org.webjars:font-awesome:6.5.2")
    implementation("org.webjars.npm:roboto-fontface:0.10.0")
    implementation("io.javalin:javalin:6.3.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0-rc1")
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.firebirdsql.jdbc:jaybird:5.0.5.java11")
    implementation("com.github.seratch:kotliquery:1.9.0")
    implementation("org.webjars.npm:sweetalert2:11.14.2")
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