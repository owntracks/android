set dotenv-load := true
gradlec := "./project/gradlew -p project"

default:
    @just --list

build:
    {{gradlec}} assembleDebug assembleAndroidTest

test:
    {{gradlec}} app:testGmsDebugUnitTest

espresso:
    {{gradlec}} app:createGmsDebugCoverageReport

tasks:
    {{gradlec}} tasks --all

sync-i18n:
    ./util/pull-translations.sh

clean:
    {{gradlec}} clean
    rm -rf project/app/build
    rm -rf project/.gradle/
    rm -rf project/build/
