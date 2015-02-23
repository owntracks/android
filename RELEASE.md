Steps to create an official release
> mv apk/android-release.apk apk/owntracks.apk
> git commit -am "Android-v.0.X.XX APK"
> git tag -a Android-v.0.X.XX -m "TAG Android-v.0.X.XX"
> git push origin master && git push --tags
> Create release with APK
> Upload to Play Store
