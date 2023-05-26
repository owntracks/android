OwnTracks for Android
=======

This is the OwnTracks Android app. See our [booklet](http://owntracks.org/booklet/features/android/) for details on how to get started with OwnTracks, as well some details about behaviour specific to the Android app.

## Build flavours

There are two build flavours for OwnTracks:

* `gms`: This is the build published to the Google Play Store. It links to and requires the Google Play Services libraries for location (using the Google location APIs), as well as the Google Maps SDK for drawing the main map.
* `oss`: This is an "un-Googled" build, which does not require or depend on Google Play Services. It uses the built-in android location capabilities and [OpenStreetMap](https://www.openstreetmap.org/) for the main map.

Both flavours are published as an APK to Github releases.
The `gms` flavour is distributed via the [Google Play Store](https://play.google.com/store/apps/details?id=org.owntracks.android).
The `oss` flavour is distributed via [F-Droid](https://f-droid.org/packages/org.owntracks.android/). *N.B. this is currently unavailable until we remove a specific closed-source dependency. See [#1288](https://github.com/owntracks/android/issues/1298)*

## Contributing

Pull requests welcome! Please see [CONTRIBUTING.md](https://github.com/owntracks/android/blob/master/CONTRIBUTING.md) for details on how to build the project locally.

If you spot a translation issue or want to help contribute translating the app into other languages, you can visit [POEditor](https://poeditor.com/projects/view?id=419041) and help out.

[![CircleCI](https://circleci.com/gh/owntracks/android/tree/master.svg?style=shield)](https://circleci.com/gh/owntracks/android/tree/master)
