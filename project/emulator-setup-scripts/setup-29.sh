adb emu geo fix 0 51
adb shell settings put secure location_providers_allowed +gps
adb shell settings put secure location_providers_allowed +network
adb shell settings put secure location_mode 3
