// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies { classpath(libs.bundles.buildscript) }
}

plugins {
  // The Android app uses AGP's built-in Kotlin; the standalone kotlin.jvm
  // plugin is only applied by the pure-JVM :location-kalman module.
  alias(libs.plugins.android.application).apply(false)
  alias(libs.plugins.kotlin.jvm).apply(false)
  alias(libs.plugins.hilt.android).apply(false)
  alias(libs.plugins.ktfmt).apply(false)
  alias(libs.plugins.ksp).apply(false)
}

extensions.findByName("develocity")?.withGroovyBuilder {
  getProperty("buildScan")?.withGroovyBuilder {
    setProperty("termsOfUseUrl", "https://gradle.com/help/legal-terms-of-use")
    setProperty("termsOfUseAgree", "yes")
  }
}

tasks.wrapper { distributionType = Wrapper.DistributionType.BIN }
