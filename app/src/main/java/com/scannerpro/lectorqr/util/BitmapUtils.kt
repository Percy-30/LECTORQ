package com.scannerpro.lectorqr.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

object BitmapUtils {
    fun getDrawableAsBitmap(context: Context, drawableResId: Int, size: Int, tintColor: Int? = null): Bitmap? {
        val drawable: Drawable = ContextCompat.getDrawable(context, drawableResId)?.mutate() ?: return null
        if (tintColor != null) {
            androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, tintColor)
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Safely decodes a bitmap from a file, downsampling it to avoid OutOfMemoryError.
     * Must be called from a coroutine.
     */
    suspend fun decodeSampledBitmapFromFile(
        path: String,
        reqWidth: Int = 500,
        reqHeight: Int = 500
    ): Bitmap? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val options = android.graphics.BitmapFactory.Options()
            options.inJustDecodeBounds = true
            android.graphics.BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            
            android.graphics.BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            android.util.Log.e("BitmapUtils", "Error loading bitmap safely from path: $path", e)
            null
        }
    }

    private fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height: Int = options.outHeight
        val width: Int = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
