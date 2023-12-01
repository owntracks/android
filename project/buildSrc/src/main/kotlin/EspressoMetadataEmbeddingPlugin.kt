import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.AndroidComponentsExtension
import java.io.File
import java.util.Locale
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register

/**
 * A little gradle plugin that grabs screenshots and logcats of failed tests, and embeds them in the test report
 *
 */
class EspressoMetadataEmbeddingPlugin : Plugin<Project> {
    // This is where the androidx test files service puts saved bitmaps
    @Suppress("SdCardPath")
    private val screenshotsDeviceFolder = "/sdcard/googletest/test_outputfiles"

    private fun String.titleCase() = this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
    }

    override fun apply(project: Project) {
        val android: ApplicationExtension = project.extensions.getByType(ApplicationExtension::class.java)
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

        // This is where AGP writes out connected test reports
        val reportsDirectoryPath = "${project.layout.buildDirectory.get().asFile.path}/reports/androidTests/connected/debug/flavors/%s"
        val logcatDirectoryPath = "${project.layout.buildDirectory.get().asFile.path}/outputs/androidTest-results/connected/debug/flavors/%s"
        android.run {
            productFlavors.all {
                val flavorName = this.name
                val flavorTestReportPath = reportsDirectoryPath.format(flavorName)
                val flavouredLogcatDirectoryPath = logcatDirectoryPath.format(flavorName)
                project.run {
                    val adbExecutable = androidComponents.sdkComponents.adb.get().asFile.invariantSeparatorsPath

                    tasks.register<Exec>("clear${flavorName.titleCase()}Screenshots") {
                        group = "reporting"
                        description = "Removes $flavorName screenshots from connected device"
                        executable = adbExecutable
                        args(
                            "shell",
                            "rm",
                            "-rf",
                            screenshotsDeviceFolder
                        )
                    }

                    tasks.register<Exec>("fetch${flavorName.titleCase()}Screenshots") {
                        group = "reporting"
                        description = "Fetches $flavorName espresso screenshots from the device"
                        executable = adbExecutable
                        args(
                            "pull",
                            screenshotsDeviceFolder,
                            reportsDirectoryPath.format(flavorName)
                        )
                        doFirst {
                            File(flavorTestReportPath).mkdirs()
                        }
                    }

                    tasks.register<EmbedScreenshotsInTestReport>("embed${flavorName.titleCase()}Screenshots") {
                        group = "reporting"
                        description = "Embeds the $flavorName screenshots in the test report"
                        dependsOn("fetch${flavorName.titleCase()}Screenshots")
                        finalizedBy("clear${flavorName.titleCase()}Screenshots")
                        reportsPath = flavorTestReportPath
                        logcatPath = flavouredLogcatDirectoryPath
                    }
                }
            }
            if (!org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
                project.tasks.configureEach {
                    when (name) {
                        "connectedGmsDebugAndroidTest" -> finalizedBy("embedGmsScreenshots")
                        "connectedOssDebugAndroidTest" -> finalizedBy("embedOssScreenshots")
                    }
                }
            }
        }
    }
}
