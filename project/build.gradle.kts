// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies { classpath(libs.bundles.buildscript) }
}

plugins {
  alias(libs.plugins.android.application).apply(false)
  alias(libs.plugins.kotlin.android).apply(false)
  alias(libs.plugins.kotlin.jvm).apply(false)
  alias(libs.plugins.hilt.android).apply(false)
  alias(libs.plugins.triplet).apply(false)
  alias(libs.plugins.ktfmt).apply(false)
  alias(libs.plugins.ksp).apply(false)
  id("com.xcporter.metaview").version("0.0.6")
}

generateUml { classTree { target = file("app/src/main") } }

extensions.findByName("buildScan")?.withGroovyBuilder {
  setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
  setProperty("termsOfServiceAgree", "yes")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions { jvmTarget = JavaVersion.VERSION_21.toString() }
}

tasks.wrapper { distributionType = Wrapper.DistributionType.BIN }
