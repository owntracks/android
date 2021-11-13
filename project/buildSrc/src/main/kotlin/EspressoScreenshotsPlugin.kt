import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import java.io.File

class EspressoScreenshotsPlugin : Plugin<Project> {
    // This is where the androidx test files service puts saved bitmaps
    @Suppress("SdCardPath")
    private val screenshotsDeviceFolder = "/sdcard/googletest/test_outputfiles"

    override fun apply(project: Project) {
        // This is where AGP writes out connected test reports
        val reportsDirectoryPath = "${project.buildDir}/reports/androidTests/connected/flavors/%s"
        val android: AppExtension? = project.extensions.findByType(AppExtension::class.java)
        android?.run {
            productFlavors.all {
                val flavorName = this.name
                val flavorTestReportPath = reportsDirectoryPath.format(flavorName)
                project.run {
                    val adbExecutable = android.adbExecutable.absolutePath
                    tasks.register<Exec>("clear${flavorName.capitalize()}Screenshots") {
                        group = "reporting"
                        description =
                            "Removes $flavorName screenshots from connected device"
                        executable = adbExecutable
                        args(
                            "shell", "rm", "-rf", screenshotsDeviceFolder
                        )
                    }

                    tasks.register<Exec>("fetch${flavorName.capitalize()}Screenshots") {
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

                    tasks.register<EmbedScreenshotsInTestReport>("embed${flavorName.capitalize()}Screenshots") {
                        group = "reporting"
                        description = "Embeds the $flavorName screenshots in the test report"
                        dependsOn("fetch${flavorName.capitalize()}Screenshots")
                        finalizedBy("clear${flavorName.capitalize()}Screenshots")
                        reportsPath = flavorTestReportPath
                    }
                }
            }
            project.tasks.whenTaskAdded {
                when (name) {
                    "connectedGmsDebugAndroidTest" -> finalizedBy("embedGmsScreenshots")
                    "connectedOssDebugAndroidTest" -> finalizedBy("embedOssScreenshots")
                }
            }
        }
    }
}