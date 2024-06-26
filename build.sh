#!/bin/sh

TEMP_DIR=ScanQR
OUT_DIR=bin
javac -d $OUT_DIR $TEMP_DIR/src/Main.java $TEMP_DIR/src/com/tencent/qbar/QbarNative.java $TEMP_DIR/src/android/graphics/* $TEMP_DIR/src/android/util/*
d8 --release $OUT_DIR/Main.class $OUT_DIR/Main\$ScanArea.class $OUT_DIR/com/tencent/qbar/*  --output ScanQR.jar