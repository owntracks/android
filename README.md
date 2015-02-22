OwnTracks for Android
=======

This is the OwnTracks Android app. 
For setup instructions and documentation see general Owntracks [wiki](https://github.com/owntracks/owntracks/wiki/Android).

The application can be installed from [Google Play](https://play.google.com/store/apps/details?id=org.owntracks.android). 

## Contribute 
To build a development version of the app from source, follow the instructions outlined below. 

1. Download and install [Android Studio](http://developer.android.com/sdk/index.html)
2. Clone the project or a fork of it to your local development machine
3. Import the project into Android Studio
4. Click on Sync Project with Gradle files to download all the dependencies
5. Open the SDK manager to install the required Android SDK Tools and Android SDK Build-tools
6. The project contains a preconfigured keystore to sign debug builds. If you already have a custom keystore you can specify it in build.gradle android.signingConfigs.debug
7. Copy the file ```src/main/res/values/keys.xml``` to ```src/main/res/values-1/keys.xml```. If the ```src/main/res/values-1``` directory does not exist, create it. 
8. Required: Create a custom Google Maps API Key
	1. Go go to https://developers.google.com/maps/documentation/android/start
	2. Scroll down to Obtain a Google Maps API key and follow the instructions
	3. If you use the preconfigured debug keystore, enter ```BC:CF:16:C8:4B:5E:5D:2D:DA:B7:35:FF:2A:53:CF:89:83:C2:D9:65;debug.org.owntracks.android```
	4. If you use a custom keystore run ```keytool -list -v -keystore $PATH_TO_YOUR_KEYSTORE.jks``` and replace BC:CF:...:D9:65 with your SHA1 fingerprint
	5. Replace ```YOUR_GOOGLE_MAPS_API_KEY``` in ```src/main/res/values-1/keys.xml``` with the generated forty-character API key

9. Optional: If you need crash reports, register at [Bugsnag.com](https://bugsnag.com), create a new Android project and replace ```YOUR_BUGSNAG_API_KEY``` in ```src/main/res/values-1/keys.xml``` with your API key 
10. Build the project
