# CI pipelines / flow

OwnTracks Android uses Github Actions to build, test and release.

## jobs

### Build, test & lint

Assembles the debug and release variants of both GMS and OSS flavours. Also compiles the test sources, then runs the unit tests (with coverage) and the Android lint.

### F-Droid scan

Runs the [F-Droid server scanner](https://gitlab.com/fdroid/fdroidserver) against the OSS Release APK to make sure that no non-free components snuk into the release.

### UI tests

Runs the espresso tests. Can be parameterized over which flavour to run (OSS/GMS), parallelism, the Android API version of the emulator to use. Uploads coverage to [CodeCov](https://app.codecov.io/gh/owntracks/android)

### Publish to Play Store

Uploads the release bundle of the GMS flavour to the Google Play Store internal testing track.

## Workflows

Three main workflows:

1. `Build and Test` runs the `build-test-lint`, `fdroid-scanner`, `espresso-test` (matrixed across oss/gms flavours) and `publish-to-play-store` jobs. Triggered on every commit to `master` that changes the source.
2. `Android Release` pulls the appropriate secrets and then assembles a `release` variant of the `gms` flavour, before uploading to the right Google Play Store track.
3. `Close stale issues and PRs` closes open issues that are waiting a response and haven't received one in a while.

## Github Actions jobs

There's 3 GHA workflows that help manage PRs and releases.

### Android Release

Triggered by a version tag, this workflow creates a Github Release, fetches the APKs from the corresponding CircleCI workflow and attaches them to the release, and finally promotes the Play Store build between either internal and beta, beta and production or internal and production (depending on whether it's a beta tag or not).
