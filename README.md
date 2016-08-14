OwnTracks for Android
=======

This is the OwnTracks Android app. 
For setup instructions and documentation see general OwnTracks [wiki](https://github.com/owntracks/owntracks/wiki/Android).

The application can be installed from [Google Play](https://play.google.com/store/apps/details?id=org.owntracks.android). 

## Contribute 
To build a development version of the app from source, follow the instructions outlined below. 

1. Download and install [Android Studio](http://developer.android.com/sdk/index.html)
2. Clone the project or a fork of it to your local development machine
3. Import the project into Android Studio
4. Sync Project with Gradle files to download all the dependencies
5. Open the SDK manager to install the required Android SDK Tools and Android SDK Build-tools
6. The project contains a preconfigured keystore to sign debug builds. If you already have a custom keystore you can specify it in project/app/build.gradle android.signingConfigs.debug
8. Required: Create a custom Google Maps API Key
    The provided Google Maps API Key is tied to our signing key, so it cannot be used with the debug keystore
	1. Go go to https://developers.google.com/maps/documentation/android/start
	2. Scroll down to Obtain a Google Maps API key and follow the instructions
	3. If you use the preconfigured debug keystore, enter ```BC:CF:16:C8:4B:5E:5D:2D:DA:B7:35:FF:2A:53:CF:89:83:C2:D9:65;org.owntracks.android.debug```
	4. If you use a custom keystore run ```keytool -list -v -keystore $PATH_TO_YOUR_KEYSTORE.jks``` and replace BC:CF:...:D9:65 with your SHA1 fingerprint
	5. Create the directory ```project/app/src/main/res/values-v1/apis.xml```
	6. Copy ```project/app/src/main/res/values/apis.xml``` to ```project/app/src/main/res/values-v1/apis.xml```
	7. Replace the API key in ```project/app/src/main/res/values-v1/apis.xml``` with the generated API key

10. Build the project
