// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://storage.googleapis.com/r8-releases/raw")
        }
    }
    dependencies {
        classpath(libs.bundles.buildscript)
    }
}

plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.hilt.android).apply(false)
    alias(libs.plugins.triplet).apply(false)
    alias(libs.plugins.ktfmt).apply(false)
    alias(libs.plugins.ksp).apply(false)
}

extensions.findByName("buildScan")
    ?.withGroovyBuilder {
        setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
        setProperty("termsOfServiceAgree", "yes")
    }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.BIN
}
