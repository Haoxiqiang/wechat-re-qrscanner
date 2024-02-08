package com.wechat.mm.qbar

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView

@Suppress("DEPRECATION")
@SuppressLint("UnsafeOptInUsageError")
class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var cameraPreview: PreviewView

    private var lastString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)
        cameraPreview = findViewById(R.id.cameraPreview)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.clear()
        menu?.add(0, 0, 0, "Release Assets")
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.groupId == 0 && item.itemId == 0) {
            Params.releaseAssets(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun onClickInit(view: View) {
        textView.text = Params.wechatScanner.version()
    }

    fun onClickOpenCamera(view: View) {
        Params.startScan(
            activity = this,
            cameraPreview = cameraPreview,
            qbarCallback = { code: Int, message: String, contents: List<String> ->
                val newData = contents.joinToString()
                Log.d("WXScanner", "code:$code  message:$message $newData")
                if (lastString == newData) {
                    return@startScan
                }
                lastString = newData
                if (newData.isNotEmpty()) {
                    textView.post {
                        textView.text = newData
                    }
                }
            }
        )
    }

    fun onClickOpenFile(view: View) {
        Params.startScan(
            activity = this,
            cameraPreview = cameraPreview,
            qbarCallback = { code: Int, message: String, contents: List<String> ->
                val newData = contents.joinToString()
                Log.d("WXScanner", "code:$code  message:$message $newData")
                if (lastString == newData) {
                    return@startScan
                }
                lastString = newData
                if (newData.isNotEmpty()) {
                    textView.post {
                        textView.text = newData
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        Params.wechatScanner.release()
        super.onDestroy()
    }
}
