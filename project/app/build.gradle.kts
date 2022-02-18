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

        versionCode = 20408000
        versionName = "2.4.8"

        val locales = listOf("en", "de", "fr", "es", "ru", "ca", "pl", "cs", "ja", "pt", "zh")
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
                "coverageFilePath" to "/sdcard/coverage/",
                "disableAnalytics" to "true",
                "useTestStorageService" to "false", // TODO: use this when we get to AGP 7.1
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
        baselineFile = file("../../lint/lint-baseline.xml")
        isCheckAllWarnings = true
        isWarningsAsErrors = false
        isAbortOnError = false
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
                gmsImplementation("com.google.android.gms:play-services-maps:18.0.1")
                gmsImplementation("com.google.android.gms:play-services-location:19.0.1")
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

val okHttpVersion = "4.9.3"
val jacksonVersion = "2.13.1"
val materialDialogsVersion = "0.9.6.0"
val espressoVersion = "3.4.0"
val kotlinCoroutinesVersion = "1.6.0"
val jaxbVersion = "3.0.2"
val hiltVersion = rootProject.ext["hiltVersion"]

dependencies {
    // AndroidX
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.work:work-runtime:2.7.1")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.test.espresso:espresso-idling-resource:${espressoVersion}")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.4.0")

    implementation("com.google.android.material:material:1.5.0")

    // Explicit dependency on conscrypt to give up-to-date TLS support on all devices
    implementation("org.conscrypt:conscrypt-android:2.5.2")

    // Mapping
    implementation("org.osmdroid:osmdroid-android:6.1.11")

    // Utility libraries
    implementation("com.google.dagger:hilt-android:${hiltVersion}")
    implementation("org.greenrobot:eventbus:3.2.0")

    // Connectivity
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("com.squareup.okhttp3:okhttp:${okHttpVersion}")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-core:${jacksonVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlinCoroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${kotlinCoroutinesVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")

    implementation("com.squareup.tape2:tape:2.0.0-beta1")
    implementation("com.jakewharton:process-phoenix:2.1.1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.github.joshjdevl.libsodiumjni:libsodium-jni-aar:2.0.2")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.1.3")
    implementation("commons-codec:commons-codec:1.15")
    implementation("com.github.joschi.jackson:jackson-datatype-threetenbp:2.12.5")
    implementation("org.threeten:threetenbp:1.5.2") // Jackson-datatype brings in an earlier vesion, so pin the later

    // Widget libraries
    implementation("com.rengwuxian.materialedittext:library:2.1.4")
    implementation("com.mikepenz:materialdrawer:6.1.2@aar") { isTransitive = true }
    implementation("com.mikepenz:materialize:1.2.1@aar")
    implementation("com.takisoft.preferencex:preferencex:1.1.0")

    // These Java EE libs are no longer included in JDKs, so we include explicitly
    kapt("javax.xml.bind:jaxb-api:2.3.1")
    kapt("com.sun.xml.bind:jaxb-core:$jaxbVersion")
    kapt("com.sun.xml.bind:jaxb-impl:$jaxbVersion")

    // Preprocessors
    kapt("org.greenrobot:eventbus-annotation-processor:3.2.0")
    kapt("com.google.dagger:hilt-android-compiler:${hiltVersion}")

    kaptTest("com.google.dagger:hilt-android-compiler:${hiltVersion}")

    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("androidx.arch.core:core-testing:2.1.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test:core-ktx:1.4.0")
    androidTestImplementation("com.adevinta.android:barista:4.2.0") {
        exclude("org.jetbrains.kotlin")
    }
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:${okHttpVersion}")
    androidTestImplementation("com.github.davidepianca98.KMQTT:kmqtt:0.2.9")

    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("com.squareup.leakcanary:leakcanary-android-instrumentation:2.8.1")

    androidTestUtil("androidx.test.services:test-services:1.4.1")
    androidTestUtil("androidx.test:orchestrator:1.4.1")
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