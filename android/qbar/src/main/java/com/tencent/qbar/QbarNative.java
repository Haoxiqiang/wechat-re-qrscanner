package com.tencent.qbar;

import android.graphics.Bitmap;

public class QbarNative {

    public static class QBarCodeDetectInfo {
        public float prob;
        public int readerId;
    }

    public static class QBarPoint {
        public int point_cnt;
        public float x0;
        public float x1;
        public float x2;
        public float x3;
        public float y0;
        public float y1;
        public float y2;
        public float y3;
    }

    public static class QBarZoomInfo {
        public boolean isZoom;
        public float zoomFactor;
    }

    public static class QBarReportMsg {
        public int qrcodeVersion;
    }

    /**
     * 扫一扫模块初始化参数
     * 参数暂时不明确含义
     */
    public static class QbarAiModelParam {
        public String detectModelVersion;
        public String detect_model_bin_path_;
        public String detect_model_param_path_;
        public boolean enable_seg = false;
        public String qbar_segmentation_model_path_;
        public String superResolutionModelVersion;
        public String superresolution_model_bin_path_;
        public String superresolution_model_param_path_;
    }

    /**
     * 扫一扫返回数组
     */
    public static class QBarResultJNI {

        /**
         * 字符集合
         *
         * @see java.nio.charset.Charset
         */
        public String charset = "";

        /**
         * 字符集合的内容，常见格式：UTF-8、ASCII、ISO8859-1
         * 一般使用 new String(data, charset) 可解出数据
         *
         * @see String
         * @see java.nio.charset.Charset
         */
        public byte[] data = new byte[1024];

        /**
         * 未知含义
         */
        public int typeID = 0;

        /**
         * 当前扫出来二维码类型
         * 一般有：CODE_25、CODE_39、CODE_128、QR_CODE、
         */
        public String typeName = "";
    }

    public static native int Encode(byte[] bArr, int[] iArr, String str, int i, int i2, String str2, int i3);

    public static native int EncodeBitmap(String str, Bitmap bitmap, int i, int i2, int i3, int i4, String str2, int i5);

    public static native String GetVersion();

    public static native int nativeArrayConvert(int i, int i2, byte[] bArr, int[] iArr);

    /**
     * 对 onPreviewFrame 输出对数据进行 灰度、旋转、裁剪 一条龙服务
     * 经过测试通过的图片格式 android.graphics.ImageFormat.NV21
     *
     * @param onPreviewFrameData       相机预览数据
     * @param onPreviewFrameDataWeight 相机预览数据宽度
     * @param onPreviewFrameDataHeight 相机预览数据高度
     * @param cropLeft                 裁剪左坐标
     * @param cropTop                  裁剪上坐标
     * @param cropWidth                裁剪宽度
     * @param cropHeight               裁剪高度
     * @param outputData               输出数据
     * @param outputDataWH             输出数据宽，高
     *                                 width = 0
     *                                 height = 1
     * @param rotation                 旋转角度
     * @param unknown                  始终为0 不明含义
     * @return 非0失败
     */
    public static native int nativeGrayRotateCropSub(
            byte[] onPreviewFrameData,
            int onPreviewFrameDataWeight,
            int onPreviewFrameDataHeight,
            int cropLeft,
            int cropTop,
            int cropWidth,
            int cropHeight,
            byte[] outputData,
            int[] outputDataWH,
            int rotation,
            int unknown
    );

    public static native int nativeTransBytes(int[] prepareGrayData, byte[] outputData, int sizeX, int sizeY);

    public static native int nativeTransPixels(int[] iArr, byte[] bArr, int i, int i2);

    public static native int nativeYUVrotate(byte[] bArr, byte[] bArr2, int i, int i2);

    public static native int nativeYUVrotateLess(byte[] bArr, int i, int i2);

    public static native int nativeYuvToCropIntArray(byte[] bArr, int[] iArr, int i, int i2, int i3, int i4, int i5, int i6);

    public native int GetCodeDetectInfo(QBarCodeDetectInfo[] qBarCodeDetectInfoArr, QBarPoint[] qBarPointArr, int i);

    public static native int GetDetailResults(QBarResultJNI[] qBarResultJNIArr, QBarPoint[] qBarPointArr, QBarReportMsg[] qBarReportMsgArr, int i);

    public static native int GetOneResult(byte[] bArr, byte[] bArr2, byte[] bArr3, int[] iArr, int i);


    public static native int GetResults(QBarResultJNI[] qBarResultJNIArr, int i);

    public static native int GetZoomInfo(QBarZoomInfo qBarZoomInfo, int i);

    /**
     * 初始化扫一扫模块
     *
     * @param i                固定值： 1
     * @param z                固定值： true
     * @param z2               固定值： true
     * @param qBarOptForceDM   固定值： true
     * @param z3               固定值： true
     * @param i2               固定值： 1
     * @param i3               固定值： 0相机扫描 1图片扫描
     * @param str              扫描类型：ANY
     * @param str2             扫描的字符集：UTF-8
     * @param qbarAiModelParam 扫码必备资源
     * @return 初始化ID，后续好多API都需要：qBarId
     * @see QbarAiModelParam
     */
    public static native int Init(int i, boolean z, boolean z2, boolean qBarOptForceDM, boolean z3,
                                  int i2, int i3, String str, String str2, QbarAiModelParam qbarAiModelParam);

    /**
     * 释放一个已经初始化对对象
     *
     * @param qBarId id
     * @return 非0失败
     * @see QbarNative#Init
     */
    public static native int Release(int qBarId);

    /**
     * 扫描一张图片，数据需要是灰度
     *
     * @param onPreviewFrameDataGray       灰度的一个图片数据
     * @param onPreviewFrameDataGrayWeight 灰度数据的宽度
     * @param onPreviewFrameDataGrayHeight 灰度数据的高度
     * @param qBarId                       被初始化的ID
     * @return 非0失败
     * @see QbarNative#nativeGrayRotateCropSub(byte[], int, int, int, int, int, int, byte[], int[], int, int)
     * @see QbarNative#GetDetailResults(QBarResultJNI[], QBarPoint[], QBarReportMsg[], int) 
     */
    public static native int ScanImage(byte[] onPreviewFrameDataGray, int onPreviewFrameDataGrayWeight, int onPreviewFrameDataGrayHeight, int qBarId);

    /**
     * 设置一个扫描器
     *
     * @param scanMode     扫描模式
     *                     暂时不知各项含义
     *                     请固定：2，1
     * @param scanModeSize 扫描模式的大小
     * @param qBarId       被初始化的ID
     * @return 非0失败
     */
    public static native int SetReaders(int[] scanMode, int scanModeSize, int qBarId);
}