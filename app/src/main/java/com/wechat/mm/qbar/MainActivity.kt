package com.wechat.mm.qbar

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@SuppressLint("UnsafeOptInUsageError")
class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var cameraPreview: PreviewView
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private var lastString = ""
    private val qbarCallback = { code: Int, message: String, contents: List<String> ->
        val newData = contents.joinToString()
        Log.d("WXScanner", "code:$code  message:$message $newData")
        if (lastString != newData) {
            lastString = newData
            if (newData.isNotEmpty()) {
                textView.post {
                    textView.text = newData
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)
        cameraPreview = findViewById(R.id.cameraPreview)

        pickMedia =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    val resolver = applicationContext.contentResolver
                    val readOnlyMode = "r"
                    resolver.openFileDescriptor(uri, readOnlyMode).use { pfd ->
                        if (pfd == null) {
                            Toast.makeText(this, "Bitmap load failed.", Toast.LENGTH_SHORT).show()
                            return@use
                        }
                        val bitmap = BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                        lifecycleScope.launch(Dispatchers.IO) {
                            Params.scanBitmap(bitmap, qbarCallback)
                        }
                    }
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }

        textView.text = Params.wechatScanner.version()
    }

    fun onClickOpenCamera(view: View) {
        Params.startScan(
            activity = this,
            cameraPreview = cameraPreview,
            qbarCallback = qbarCallback
        )
    }

    fun onClickOpenFile(view: View) {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun onDestroy() {
        Params.wechatScanner.release()
        super.onDestroy()
    }
}
