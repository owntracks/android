import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class EmbedScreenshotsInTestReport : DefaultTask() {
    @Input
    lateinit var reportsPath: String

    @TaskAction
    fun exec() {
        val screenShotSrcRelativeDir = "test_outputfiles"
        val screenshotsDirectory = File(reportsPath, screenShotSrcRelativeDir)
        if (!screenshotsDirectory.exists()) {
            println("Could not find screenshots in ${screenshotsDirectory.absolutePath}. Skipping...")
            return
        }
        screenshotsDirectory
            .listFiles()!!
            .forEach { testClassDirectory ->
                val testClassName = testClassDirectory.name
                testClassDirectory.listFiles()?.forEach failedFile@{
                    val testName = it.name
                    val testNameWithoutExtension = it.nameWithoutExtension
                    val testClassJunitReportFile = File(reportsPath, "$testClassName.html")
                    if (!testClassJunitReportFile.exists()) {
                        println("Could not find JUnit report file for test class '$testClassJunitReportFile'")
                        return@failedFile
                    }
                    val testJunitReportContent =
                        testClassJunitReportFile.readText()

                    val failedHeaderPatternToFind =
                        "<h3 class=\"failures\">$testNameWithoutExtension</h3>"

                    val failedPatternToReplace =
                        "$failedHeaderPatternToFind <img src=\"$screenShotSrcRelativeDir/$testClassName/${testName}\" width =\"360\" />"
                    val successRecordPatternToFind =
                        "<td>$testNameWithoutExtension</td>"
                    val successPatternToReplace =
                        "<td>$testNameWithoutExtension <a href=\"$screenShotSrcRelativeDir/$testClassName/${testName}\">(screenshot)</a></td>"

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
