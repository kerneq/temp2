// See https://gradle.org and https://github.com/gradle/kotlin-dsl

// Apply the java plugin to add support for Java
plugins {
    java
    application
}

repositories {
    jcenter()
}

dependencies {
    // One-nio http server
    compile("ru.odnoklassniki:one-nio:1.0.1")

    // Annotations for better code documentation
    compile("com.intellij:annotations:12.0")

    // JUnit test framework
    testCompile("junit:junit:4.12")

    // HTTP client for unit tests
    compile("org.apache.httpcomponents:fluent-hc:4.5.3")

    // Guava for tests
    compile("com.google.guava:guava:23.1-jre")
}

tasks {
    "test"(Test::class) {
        maxHeapSize = "1g"
    }
}

application {
    // Define the main class for the application
    mainClassName = "ru.mail.polis.Cluster"

    // And limit Xmx
    applicationDefaultJvmArgs = listOf("-Xmx1g", "-Xverify:none")
}
