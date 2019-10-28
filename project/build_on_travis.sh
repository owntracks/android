
set -ev

./gradlew assembleRelease
jarsigner -verbose -sigalg SHA1withRSA -storepass $keystorepass -keypass $keystorepass -digestalg SHA1 -keystore ../keystore.jks ./app/build/outputs/apk/release/app-release-unsigned.apk owntracks
/usr/local/android-sdk/build-tools/29.0.2/zipalign -v 4 ./app/build/outputs/apk/release/app-release-unsigned.apk ./app/build/outputs/apk/release/app-release.apk
