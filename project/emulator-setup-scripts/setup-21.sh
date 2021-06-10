adb emu geo fix 0 51
sleep 10
adb shell settings put secure location_providers_allowed +gps
sleep 10
adb shell settings put secure location_providers_allowed +network
sleep 10
adb shell input keyevent 61
sleep 10
adb shell input keyevent 61
sleep 10
adb shell input keyevent 66
sleep 10
adb shell input keyevent 61
sleep 10
adb shell input keyevent 66
sleep 10
adb shell settings put secure location_mode 3
sleep 10
