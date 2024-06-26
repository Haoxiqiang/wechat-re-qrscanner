#!/bin/sh

EXEC_DIR=QR

adb root

adb push ./ScanQR.jar /data/data/com.termux/files/home/$EXEC_DIR
adb push ./ScanQR.sh /data/data/com.termux/files/home/$EXEC_DIR
adb shell chmod a+x /data/data/com.termux/files/home/$EXEC_DIR/ScanQR.sh
adb shell ./ScanQR/qbar-lib /data/data/com.termux/files/home/$EXEC_DIR/
