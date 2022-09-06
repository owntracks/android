plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    id("com.github.triplet.play")
    kotlin("android")
    kotlin("kapt")
    id("io.objectbox")
    id("com.hiya.jacoco-android")
}

apply<EspressoScreenshotsPlugin>()

val googleMapsAPIKey = extra.get("google_maps_api_key")?.toString() ?: "PLACEHOLDER_API_KEY"
val rootJacocoVersion = "0.8.7"

jacoco {
    version = rootJacocoVersion
    toolVersion = rootJacocoVersion
}


val gmsImplementation: Configuration by configurations.creating
val numShards = System.getenv("CIRCLE_NODE_TOTAL") ?: "0"
val shardIndex = System.getenv("CIRCLE_NODE_INDEX") ?: "0"

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "org.owntracks.android"
        minSdk = 21
        targetSdk = 31

        versionCode = 20410000
        versionName = "2.4.10"

        val locales = listOf("en", "de", "fr", "es", "ru", "ca", "pl", "cs", "ja", "pt", "zh", "da")
        buildConfigField(
            "String[]",
            "TRANSLATION_ARRAY",
            "new String[]{\"" + locales.joinToString("\",\"") + "\"}"
        )
        resourceConfigurations.addAll(locales)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "clearPackageData" to "true",
                "coverage" to "true",
                "disableAnalytics" to "true",
                "useTestStorageService" to "true",
                "numShards" to numShards,
                "shardIndex" to shardIndex
            )
        )
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
                    file("proguard-rules.pro")
                )
            )
            resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
            signingConfig = signingConfigs.findByName("release")
        }

        named("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro")
                )
            )
            resValue("string", "GOOGLE_MAPS_API_KEY", googleMapsAPIKey)
            applicationIdSuffix = ".debug"
            isTestCoverageEnabled = true
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    dataBinding {
        addKtx = true
    }

    packagingOptions {
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
                "Typos"
            )
        )
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        unitTests {
            isIncludeAndroidResources = true
            isIncludeAndroidResources = true
        }
    }
    testCoverage {
        jacocoVersion = rootJacocoVersion
    }

    tasks.withType<Test> {
        testLogging {
            events("passed", "skipped", "failed")
            setExceptionFormat("full")
        }
        reports.junitXml.required.set(true)
        reports.html.required.set(false)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    flavorDimensions.add("locationProvider")
    productFlavors {
        create("gms") {
            dimension = "locationProvider"
            dependencies {
                gmsImplementation("com.google.android.gms:play-services-maps:18.1.0")
                gmsImplementation("com.google.android.gms:play-services-location:20.0.0")
            }
        }
        create("oss") {
            dimension = "locationProvider"
        }
    }
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("eventBusIndex", "org.owntracks.android.EventBusIndex")
    }
}

tasks.withType<Test> {
    systemProperties["junit.jupiter.execution.parallel.enabled"] = true
    systemProperties["junit.jupiter.execution.parallel.mode.default"] = "concurrent"
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
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

    // Utility libraries
    implementation(libs.bundles.hilt)
    implementation(libs.eventbus)

    // Connectivity
    implementation(libs.paho.mqttclient)
    implementation(libs.okhttp)

    implementation(libs.bundles.jackson)
    implementation(libs.square.tape2)
    implementation(libs.processphoenix)
    implementation(libs.timber)
    implementation(libs.libsodium)
    implementation(libs.apache.httpcore)
    implementation(libs.commons.codec)

    // Widget libraries
    implementation(libs.widgets.materialedittext)
    implementation(libs.widgets.materialdrawer) { artifact { type = "aar" } }
    implementation(libs.widgets.materialize) { artifact { type = "aar" } }
    implementation(libs.widgets.preferencex)

    // These Java EE libs are no longer included in JDKs, so we include explicitly
    kapt(libs.bundles.jaxb.annotation.processors)

    // Preprocessors
    kapt(libs.eventbus.annotationprocessor)
    kapt(libs.bundles.kapt.hilt)

    kaptTest(libs.bundles.kapt.hilt)

    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.androidx.core.testing)

    androidTestImplementation(libs.bundles.androidx.test)
    androidTestImplementation(libs.barista) {
        exclude("org.jetbrains.kotlin")
    }
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.kmqtt)
    androidTestImplementation(libs.square.leakcanary)

    androidTestUtil(libs.bundles.androidx.test.util)
}


// Publishing
val serviceAccountCredentials = file("owntracks-android-gcloud-creds.json")

play {
    if (this@Build_gradle.serviceAccountCredentials.exists()) {
        enabled.set(true)
        serviceAccountCredentials.set(this@Build_gradle.serviceAccountCredentials)
    } else {
        enabled.set(false)
    }
    track.set("internal")

    resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO)
}

val codesTask = tasks.register<GetLatestVersionCodeMinusOne>("getLatestVersionCodeMinusOne") {
    dependsOn("processGmsReleaseVersionCodes")

    codes.set(file("build/intermediates/gpp/gmsRelease/available-version-codes.txt"))
    outCode.set(file("build/intermediates/version-code-minus-one.txt"))
}

androidComponents {
    onVariants { variant ->
        val minusOne = System.getenv("MAKE_APK_SAME_VERSION_CODE_AS_GOOGLE_PLAY")
        if (!minusOne.isNullOrEmpty()) {
            for (output in variant.outputs) {
                output.versionCode.set(codesTask.flatMap { it.outCode }
                    .map { it.asFile.readText().toInt() })
            }
        }
    }
}
