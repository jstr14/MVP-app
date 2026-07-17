package com.hectordev.mvp.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun compress(imageUri: Uri, maxDimension: Int = 1280, quality: Int = 80): ByteArray {
        val original = context.contentResolver.openInputStream(imageUri)
            ?.use { BitmapFactory.decodeStream(it) }
            ?: throw Exception("Cannot open image URI")

        val rotated = fixRotation(original, imageUri)
        val scaled = scaleBitmap(rotated, maxDimension)

        if (rotated != original) original.recycle()
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
        scaled.recycle()

        return output.toByteArray()
    }

    private fun fixRotation(bitmap: Bitmap, imageUri: Uri): Bitmap {
        val degrees = context.contentResolver.openInputStream(imageUri)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap
        val ratio = w.toFloat() / h.toFloat()
        val (newW, newH) = if (w > h) maxDimension to (maxDimension / ratio).toInt()
                           else (maxDimension * ratio).toInt() to maxDimension
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}