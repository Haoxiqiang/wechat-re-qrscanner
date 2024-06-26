base=/data/data/com.termux/files/QR
export CLASSPATH=$base/ScanQR.jar
export ANDROID_DATA=$base
mkdir -p $base/dalvik-cache

exec app_process $base Main "$@"
