fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android promote_internal_to_beta

```sh
[bundle exec] fastlane android promote_internal_to_beta
```

Promote internal track to beta

### android promote_beta_to_production

```sh
[bundle exec] fastlane android promote_beta_to_production
```

Promote beta to production with staged rollout

### android promote_internal_to_production

```sh
[bundle exec] fastlane android promote_internal_to_production
```

Promote internal to production with staged rollout

### android upload_to_internal

```sh
[bundle exec] fastlane android upload_to_internal
```

Upload APK/AAB to internal track

### android get_version_code

```sh
[bundle exec] fastlane android get_version_code
```

Get version code from a track

### android update_metadata

```sh
[bundle exec] fastlane android update_metadata
```

Update metadata only

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
