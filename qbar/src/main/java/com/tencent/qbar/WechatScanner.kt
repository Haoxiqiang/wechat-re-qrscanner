package com.tencent.qbar

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import java.io.File
import java.io.IOException

/**
 * 微信扫一扫功能
 */
class WechatScanner {

    /**
     * 初始化的ID
     */
    var id: Int? = null
        private set


    /**
     * 初始化扫一扫模块
     *
     * @see release
     *
     * @throws IOException 找不到初始化的各项资源
     */
    @Throws(IOException::class)
    fun init(context: Context, folder: String = "qbar"): Int? =
        init(File(context.filesDir, folder).absoluteFile)

    /**
     * 初始化扫一扫模块
     * 此方法减少了context参数
     *
     * @param folder 释放的资源完整路径
     *
     * @return 0 => 成功
     * @throws IOException 找不到初始化的各项资源
     */
    @Throws(IOException::class)
    fun init(folder: File): Int? {

        if (id != null) throw RuntimeException("already")

        System.loadLibrary("opencv_world")
        System.loadLibrary("XNet")
        System.loadLibrary("wechatQrMod")

        val detectModelBinPathFile = File(folder, "detect_model.bin")
        val detectModelParamPath = File(folder, "detect_model.param")
        val superResolutionModelBinPath = File(folder, "srnet.bin")
        val superResolutionModelParamPath = File(folder, "srnet.param")
        val qbarSegmentationPath = File(folder, "QBarModels/V1.1.0.26/qbar_seg.xnet")
        val qbarSrPath = File(folder, "QBarModels/V1.1.0.26/qbar_sr.xnet")
        val qbarDetectModelPath = File(folder, "QBarModels/V1.5.0.26/qbar_detect.xnet")

        if (!detectModelBinPathFile.exists()) throw IOException()
        if (!detectModelParamPath.exists()) throw IOException()
        if (!superResolutionModelBinPath.exists()) throw IOException()
        if (!superResolutionModelParamPath.exists()) throw IOException()

        val qbarAiModelParam: QbarNative.QbarAiModelParam = QbarNative.QbarAiModelParam()
        qbarAiModelParam.detectModelVersion = "V1.5.0.26"
        qbarAiModelParam.superResolutionModelVersion = "V1.1.0.26"
        qbarAiModelParam.enable_seg = true
        qbarAiModelParam.qbar_segmentation_model_path_ = qbarSegmentationPath.absolutePath
        qbarAiModelParam.detect_model_bin_path_ = qbarDetectModelPath.absolutePath
        qbarAiModelParam.detect_model_param_path_ = ""
        qbarAiModelParam.superresolution_model_bin_path_ = qbarSrPath.absolutePath
        qbarAiModelParam.superresolution_model_param_path_ =
            superResolutionModelParamPath.absolutePath

        id = QbarNative.Init(
            1, true, true, true, true,
            1, 0, "ANY", "UTF-8", qbarAiModelParam
        )
        return id
    }

    /**
     * 设置解码器
     *
     * @param intArray 解码支持参数
     *  ALL_READERS = 0
     *  ONED_BARCODE = 1
     *  QRCODE = 2
     *  WXCODE = 3
     *  PDF417 = 4
     *  DATA_MATRIX = 5
     *
     * @return 0 => 成功
     */
    fun setReader(intArray: IntArray = intArrayOf(3, 2)): Int =
        QbarNative.SetReaders(
            intArray,
            intArray.size,
            id ?: throw IllegalArgumentException("did init ?")
        )

    /**
     * 当前扫一扫版本信息
     *
     * @return 3.2.20190712
     */
    fun version(): String = QbarNative.GetVersion()

    /**
     * 相机预览的数据
     *
     * @param data      相机数据
     * @param size      data对应的图片大小
     * @param crop      裁剪的图片大小
     * @param rotation  旋转图片角度
     *
     * @return 扫描完成的List
     */
    fun onPreviewFrame(
        data: ByteArray,
        size: Point,
        crop: Rect,
        rotation: Int,
    ): List<QbarNative.QBarResultJNI> {
        val qBarId: Int = id ?: throw RuntimeException("did init ?")

        val nativeGrayRotateCropSubDataWH = IntArray(2)
        val nativeGrayRotateCropSubData = ByteArray(crop.width() * crop.height() * 3 / 2)
        val nativeGrayRotateCropSubResult: Int = QbarNative.nativeGrayRotateCropSub(
            data,
            size.x,
            size.y,
            crop.left,
            crop.top,
            crop.width(),
            crop.height(),
            nativeGrayRotateCropSubData,
            nativeGrayRotateCropSubDataWH,
            rotation,
            0
        )
        if (nativeGrayRotateCropSubResult != 0) {
            throw RuntimeException("Native.nativeGrayRotateCropSub error: $nativeGrayRotateCropSubResult")
        }

        val scanImageResult: Int = QbarNative.ScanImage(
            nativeGrayRotateCropSubData.copyOf(),
            nativeGrayRotateCropSubDataWH[0],
            nativeGrayRotateCropSubDataWH[1],
            qBarId
        )
        if (scanImageResult != 0) {
            throw RuntimeException("Native.ScanImage error: $scanImageResult")
        }

        val qBarResultJNIArr: Array<QbarNative.QBarResultJNI> = arrayOf(
            QbarNative.QBarResultJNI(),
            QbarNative.QBarResultJNI(),
            QbarNative.QBarResultJNI()
        )

//        QbarNative.GetResults(qBarResultJNIArr, scanImageResult)
//        Log.d(
//            "GetResults",
//            qBarResultJNIArr.map { qBarResultJNI: QbarNative.QBarResultJNI? -> qBarResultJNI?.typeName }
//                .joinToString()
//        )

        val qBarPointArr: Array<QbarNative.QBarPoint> =
            arrayOf(QbarNative.QBarPoint(), QbarNative.QBarPoint(), QbarNative.QBarPoint())
        val qBarReportMsgArr: Array<QbarNative.QBarReportMsg> = arrayOf(
            QbarNative.QBarReportMsg(),
            QbarNative.QBarReportMsg(),
            QbarNative.QBarReportMsg()
        )

        val getDetailResults: Int =
            QbarNative.GetDetailResults(qBarResultJNIArr, qBarPointArr, qBarReportMsgArr, qBarId)
        if (getDetailResults < 0) {
            throw RuntimeException("Native.GetDetailResults error: $getDetailResults")
        }

        return qBarResultJNIArr.filter { qBarResultJNI: QbarNative.QBarResultJNI? -> qBarResultJNI?.typeName?.isNotEmpty() == true }
    }

    fun onDecodeImageFile() {

    }

    /**
     * 关闭
     */
    fun release(): Int = QbarNative.Release(id ?: throw RuntimeException("did init ?"))

}