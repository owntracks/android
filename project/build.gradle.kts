// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.2")
        classpath("com.hiya:jacoco-android:0.2")
        classpath("io.objectbox:objectbox-gradle-plugin:2.8.1")
        //noinspection DifferentKotlinGradleVersion
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${KotlinVersion.CURRENT}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}