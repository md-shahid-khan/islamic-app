buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        // Enforce modern commons-compress so Spring Boot 3.3.5 bootJar doesn't conflict with AGP's bundled version
        classpath("org.apache.commons:commons-compress:1.26.2")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
