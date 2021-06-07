adb emu geo fix 0 51
sleep 10
adb shell screencap /sdcard/Download/out1.png
adb shell settings put secure location_providers_allowed +gps
sleep 10
adb shell screencap /sdcard/Download/out2.png
adb shell settings put secure location_providers_allowed +network
sleep 10
adb shell screencap /sdcard/Download/out3.png
adb shell input keyevent 61
sleep 10
adb shell screencap /sdcard/Download/out4.png
adb shell input keyevent 61
sleep 10
adb shell screencap /sdcard/Download/out5.png
adb shell input keyevent 66
sleep 10
adb shell screencap /sdcard/Download/out6.png
adb shell input keyevent 61
sleep 10
adb shell screencap /sdcard/Download/out7.png
adb shell input keyevent 66
sleep 10
adb shell screencap /sdcard/Download/out8.png
adb shell settings put secure location_mode 3
sleep 10
adb shell screencap /sdcard/Download/out9.png
mkdir tempscreenshots
adb pull /sdcard/Download
mv Download/out*.png tempscreenshots