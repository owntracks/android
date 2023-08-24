OwnTracks for Android
=======

This is the OwnTracks Android app. See our [booklet](http://owntracks.org/booklet/features/android/) for details on how to get started with OwnTracks, as well some details about behaviour specific to the Android app.

## Build flavours

There are two build flavours for OwnTracks:

* `gms`: This is the build published to the Google Play Store. It links to and requires the Google Play Services libraries for location (using the Google location APIs), as well as the Google Maps SDK for drawing the main map.
* `oss`: This is an "un-Googled" build, which does not require or depend on Google Play Services. It uses the built-in android location capabilities and [OpenStreetMap](https://www.openstreetmap.org/) for the main map.

Both flavours are published as an APK to Github releases.
The `gms` flavour is distributed via the [Google Play Store](https://play.google.com/store/apps/details?id=org.owntracks.android).
The `oss` flavour is distributed via [F-Droid](https://f-droid.org/packages/org.owntracks.android/).

### Signing keys

* Google Play store-distributed builds are signed with Google's App signing key: `02:FD:16:4A:95:46:17:F0:B7:94:57:97:37:C9:7A:07:B8:31:83:1D:0A:05:90:C3:8D:07:2B:FE:29:01:08:F1`
* APKs attached to Github Releases are signed with our own key: `1F:C4:DE:52:D0:DA:A3:3A:9C:0E:3D:67:21:7A:77:C8:95:B4:62:66:EF:02:0F:AD:0D:48:21:6A:6A:D6:CB:70`
* F-Droid builds are signed with their own key, details at <https://f-droid.org/en/docs/Release_Channels_and_Signing_Keys/>

## Contributing

Pull requests welcome! Please see [CONTRIBUTING.md](https://github.com/owntracks/android/blob/master/CONTRIBUTING.md) for details on how to build the project locally.

If you spot a translation issue or want to help contribute translating the app into other languages, you can visit [POEditor](https://poeditor.com/projects/view?id=419041) and help out.

[![CircleCI](https://circleci.com/gh/owntracks/android/tree/master.svg?style=shield)](https://circleci.com/gh/owntracks/android/tree/master)
