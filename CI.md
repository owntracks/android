# CI pipelines / flow

OwnTracks Android uses [CircleCI](https://app.circleci.com/pipelines/github/owntracks/android) to run builds, unit tests, integration tests and publish jobs. Main reason is that espresso testing on an android emulator was the most stable / flexible on Circle's platform (as opposed to travis or GHA).

## CircleCI jobs

### Build release

Assembles the debug and release variants of both GMS and OSS flavours. Also compiles the test sources.

### Unit test

Runs the unit tests and generates a coverage report for codecov.io.

### F-Droid scan

Runs the [F-Droid server scanner](https://gitlab.com/fdroid/fdroidserver) against the OSS Release APK to make sure that no non-free components snuk into the release.

### Lint

Runs the Android lint and fails on new issues.

### UI tests

Runs the espresso tests. Can be parameterized over which flavour to run (OSS/GMS), parallelism, the Android API version of the emulator to use, and a flag to indicate that only tests annotated `@SmokeTest` should be run. Coverage report uploaded to codecov.io if the smoketest flag is not set

### Publish to Play Store

Uploads the release bundle of the GMS flavour to the Google Play Store internal testing track.

## Workflows

Three main workflows:

1. `build-integration-test-publish` runs the `build-release`, `fdoird-scanner`, `unit-test`, `lint`, `ui-tests` (matrixed across oss/gms flavours) and `publish-to-play-store` jobs. Triggered on every commit to `master` that changes the source.
2. `smoke-test` runs the `unit-test` and `ui-tests` jobs just on the `gms` flavour an flagging that only `@SmokeTest` tests should be run. Triggered when the `smoke-test-required` label is added to a PR.
3. `integration-test` runs the `unit-test` and `ui-tests` jobs on both `gms` and `oss` flavours across all test cases.

## Github Actions jobs

There's 3 GHA workflows that help manage PRs and releases.

### Android Release

Triggered by a version tag, this workflow creates a Github Release, fetches the APKs from the corresponding CircleCI workflow and attaches them to the release, and finally promotes the Play Store build between either internal and beta, beta and production or internal and production (depending on whether it's a beta tag or not)

### Auto-Label PRs

Labels new PRs with the approprate test required label depending on what's changed.

### Test PRs

Triggered by a PR being labelled, this workflow triggeres a CircleCI build depending on whether the label was `smoke-test-required` or `intergration-test-required`.

## Forked PRs

Forked PRs can't trigger workflows on CircleCI because that requires an API key stored in a Github secret. Github secrets aren't exposed to `pull_request` flows running on forked branches, so the workaround is to review the forked PR and then push those to a separate branch on the main repo using the `git-push-fork-to-upstream-branch` script. This can then trigger a CI test run on CircleCI depending on the branch pushed to. Commits pushed to `trigger-smoke-test` will trigger a smoketest run, and `trigger-integration-test` will trigger a full integration test. On completion, Circle should post the result status to GH for that commit, which should update the status on the forked PR.
