set dotenv-load := true
set positional-arguments := true
gradlec := "./project/gradlew -p project"

default:
    @just --list

[group('build')]
gradle *args='':
    {{gradlec}} $@

[group('build')]
build:
    {{gradlec}} assembleDebug

[group('build')]
build-all:
    {{gradlec}} assembleDebug app:assembleAndroidTest app:assembleGmsDebugUnitTest assembleRelease

[group('build')]
tasks:
    {{gradlec}} tasks --all

[group('build')]
clean:
    {{gradlec}} clean
    rm -rf project/app/build
    rm -rf project/.gradle/
    rm -rf project/build/

[group('testing')]
unit-test:
    {{gradlec}} app:testGmsDebugUnitTest

[group('testing')]
espresso:
    {{gradlec}} app:createGmsDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=*

[group('testing')]
small-espresso:
    {{gradlec}} clean createGmsDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=androidx.test.filters.SmallTest

[group('testing')]
single-espresso:
    {{gradlec}} clean createGmsDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=org.owntracks.android.testutils.JustThisTestPlease

[group('testing')]
managed-device-test:
    {{gradlec}} app:phoneAtdGmsDebugAndroidTest

[group('formatting')]
ktfmt:
    {{gradlec}} ktfmtFormat

[group('formatting')]
ktfmt-check:
    {{gradlec}} ktfmtCheck

[group('utilities')]
sync-i18n:
    ./util/pull-translations.sh

[group('utilities')]
update-all-prs:
     gh pr list --json number|jq -r .[].number|xargs -I{} gh pr update-branch {}

[group('local infrastructure')]
local-stack:
    cd util/mqtt-local && podman-compose up

[group('local infrastructure')]
mqtt-subscribe:
    mosquitto_sub -v -L mqtt://localhost/owntracks/# -u test -P test

[group('device')]
install: (gradle "app:assembleGmsDebug")
    adb install -r project/app/build/outputs/apk/gms/debug/app-gms-debug.apk

[group('device')]
wipe-device:
    adb uninstall org.owntracks.android; adb uninstall org.owntracks.android.debug; adb uninstall androidx.test.orchestrator ; adb uninstall androidx.test.services; adb uninstall androidx.test.tools.crawler; adb uninstall androidx.test.tools.crawler.stubapp; echo "done"
