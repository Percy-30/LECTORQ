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
}
