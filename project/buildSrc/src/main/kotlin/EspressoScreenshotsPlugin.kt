import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import java.io.File
import java.util.*

class EspressoScreenshotsPlugin : Plugin<Project> {
    // This is where the androidx test files service puts saved bitmaps
    @Suppress("SdCardPath")
    private val screenshotsDeviceFolder = "/sdcard/googletest/test_outputfiles"

    override fun apply(project: Project) {
        val android: ApplicationExtension =
            project.extensions.getByType(ApplicationExtension::class.java)
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

        // This is where AGP writes out connected test reports
        val reportsDirectoryPath = "${project.buildDir}/reports/androidTests/connected/flavors/%s"
        android.run {
            productFlavors.all {
                val flavorName = this.name
                val flavorTestReportPath = reportsDirectoryPath.format(flavorName)
                project.run {

                    val adbExecutable =
                        androidComponents.sdkComponents.adb.get().asFile.invariantSeparatorsPath

                    tasks.register<Exec>("clear${flavorName.capitalize(Locale.ROOT)}Screenshots") {
                        group = "reporting"
                        description =
                            "Removes $flavorName screenshots from connected device"
                        executable = adbExecutable
                        args(
                            "shell", "rm", "-rf", screenshotsDeviceFolder
                        )
                    }

                    tasks.register<Exec>("fetch${flavorName.capitalize(Locale.ROOT)}Screenshots") {
                        group = "reporting"
                        description = "Fetches $flavorName espresso screenshots from the device"
                        executable = adbExecutable
                        args(
                            "pull", screenshotsDeviceFolder, reportsDirectoryPath.format(flavorName)
                        )
                        doFirst {
                            File(flavorTestReportPath).mkdirs()
                        }
                    }

                    tasks.register<EmbedScreenshotsInTestReport>(
                        "embed${
                            flavorName.capitalize(
                                Locale.ROOT
                            )
                        }Screenshots"
                    ) {
                        group = "reporting"
                        description = "Embeds the $flavorName screenshots in the test report"
                        dependsOn("fetch${flavorName.capitalize(Locale.ROOT)}Screenshots")
                        finalizedBy("clear${flavorName.capitalize(Locale.ROOT)}Screenshots")
                        reportsPath = flavorTestReportPath
                    }
                }
            }
            if (!org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
                project.tasks.whenTaskAdded {
                    when (name) {
                        "connectedGmsDebugAndroidTest" -> finalizedBy("embedGmsScreenshots")
                        "connectedOssDebugAndroidTest" -> finalizedBy("embedOssScreenshots")
                    }
                }
            }
        }
    }
}