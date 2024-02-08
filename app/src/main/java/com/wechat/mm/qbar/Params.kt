package com.wechat.mm.qbar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.SystemClock
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.tencent.qbar.QbarNative
import com.tencent.qbar.WechatScanner
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


@ExperimentalGetImage
object Params {

    private const val TAG = "QRCode"
    const val NO_PERMISSION: Int = -2
    const val YUV_FORMAT_FAILED: Int = -3
    const val RESULT_COPY_FAILED: Int = -4

    private val executor = Executors.newFixedThreadPool(2)
    private val permissions = listOf(Manifest.permission.CAMERA)

    private var camera: Camera? = null
    private var enableFlash = false

    val wechatScanner by lazy {
        val scanner = WechatScanner()
        val context = ContextProvider.get()
        scanner.init(context)
        scanner.setReader()
        scanner
    }

    /**
     * 释放扫码必备的资源文件
     *
     * 主要释放Assert下对qbar文件到 /data/data/package/files/qbar 下
     *
     * @param context 上下文
     * @param folder  输出到文件夹名称 默认：qbar
     *
     * @throws IOException 可能文件权限有问题
     */
    @Throws(IOException::class)
    fun releaseAssets(context: Context, folder: String = "qbar") {

        val outputFolder = File(context.filesDir, folder)
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }
        val files = arrayOf(
            "detect_model.bin",
            "detect_model.param",
            "srnet.bin",
            "srnet.param",
            "net_fc.bin",
            "net_fc.param",
            "net_fc.param",
            "QBarModels/V1.1.0.26/qbar_seg.xnet",
            "QBarModels/V1.1.0.26/qbar_sr.xnet",
            "QBarModels/V1.5.0.26/qbar_detect.xnet",
        )
        files.forEach { file ->
            context.assets.open("qbar/$file")
                .use { input ->
                    val target = File(outputFolder, file)
                    val parentDir = File(target.parent)
                    if (!parentDir.exists()) {
                        parentDir.mkdirs()
                    }
                    target.delete()
                    input.copyTo(target.outputStream())
                }
        }
    }

    suspend fun scanImageFile(
        path: String,
        bar: (code: Int, content: List<String>) -> Unit,
    ) {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val imageWidth: Int = options.outWidth
        val imageHeight: Int = options.outHeight
        options.inJustDecodeBounds = false
        options.inSampleSize = calImageSubsamplingSize(imageWidth, imageHeight)
        val bitmap =
            BitmapFactory.decodeFile(path, options)
        scanBitmap(bitmap, bar)
    }

    suspend fun scanBitmap(
        bitmap: Bitmap,
        bar: (code: Int, content: List<String>) -> Unit,
    ) {
        val qrBitmap = bitmap.preQRBitmap()
        val width = qrBitmap.width
        val height = qrBitmap.height
        val bytes = qrBitmap.getNV21()
        val scanResultList: List<QbarNative.QBarResultJNI> =
            wechatScanner.onPreviewFrame(
                data = bytes,
                size = Point(
                    width,
                    height
                ),
                crop = Rect(
                    0,
                    0,
                    width,
                    height
                ),
                rotation = 0
            )
        if (scanResultList.isNotEmpty()) {
            scanResultList.forEach { qBarResultJNI: QbarNative.QBarResultJNI ->
                Log.d(
                    "QBarRE",
                    "onPreviewFrame typeName=${qBarResultJNI.typeName} charset=${qBarResultJNI.charset} data=${
                        String(
                            qBarResultJNI.data,
                            Charset.forName(qBarResultJNI.charset)
                        )
                    }"
                )
            }
            val newResult =
                scanResultList.map { qBarResultJNI ->
                    String(
                        qBarResultJNI.data,
                        Charset.forName(qBarResultJNI.charset)
                    )
                }
            bar.invoke(1, newResult)
        }
    }

    fun startScan(
        activity: FragmentActivity,
        cameraPreview: PreviewView,
        qbarCallback: (code: Int, message: String, contents: List<String>) -> Unit,
    ) {
        if (!hasPermissions(activity)) {
            qbarCallback.invoke(-1, "no permission", emptyList())
            return
        }
        cameraPreview.bindCameraUseCases(
            activity = activity,
            qbarCallback = qbarCallback
        )
    }

    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(
            context.applicationContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun convertBitmapToByteArrayUncompressed(bitmap: Bitmap): ByteArray {
        val byteBuffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(byteBuffer)
        byteBuffer.rewind()
        return byteBuffer.array()
    }

    private fun PreviewView.bindCameraUseCases(
        activity: FragmentActivity,
        qbarCallback: (code: Int, message: String, contents: List<String>) -> Unit,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            // val size = Size(1600, 1200)
            // Set up the view finder use case to display camera preview
            val preview = Preview.Builder()
                .build()

            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAllowedResolutionMode(ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION)
                        .build()
                )
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                executor
            ) { imageProxy ->
                if (imageProxy.image != null) {
                    val startTime = SystemClock.elapsedRealtime()
                    val image = imageProxy.image!!
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val width = image.width
                    val height = image.height

                    val bytes = imageProxy.nv21ByteArray()
                    if (bytes != null) {
                        val scanResultList: List<QbarNative.QBarResultJNI> =
                            wechatScanner.onPreviewFrame(
                                data = bytes,
                                size = Point(
                                    width,
                                    height
                                ),
                                crop = Rect(
                                    0,
                                    0,
                                    width,
                                    height
                                ),
                                rotation = rotation
                            )

                        val resultString = scanResultList.map { qBarResultJNI ->
                            val json = JSONObject()
                            json.put("charset", qBarResultJNI.charset)
                            json.put("typeID", qBarResultJNI.typeID)
                            json.put("typeName", qBarResultJNI.typeName)
                            json.put(
                                "data", String(
                                    qBarResultJNI.data,
                                    Charset.forName(qBarResultJNI.charset)
                                )
                            )
                            json.put("costTime", "${SystemClock.elapsedRealtime() - startTime}ms")
                            json.toString()
                        }
                        if (resultString.isNotEmpty()) {
                            qbarCallback.invoke(1, "success", resultString)
                        }

                        Log.d(
                            "QBarRE",
                            "WeChatScanner decode image(width:$width height:$height rotation:$rotation) " +
                                    "cost: ${SystemClock.elapsedRealtime() - startTime}ms result:$resultString"
                        )
                    }
                }
                imageProxy.close()
            }

            // Create a new camera selector each time, enforcing lens facing
            val cameraSelector = CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Apply declared configs to CameraX using the same lifecycle owner
            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                activity,
                cameraSelector,
                preview,
                imageAnalysis
            )
            updateCameraOptions()
            preview.setSurfaceProvider(this.surfaceProvider)
        }, ContextCompat.getMainExecutor(activity))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun updateCameraOptions() {
        camera?.apply {
            // Reduce exposure time to decrease effect of motion blur
            val camera2Control = Camera2CameraControl.from(cameraControl)
            camera2Control.captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.FLASH_MODE,
                    if (enableFlash) {
                        CameraMetadata.FLASH_MODE_TORCH
                    } else {
                        CameraMetadata.FLASH_MODE_OFF
                    }
                )
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                    CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO
                )
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_MODE,
                    CameraMetadata.CONTROL_MODE_AUTO
                )
                .setCaptureRequestOption(
                    CaptureRequest.EDGE_MODE,
                    CameraMetadata.EDGE_MODE_OFF
                )
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_SCENE_MODE,
                    CameraMetadata.CONTROL_SCENE_MODE_BARCODE
                )
                .build()
        }
    }

    fun calImageSubsamplingSize(width: Int, height: Int): Int {
        val w = width + width % 2
        val h = height + height % 2

        val longSide = max(w, h)
        val shortSide = min(w, h)
        if (longSide == 0 || shortSide == 0) {
            return -1
        }
        val scale = shortSide.toFloat() / longSide.toFloat()
        if (scale <= 1.0f && scale > 0.5625) {
            return if (longSide < 1664) {
                1
            } else if (longSide < 4990) {
                2
            } else if (longSide < 10240) {
                4
            } else {
                8
            }
        } else if (scale <= 0.5625f && scale > 0.5f) {
            return if (longSide < 1280) {
                1
            } else {
                longSide / 1280
            }
        } else {
            return if (longSide < 4990) {
                1
            } else if (longSide < 10240) {
                2
            } else if (longSide < 20480) {
                4
            } else {
                ceil(longSide.toFloat() / (shortSide * 2)).toInt().coerceAtLeast(8)
            }
        }
    }
}
