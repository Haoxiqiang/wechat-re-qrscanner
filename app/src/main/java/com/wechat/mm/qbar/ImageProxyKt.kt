package com.wechat.mm.qbar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.nio.ReadOnlyBufferException
import kotlin.experimental.inv
import kotlin.math.max
import kotlin.math.min

fun Bitmap.preQRBitmap(): Bitmap {
    if (hasAlpha()) {
        val bmpGrayscale: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixColorFilter
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(this, 0f, 0f, paint)
        return bmpGrayscale
    }
    return this
}

fun Bitmap.getNV21(): ByteArray {
    val argb = IntArray(width * height)
    getPixels(argb, 0, width, 0, 0, height, height)
    val yuv = ByteArray(width * height * 3 / 2)
    encodeYUV420SP(yuv, argb, width, height)
    recycle()
    return yuv
}

fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
    val frameSize = width * height
    var yIndex = 0
    var uvIndex = frameSize

    var index = 0
    for (j in 0 until height) {
        for (i in 0 until width) {
            val a = argb[index] and -0x1000000 shr 24 // a is not used obviously
            val r = argb[index] and 0xff0000 shr 16
            val g = argb[index] and 0xff00 shr 8
            val b = argb[index] and 0xff shr 0

            // well known RGB to YUV algorithm
            val y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
            val u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
            val v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128

            // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
            //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
            //    pixel AND every other scanline.

            yuv420sp[yIndex++] = max(0, min(255, y)).toByte()
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = max(0, min(255, v)).toByte()
                yuv420sp[uvIndex++] = max(0, min(255, u)).toByte()
            }
            index++
        }
    }
}

@ExperimentalGetImage
fun ImageProxy.nv21ByteArray(): ByteArray? {
    val image = image ?: return null
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