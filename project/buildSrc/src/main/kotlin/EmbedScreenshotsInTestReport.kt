import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.walk

abstract class EmbedScreenshotsInTestReport : DefaultTask() {
    @Input
    lateinit var reportsPath: String

    @Input
    lateinit var logcatPath: String

    @OptIn(ExperimentalPathApi::class)
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

                    val logcat = Path(logcatPath).walk().firstOrNull {file -> file.name=="logcat-$testClassName-$testNameWithoutExtension.txt" }?.readText() ?: "(No logcat found)"

                    val failedHeaderPatternToFind =
                        "<h3 class=\"failures\">$testNameWithoutExtension</h3>"

                    val failedPatternToReplace =
                        """$failedHeaderPatternToFind <img src=\"$screenShotSrcRelativeDir/$testClassName/${testName}\" width =\"360\" />
                            <details>
                            <summary>Logcat</summary>
                            <span class=\"code\">
                            <pre>$logcat</pre>
                            </span>
                            </details>""".trimMargin()
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
