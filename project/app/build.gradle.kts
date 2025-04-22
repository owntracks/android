import org.gradle.api.tasks.testing.logging.TestLogEvent
import kotlin.io.path.isRegularFile

plugins {
  id("com.android.application")
  id("com.google.dagger.hilt.android")
  id("com.github.triplet.play")
  kotlin("android")
  kotlin("kapt")
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.ksp)
}

apply<EspressoMetadataEmbeddingPlugin>()

val googleMapsAPIKey =
    System.getenv("GOOGLE_MAPS_API_KEY")?.toString() ?: extra.get("google_maps_api_key")?.toString()
    ?: "PLACEHOLDER_API_KEY"

val gmsImplementation: Configuration by configurations.creating

val packageVersionCode: Int = System.getenv("VERSION_CODE")?.toInt() ?: 420503000
val manuallySetVersion: Boolean = System.getenv("VERSION_CODE") != null

android {
  compileSdk = 34
  namespace = "org.owntracks.android"

  defaultConfig {
    applicationId = "org.owntracks.android"
    minSdk = 24
    targetSdk = 34

    versionCode = packageVersionCode
    versionName = "2.5.3"

    val localeCount = fileTree("src/main/res/").map {
      it.toPath()
    }.count { it.isRegularFile() && it.fileName.toString() == "strings.xml" }
    buildConfigField(
        "int", "TRANSLATION_COUNT", localeCount.toString(),
    )

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments.putAll(
        mapOf(
            "clearPackageData" to "false",
            "coverage" to "true",
            "disableAnalytics" to "true",
            "useTestStorageService" to "false",
        ),
    )
    javaCompileOptions {
      annotationProcessorOptions {
        arguments["room.schemaLocation"] = "$projectDir/schemas"
      }
    }
  }

  androidResources {
    generateLocaleConfig = true
  }

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
    dataBinding = true
    viewBinding = true
  }

  dataBinding {
    addKtx = true
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
            "TypographyFractions", "TypographyQuotes", "Typos",
        ),
    )
  }
  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
    animationsDisabled = true
    unitTests {
      isIncludeAndroidResources = true
    }
    managedDevices {
      localDevices {
        val hardware = "Small Phone"
        create("api34aosp") {
          device = hardware
          apiLevel = 34
          systemImageSource = "aosp-atd"
        }
        create("api27aosp") {
          device = hardware
          apiLevel = 27
          systemImageSource = "aosp"
        }
        create("api34gms") {
          device = hardware
          apiLevel = 34
          systemImageSource = "google-atd"
        }
        create("api27gms") {
          device = hardware
          apiLevel = 27
          systemImageSource = "google"
        }
      }
      groups {
        create("allGmsApis") {
          targetDevices.addAll(listOf(devices["api34gms"], devices["api27gms"]))
        }
        create("allAospApis") {
          targetDevices.addAll(listOf(devices["api34aosp"], devices["api27aosp"]))
        }
      }
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

  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_21.toString()
  }

  flavorDimensions.add("locationProvider")
  productFlavors {
    create("gms") {
      dimension = "locationProvider"
      dependencies {
        gmsImplementation(libs.gms.play.services.maps)
        gmsImplementation(libs.play.services.location)
      }
    }
    create("oss") {
      dimension = "locationProvider"
    }
  }
  playConfigs {
    register("gms") {
      enabled.set(true)
      track.set("internal")
      if (manuallySetVersion) {
        resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.IGNORE)
      } else {
        resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO)
      }
    }
  }
}

kapt {
  useBuildCache = true
  correctErrorTypes = true
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<Test> {
  systemProperties["junit.jupiter.execution.parallel.enabled"] = false
  systemProperties["junit.jupiter.execution.parallel.mode.default"] = "same_thread"
  systemProperties["junit.jupiter.execution.parallel.mode.classes.default"] = "concurrent"
  maxParallelForks = 1
}

tasks.withType<JavaCompile>().configureEach {
  options.isFork = true
}

dependencies {
  implementation(libs.bundles.kotlin)
  implementation(libs.bundles.androidx)
  implementation(libs.androidx.test.espresso.idling)

  implementation(libs.google.material)

  // Explicit dependency on conscrypt to give up-to-date TLS support on all devices
  implementation(libs.conscrypt)

  // Mapping
  implementation(libs.osmdroid)

  // Connectivity
  implementation(libs.paho.mqttclient)
  implementation(libs.okhttp)

  // Utility libraries
  implementation(libs.bundles.hilt)
  implementation(libs.bundles.jackson)
  implementation(libs.square.tape2)
  implementation(libs.timber)
  implementation(libs.libsodium)
  implementation(libs.apache.httpcore)
  implementation(libs.commons.codec)
  implementation(libs.bundles.androidx.room)
  implementation(libs.bundles.objectbox.migration)
  implementation(libs.kotlin.datetime)
  implementation(libs.kotlin.serialization)

  // The BC version shipped under com.android is half-broken. Weird certificate issues etc.
  // To solve, we bring in our own version of BC
  implementation(libs.bouncycastle)

  // Widget libraries
  implementation(libs.widgets.materialdrawer) { artifact { type = "aar" } }
  implementation(libs.widgets.materialize) { artifact { type = "aar" } }

  // These Java EE libs are no longer included in JDKs, so we include explicitly
  kapt(libs.bundles.jaxb.annotation.processors)

  // Preprocessors
  kapt(libs.bundles.kapt.hilt)
  ksp(libs.androidx.room.compiler)

  kaptTest(libs.bundles.kapt.hilt)

  testImplementation(libs.mockito.kotlin)
  testImplementation(libs.androidx.core.testing)
  testImplementation(libs.kotlin.coroutines.test)

  androidTestImplementation(libs.bundles.androidx.test)
  androidTestImplementation(libs.barista) {
    exclude("org.jetbrains.kotlin")
  }
  androidTestImplementation(libs.okhttp.mockwebserver)
  androidTestImplementation(libs.bundles.kmqtt)
  androidTestImplementation(libs.square.leakcanary)

  androidTestUtil(libs.bundles.androidx.test.util)

  coreLibraryDesugaring(libs.desugar)
}

// Publishing
// Handled now in the android / playConfigs block
play {
  enabled.set(false)
}
