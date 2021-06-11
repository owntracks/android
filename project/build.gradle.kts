// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    extra.apply {
        set("dagger-version","2.37")
    }
    repositories {
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.1")
        classpath("com.hiya:jacoco-android:0.2")
        classpath("io.objectbox:objectbox-gradle-plugin:2.9.1")
        //noinspection DifferentKotlinGradleVersion
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${rootProject.extra["dagger-version"]}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}