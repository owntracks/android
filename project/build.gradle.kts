// Top-level build file where you can add configuration options common to all sub-projects/modules.

ext["hiltVersion"] = "2.40.1"
plugins {
    id("org.jetbrains.kotlin.android").version("1.6.10").apply(false)
    id("dagger.hilt.android.plugin").version("2.40.1").apply(false)
    id("io.objectbox").version("3.1.1").apply(false)
    id("com.github.triplet.play").version("3.7.0").apply(false)
    id("com.hiya.jacoco-android").version("0.2").apply(false)
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}
