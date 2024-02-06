package com.wechat.mm.qbar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.Image
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
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.nio.charset.Charset
import java.util.concurrent.Executors
import kotlin.experimental.inv


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

    suspend fun scanBitmap(
        bitmap: Bitmap,
        bar: (code: Int, content: List<String>) -> Unit,
    ) {

    }

    fun startScan(
        activity: FragmentActivity,
        cameraPreview: PreviewView,
        bar: (code: Int, contents: List<String>) -> Unit,
    ) {
        if (!hasPermissions(activity)) {
            bar.invoke(NO_PERMISSION, emptyList())
            return
        }

        cameraPreview.bindCameraUseCases(
            activity = activity,
            analysisCallback = bar
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
        analysisCallback: (code: Int, contents: List<String>) -> Unit,
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

                    Log.d(
                        "QBarRE",
                        "WeChatScanner start decode image(width:$width height:$height rotation:$rotation)."
                    )

                    // val bytes = ByteArray(image.planes[0].buffer.remaining())
                    val bytes = YUV_420_888toNV21(image)

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
                        analysisCallback.invoke(1, newResult)
                    }

                    Log.d(
                        "QBarRE",
                        "onPreviewFrame scan cost: ${SystemClock.elapsedRealtime() - startTime}ms",
                    )
                } else {
                    Log.d(
                        "QBarRE",
                        "imageProxy image is null."
                    )
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

    fun YUV_420_888toNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V
        var rowStride = image.planes[0].rowStride
        assert(image.planes[0].pixelStride == 1)
        var pos = 0
        if (rowStride == width) { // likely
            yBuffer[nv21, 0, ySize]
            pos += ySize
        } else {
            var yBufferPos = -rowStride.toLong() // not an actual position
            while (pos < ySize) {
                yBufferPos += rowStride.toLong()
                yBuffer.position(yBufferPos.toInt())
                yBuffer[nv21, pos, width]
                pos += width
            }
        }
        rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride
        assert(rowStride == image.planes[1].rowStride)
        assert(pixelStride == image.planes[1].pixelStride)
        if (pixelStride == 2 && rowStride == width && uBuffer[0] == vBuffer[1]) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            val savePixel = vBuffer[1]
            try {
                vBuffer.put(1, savePixel.inv())
                if (uBuffer[0] == savePixel.inv()) {
                    vBuffer.put(1, savePixel)
                    vBuffer.position(0)
                    uBuffer.position(0)
                    vBuffer[nv21, ySize, 1]
                    uBuffer[nv21, ySize + 1, uBuffer.remaining()]
                    return nv21 // shortcut
                }
            } catch (ex: ReadOnlyBufferException) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel)
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                nv21[pos++] = vBuffer[vuPos]
                nv21[pos++] = uBuffer[vuPos]
            }
        }
        return nv21
    }
}
