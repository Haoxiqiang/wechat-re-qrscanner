package com.wechat.mm.qbar

import android.graphics.Point
import android.graphics.Rect
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tencent.qbar.QbarNative
import com.tencent.qbar.WechatScanner
import java.nio.charset.Charset

@Suppress("DEPRECATION")
class MainActivity2 : AppCompatActivity(), SurfaceHolder.Callback, Camera.PreviewCallback,
    Camera.AutoFocusCallback {

    private lateinit var wechatScanner: WechatScanner
    private lateinit var camera: Camera
    private lateinit var textView: TextView
    private lateinit var surfaceView: SurfaceView

    private var isProcessing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)
        surfaceView = findViewById(R.id.surfaceView)
    }

    fun onClickInit(view: View) {
        wechatScanner = WechatScanner()
        wechatScanner.releaseAssert(view.context)
        wechatScanner.init(view.context)
        wechatScanner.setReader()
        textView.text = wechatScanner.version()
    }

    fun onClickOpen(view: View) {
        surfaceView.holder.addCallback(this)

        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        camera.setPreviewDisplay(surfaceView.holder)
        camera.setDisplayOrientation(90)

        val parameters: Camera.Parameters = camera.parameters
        parameters.focusMode = Camera.Parameters.FLASH_MODE_AUTO

        camera.parameters = parameters
        camera.setPreviewCallback(this)
        camera.startPreview()
    }

    fun onClickFouce(view: View) {
        camera.autoFocus(this)
    }

    fun onClickReset(view: View) {
        textView.text = ""
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        if (isProcessing)
            return

        isProcessing = true

        val startTimestamp: Long = System.currentTimeMillis()
        val scanResultList: List<QbarNative.QBarResultJNI> = wechatScanner.onPreviewFrame(
            data = data,
            size = Point(camera.parameters.previewSize.width, camera.parameters.previewSize.height),
            crop = Rect(373, 36, 1163, 826),
            rotation = 90
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
                scanResultList.first().let { String(it.data, Charset.forName(it.charset)) }
            if (newResult.isNotEmpty()) {
                textView.post {
                    textView.text = newResult
                }
            }

            Log.d(
                "QBarRE",
                "onPreviewFrame scan cost: ${System.currentTimeMillis() - startTimestamp}ms",
            )
        }

        isProcessing = false
    }

    override fun onAutoFocus(success: Boolean, camera: Camera?) {
        Log.d(
            "QBarRE", "onAutoFocus success=$success"
        )
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        camera.release()
        super.onDestroy()
        wechatScanner.release()
    }

}
