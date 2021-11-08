import com.android.build.gradle.AppExtension
import org.apache.tools.ant.taskdefs.ExecTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import java.io.File

class EspressoScreenshotsPlugin : Plugin<Project> {
    private val screenshotsDeviceFolder = "/sdcard/Download/testscreenshots"

    override fun apply(project: Project) {
        val reportsDirectoryPath = "${project.buildDir}/reports/androidTests/connected/flavors/%s"

        val android: AppExtension? = project.extensions.findByType(AppExtension::class.java)

        android?.run {
            productFlavors.all {
                val flavorName = this.name.capitalize()
                project.run {
                    tasks.register<Exec>("create${flavorName}ScreenshotDirectory") {
                        group = "reporting"
                        description =
                            "Creates $flavorName screenshot directory on connected device"
                        executable = "${android.adbExecutable}"
                        args(mutableListOf("shell", "mkdir", "-p", screenshotsDeviceFolder))
                    }
                    tasks.register<Exec>("clear${flavorName}Screenshots") {
                        group = "reporting"
                        description =
                            "Removes ${flavorName} screenshots from connected device"
                        executable = "${android.adbExecutable}"
                        args("shell", "rm", "-rf", screenshotsDeviceFolder)
                    }

                    tasks.register<Exec>("fetch${flavorName}Screenshots") {
                        group = "reporting"
                        description =
                            "Fetches ${flavorName} espresso screenshots from the device"
                        executable = "${android.adbExecutable}"
                        args(
                            "pull",
                            screenshotsDeviceFolder,
                            reportsDirectoryPath.format(flavorName)
                        )
                        dependsOn("create${flavorName}ScreenshotDirectory")
                        doFirst {
                            File(reportsDirectoryPath.format(flavorName)).mkdirs()
                        }
                    }

                    tasks.register("embed${flavorName}Screenshots") {
                        group = "reporting"
                        description =
                            "Embeds the ${flavorName} screenshots in the test report"
                        dependsOn("fetch${flavorName}Screenshots")
                        finalizedBy("clear${flavorName}Screenshots")
                        doFirst {
                            val reportsPath = reportsDirectoryPath.format(flavorName)
                            val screenshotsDirectory = File(reportsPath, "testscreenshots/")
                            if (!screenshotsDirectory.exists()) {
                                println("Could not find screenshots. Skipping...")
                                return@doFirst
                            }
                            screenshotsDirectory
                                .listFiles()!!
                                .forEach { testClassDirectory ->
                                    val testClassName = testClassDirectory.name
                                    testClassDirectory.listFiles()?.forEach failedFile@{
                                        val testName = it.name
                                        val testNameWithoutExtension = it.nameWithoutExtension
                                        val testClassJunitReportFile =
                                            File(reportsPath, "${testClassName}.html")
                                        if (!testClassJunitReportFile.exists()) {
                                            println("Could not find JUnit report file for test class '${testClassJunitReportFile}'")
                                            return@failedFile
                                        }
                                        val testJunitReportContent =
                                            testClassJunitReportFile.readText()

                                        val failedHeaderPatternToFind =
                                            "<h3 class=\"failures\">${testNameWithoutExtension}</h3>"

                                        val failedPatternToReplace =
                                            "$failedHeaderPatternToFind <img src=\"testscreenshots/${testClassName}/${testName}\" width =\"360\" />"
                                        val successRecordPatternToFind =
                                            "<td>${testNameWithoutExtension}</td>"
                                        val successPatternToReplace =
                                            "<td>${testNameWithoutExtension} <a href=\"testscreenshots/${testClassName}/${testName}\">(screenshot)</a></td>"

                                        testClassJunitReportFile.writeText(
                                            testJunitReportContent
                                                .replace(
                                                    failedHeaderPatternToFind,
                                                    failedPatternToReplace
                                                )
                                                .replace(
                                                    successRecordPatternToFind,
                                                    successPatternToReplace
                                                )
                                        )
                                    }
                                }
                        }
                    }
                }
            }
            project.tasks.whenTaskAdded {
                when(name) {
                    "connectedGmsDebugAndroidTest" -> finalizedBy("embedGmsScreenshots")
                    "connectedOssDebugAndroidTest" -> finalizedBy("embedOssScreenshots")
                }
            }

        }
    }
}

abstract class CreateScreenshotDirectory : ExecTask() {

}

abstract class ClearScreenshots : ExecTask() {
    override fun init() {
        super.init()

    }
}

abstract class FetchScreenshots : ExecTask() {}

abstract class EmbedScreenshots : ExecTask() {}