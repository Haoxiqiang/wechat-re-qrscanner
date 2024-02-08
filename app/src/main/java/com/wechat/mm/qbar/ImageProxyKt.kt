package com.wechat.mm.qbar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

fun Bitmap.preQRBitmap(): Bitmap {
    if (!hasAlpha()) {
        val bmpGrayscale: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixColorFilter
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(this, 0f, 0f, paint)
        return resizeBitmap(480, 480)
    }
    return resizeBitmap(480, 480)
}

fun Bitmap.resizeBitmap(maxHeight: Int, maxWidth: Int): Bitmap {
    val sourceWidth: Int = width
    val sourceHeight: Int = height

    var targetWidth = maxWidth
    var targetHeight = maxHeight

    val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
    val targetRatio = maxWidth.toFloat() / maxHeight.toFloat()

    if (targetRatio > sourceRatio) {
        targetWidth = (maxHeight.toFloat() * sourceRatio).toInt()
    } else {
        targetHeight = (maxWidth.toFloat() / sourceRatio).toInt()
    }

    return Bitmap.createScaledBitmap(
        this, targetWidth, targetHeight, true
    )
}

fun Bitmap.toRGBABytes(): ByteArray {
    val pixels = IntArray(width * height)
    val bytes = ByteArray(pixels.size * 4)
    getPixels(
        pixels,
        0,
        width,
        0,
        0,
        width,
        height
    )
    var i = 0
    for (pixel in pixels) {
        // Get components assuming is ARGB
        val alpha = pixel shr 24 and 0xff
        val red = pixel shr 16 and 0xff
        val green = pixel shr 8 and 0xff
        val blue = pixel and 0xff
        bytes[i++] = red.toByte()
        bytes[i++] = green.toByte()
        bytes[i++] = blue.toByte()
        bytes[i++] = alpha.toByte()
    }
    return bytes
}