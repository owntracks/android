## Building
To build a development version of the app from source, follow the instructions outlined below.

1. Download and install [Android Studio](http://developer.android.com/sdk/index.html)
2. Clone the project or a fork of it to your local development machine
3. Import the `project` subfolder into Android Studio
4. Sync Project with Gradle files to download all the dependencies
5. Open the SDK manager to install the required Android SDK Tools and Android SDK Build-tools
6. To get the Google Maps functionality working, you'll need a Google Maps API Key. Builds will work without it, but you won't see any map data on the main activity, and you will also see an exception logged on startup. To set the API key:
	1. Go go to https://developers.google.com/maps/documentation/androidA/start
	2. Scroll down to Obtain a Google Maps API key and follow the instructions (currently it's "Step 4. Set up a Google Maps API key")
	3. If you want to restrict your API key to the android app, you'll need to provide the fingerprint. This is likely to be ```BC:CF:16:C8:4B:5E:5D:2D:DA:B7:35:FF:2A:53:CF:89:83:C2:D9:65;org.owntracks.android.debug```, but if in doubt you can check the exception given on startup in the logs. This should contain the fingerprint of your current build.
	4. Create or edit the local gradle.properties file. It might be located in `~/.gradle/gradle.properties`
	5. Add `google_maps_api_key=$YOUR_OBTAINED_API_KEY` and save the file
7. Build the project
