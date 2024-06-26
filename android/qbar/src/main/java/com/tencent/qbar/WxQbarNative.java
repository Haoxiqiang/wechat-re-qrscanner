package com.tencent.qbar;

import android.graphics.Bitmap;

public class WxQbarNative {
    /* loaded from: classes10.dex */
    public static class QBarReportMsg {
        public int afterDetect;
        public int afterSeg;
        public String binaryMethod;
        public String charsetMode;
        public int decodeAll;
        public float decodeScale;
        public int decodeTime;
        public int detectInferAfterTime;
        public int detectInferPreTime;
        public int detectInferTime;
        public int detectTime;
        public String ecLevel;
        public boolean hasDecode;
        public boolean hasSeg;
        public boolean hasSr;
        public boolean inBlackList;
        public boolean inWhiteList;
        public int preDetect;
        public int pyramidLv;
        public int qrcodeVersion;
        public String scaleList;
        public int segCost;
        public int srTime;
    }

    public static native int EncodeCustom(byte[] bArr, int[] iArr, String str, int i, int i2, String str2, int i3, int i4, boolean z, Bitmap bitmap, Bitmap bitmap2, Bitmap bitmap3, Bitmap bitmap4, Bitmap bitmap5, Bitmap bitmap6);

    public static native int FocusInit(int i, int i2, boolean z, int i3, int i4);

    public static native boolean FocusPro(byte[] bArr, boolean z, boolean[] zArr);

    public static native int FocusRelease();

    public static native int GetDominantColors(Bitmap bitmap, int[] iArr);

    public static native int QIPUtilYUVCrop(byte[] bArr, byte[] bArr2, int i, int i2, int i3, int i4, int i5, int i6);

    public static native int TestGenQRCode(Bitmap bitmap, Bitmap bitmap2, Bitmap bitmap3, Bitmap bitmap4, Bitmap bitmap5, Bitmap bitmap6, Bitmap bitmap7);

    public static native int focusedEngineForBankcardInit(int i, int i2, int i3, boolean z);

    public static native int focusedEngineGetVersion();

    public static native int focusedEngineProcess(byte[] bArr);

    public static native int focusedEngineRelease();

    public native int AddBlackInternal(int i, int i2);

    public native int AddBlackList(String str, int i);

    public native int AddWhiteList(String str, int i);

    public native String GetCallSnapshot(int i);

    public native String GetDebugString(int i);

    public static native int GetDetailResults(QbarNative.QBarResultJNI[] qBarResultJNIArr, QbarNative.QBarPoint[] qBarPointArr, QBarReportMsg[] qBarReportMsgArr, int i);

    public static native int GetDetailResultsNew(QbarNative.QBarResultJNI[] qBarResultJNIArr, QbarNative.QBarPoint[] qBarPointArr, QBarReportMsg[] qBarReportMsgArr, int i);

    public native int GetDetectInfoByFrames(QbarNative.QBarCodeDetectInfo qBarCodeDetectInfo, QbarNative.QBarPoint qBarPoint, int i);

    public native int GetOneResultReport(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, int[] iArr, int[] iArr2, int i);

    public native int GetZoomInfo(QbarNative.QBarZoomInfo qBarZoomInfo, int i);

    public native void Reset(int i, boolean z);

    public native int ScanImage712(byte[] bArr, int i, int i2, int i3, boolean z);

    public native int SetCenterCoordinate(int i, int i2, int i3, int i4, int i5);

    public native int SetScanTryHarder(int i, int i2, int i3, float f);

    public native int SetTouchCoordinate(int i, float f, float f2);
}
