import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

// From  https://github.com/Triple-T/gradle-play-publisher/issues/974
// We need a way to get the *current* release code on GP, so that we can make an APK with the same
// code for the GH release. GPP has a way of getting the *next* release, so we can get that and
// subtract one

abstract class GetLatestVersionCodeMinusOne : DefaultTask() {
    @get:InputFile
    abstract val codes: RegularFileProperty

    @get:OutputFile
    abstract val outCode: RegularFileProperty

    @TaskAction
    fun read() {
        val code = codes.get().asFile.readLines().first().toInt() - 1
        outCode.get().asFile.writeText(code.toString())
    }
}
