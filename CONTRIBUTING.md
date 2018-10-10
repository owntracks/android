## Building 
To build a development version of the app from source, follow the instructions outlined below. 

1. Download and install [Android Studio](http://developer.android.com/sdk/index.html)
2. Clone the project or a fork of it to your local development machine
3. Import the `project` subfolder into Android Studio
4. Sync Project with Gradle files to download all the dependencies
5. Open the SDK manager to install the required Android SDK Tools and Android SDK Build-tools
6. The project contains a preconfigured keystore to sign debug builds. 
8. Required: Create a custom Google Maps API Key
    Provide a custom Google Maps API key for a build signed by the preconfigured debug keystore
	1. Go go to https://developers.google.com/maps/documentation/android/start
	2. Scroll down to Obtain a Google Maps API key and follow the instructions
	3. If you use the preconfigured debug keystore, use the following fingerprint ```BC:CF:16:C8:4B:5E:5D:2D:DA:B7:35:FF:2A:53:CF:89:83:C2:D9:65;org.owntracks.android.debug```
	4. If you use a custom keystore run ```keytool -list -v -keystore $PATH_TO_YOUR_KEYSTORE.jks``` and replace BC:CF:...:D9:65 with your SHA1 fingerprint
	5. Create or edit the local gradle.properties file. It might be located in `~/.gradle/gradle.properties`
	6. Add `google_maps_api_key=$YOUR_OBTAINED_API_KEY` and save the file
9. Build the project

