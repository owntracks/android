set dotenv-load := true
gradlec := "./project/gradlew -p project --scan"

default:
    @just --list

build:
    {{gradlec}} assembleDebug app:assembleAndroidTest app:assembleGmsDebugUnitTest assembleRelease

unit-test:
    {{gradlec}} app:testGmsDebugUnitTest

espresso:
    {{gradlec}} app:createGmsDebugCoverageReport

single-espresso:
    {{gradlec}} clean createGmsDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=org.owntracks.android.testutils.JustThisTestPlease

format:
    {{gradlec}} app:ktfmtFormat

tasks:
    {{gradlec}} tasks --all

sync-i18n:
    ./util/pull-translations.sh

clean:
    {{gradlec}} clean
    rm -rf project/app/build
    rm -rf project/.gradle/
    rm -rf project/build/

update-all-prs:
     gh pr list --json number|jq -r .[].number|xargs -I{} gh pr update-branch {}

local-stack:
    cd util/mqtt-local && podman-compose up

mqtt-subscribe:
    mosquitto_sub -L mqtt://localhost/owntracks/# -u test -P test

wipe-device:
    adb uninstall org.owntracks.android; adb uninstall org.owntracks.android.debug; adb uninstall androidx.test.orchestrator ; adb uninstall androidx.test.services; adb uninstall androidx.test.tools.crawler; adb uninstall androidx.test.tools.crawler.stubapp; echo "done"
