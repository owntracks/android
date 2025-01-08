set dotenv-load := true
gradlec := "./project/gradlew -p project"

default:
    @just -l

build:
    {{gradlec}} assembleDebug

unit-test:
    {{gradlec}} app:testGmsDebugUnitTest

espresso:
    {{gradlec}} app:createGmsDebugCoverageReport

single-espresso:
    {{gradlec}} clean createGmsDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=org.owntracks.android.testutils.JustThisTestPlease

format:
    {{gradlec}} app:ktfmtFormat

clean:
    {{gradlec}} clean

tasks:
    {{gradlec}} tasks --all

sync-i18n:
    ./util/pull-translations.sh

local-stack:
    cd util/mqtt-local && podman-compose up
