import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  id("com.android.application")
  kotlin("android")
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
  id("com.google.dagger.hilt.android")
  alias(libs.plugins.ktfmt)
}

apply<EspressoMetadataEmbeddingPlugin>()

val googleMapsAPIKey =
    System.getenv("GOOGLE_MAPS_API_KEY")?.toString()
        ?: extra.get("google_maps_api_key")?.toString()
        ?: "PLACEHOLDER_API_KEY"

val gmsImplementation: Configuration by configurations.creating

val packageVersionCode: Int = System.getenv("VERSION_CODE")?.toInt() ?: 1
val manuallySetVersion: Boolean = System.getenv("VERSION_CODE") != null
val enablePlayPublishing: Boolean = !System.getenv("ANDROID_PUBLISHER_CREDENTIALS").isNullOrBlank()

android {
  compileSdk = 36
  namespace = "org.owntracks.android"

  defaultConfig {
    applicationId = "org.owntracks.android"
    minSdk = 24
    targetSdk = 36

    versionCode = packageVersionCode
    versionName = "2.5.6"

    val localeCount = fileTree("src/main/res/").matching { include("**/strings.xml") }.files.size

    buildConfigField(
        "int",
        "TRANSLATION_COUNT",
        localeCount.toString(),
    )

    testInstrumentationRunner = "org.owntracks.android.testutils.hilt.CustomTestRunner"

    testInstrumentationRunnerArguments.putAll(
        mapOf(
            "clearPackageData" to "false",
            "coverage" to "true",
            "disableAnalytics" to "true",
            "useTestStorageService" to "false",
        ),
    )
    javaCompileOptions {
      annotationProcessorOptions { arguments["room.schemaLocation"] = "$projectDir/schemas" }
    }
  }

  androidResources { generateLocaleConfig = true }

  if (!System.getenv("KEYSTORE_PASSPHRASE").isNullOrBlank()) {
    signingConfigs {
      register("release") {
        keyAlias = "upload"
        keyPassword = System.getenv("KEYSTORE_PASSPHRASE")
        storeFile = file("../owntracks.release.keystore.jks")
        storePassword = System.getenv("KEYSTORE_PASSPHRASE")
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = true
        enableV4Signing = true
      }
    }
  }

  buildTypes {
    named("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles.addAll(
          listOf(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              file("proguard-rules.pro"),
          ),
      )
      resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
      signingConfig = signingConfigs.findByName("release")
    }

    named("debug") {
      isMinifyEnabled = false
      isShrinkResources = false
      isPseudoLocalesEnabled = true
      proguardFiles.addAll(
          listOf(
              getDefaultProguardFile("proguard-android-optimize.txt"),
              file("proguard-rules.pro"),
          ),
      )
      resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
      applicationIdSuffix = ".debug"
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }
  }

  buildFeatures {
    buildConfig = true
    compose = true
  }

  packaging {
    resources.excludes.add("META-INF/*")
    jniLibs.useLegacyPackaging = false
  }

  lint {
    baseline = file("../../lint/lint-baseline.xml")
    lintConfig = file("../../lint/lint.xml")
    checkAllWarnings = true
    warningsAsErrors = false
    abortOnError = false
    disable.addAll(
        setOf(
            "TypographyFractions",
            "TypographyQuotes",
            "Typos",
        ),
    )
  }
  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    animationsDisabled = true
    unitTests { isIncludeAndroidResources = true }
    managedDevices {
      localDevices {}
      groups {}
    }
  }

  tasks.withType<Test> {
    testLogging {
      events(
          TestLogEvent.SKIPPED,
          TestLogEvent.FAILED,
          TestLogEvent.STANDARD_OUT,
          TestLogEvent.STANDARD_ERROR,
      )
      setExceptionFormat("full")
      showStandardStreams = true

      showCauses = true
      showExceptions = true
      showStackTraces = true
    }
    reports.junitXml.required.set(true)
    reports.html.required.set(true)
    outputs.upToDateWhen { false }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    isCoreLibraryDesugaringEnabled = true
  }

  flavorDimensions.add("locationProvider")
  productFlavors {
    create("gms") { dimension = "locationProvider" }
    create("oss") { dimension = "locationProvider" }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
  }
}

// kapt block disabled - using KSP instead
// kapt {
//   useBuildCache = true
//   correctErrorTypes = true
// }

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

tasks.withType<Test> {
  systemProperties["junit.jupiter.execution.parallel.enabled"] = false
  systemProperties["junit.jupiter.execution.parallel.mode.default"] = "same_thread"
  systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"
  maxParallelForks = 1
}

tasks.withType<JavaCompile>().configureEach { options.isFork = true }

dependencies {
  implementation(libs.bundles.kotlin)
  implementation(libs.bundles.androidx)
  implementation(libs.lifecycle.service)
  implementation(libs.androidx.test.espresso.idling)

  // Compose
  implementation(platform(libs.compose.bom))
  implementation(libs.bundles.compose)
  implementation(libs.hilt.navigation.compose)
  debugImplementation(libs.compose.ui.tooling)

  implementation(libs.google.material)

  // Explicit dependency on conscrypt to give up-to-date TLS support on all devices
  implementation(libs.conscrypt)

  // Mapping
  implementation(libs.osmdroid)
  "gmsImplementation"(libs.gms.play.services.maps)
  "gmsImplementation"(libs.play.services.location)
  "gmsImplementation"(libs.maps.compose)

  // Connectivity
  implementation(libs.paho.mqttclient)
  implementation(libs.okhttp)

  // Utility libraries
  implementation(libs.bundles.hilt)
  implementation(libs.bundles.jackson)
  implementation(libs.square.tape2)
  implementation(libs.timber)
  implementation(libs.apache.httpcore)
  implementation(libs.bundles.androidx.room)
  implementation(libs.bundles.objectbox.migration)
  implementation(libs.kotlin.datetime)
  implementation(libs.kotlin.serialization)

  // The BC version shipped under com.android is half-broken. Weird certificate issues etc.
  // To solve, we bring in our own version of BC
  implementation(libs.bouncycastle)

  // Widget libraries
  implementation(libs.widgets.materialize) { artifact { type = "aar" } }

  // These Java EE libs are no longer included in JDKs, so we include explicitly
  // kapt(libs.bundles.jaxb.annotation.processors) // Temporarily disabled to test KSP

  // Preprocessors (using KSP)
  ksp(libs.hilt.compiler)
  ksp(libs.hilt.androidx)
  ksp(libs.androidx.room.compiler)

  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.androidx.core.testing)
  testImplementation(libs.kotlin.coroutines.test)

  androidTestImplementation(libs.bundles.androidx.test)

  // Hilt Android Testing
  androidTestImplementation(libs.hilt.android.testing)

  androidTestImplementation(libs.barista) { exclude("org.jetbrains.kotlin") }
  androidTestImplementation(libs.okhttp.mockwebserver)
  androidTestImplementation(libs.bundles.kmqtt)
  androidTestImplementation(libs.square.leakcanary)

  androidTestUtil(libs.bundles.androidx.test.util)

  coreLibraryDesugaring(libs.desugar)

}
