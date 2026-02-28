package com.scannerpro.lectorqr.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.vector.ImageVector
import com.scannerpro.lectorqr.R

object QrLogoHelper {

    fun getLogoForType(context: Context, typeId: Int, content: String? = null, foregroundColor: Int? = null): Bitmap? {
        val socialId = getSocialLogoId(content)
        val isSocial = socialId != null && typeId == com.google.mlkit.vision.barcode.common.Barcode.TYPE_URL
        
        val drawableId = when (typeId) {
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_WIFI -> R.drawable.ic_wifi
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_URL -> socialId ?: R.drawable.ic_link
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_CONTACT_INFO -> R.drawable.ic_person
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_EMAIL -> R.drawable.ic_email
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_PHONE -> R.drawable.ic_phone
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_SMS -> R.drawable.ic_sms
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_GEO -> R.drawable.ic_location
            com.google.mlkit.vision.barcode.common.Barcode.TYPE_CALENDAR_EVENT -> R.drawable.ic_calendar
            else -> R.drawable.ic_text
        }
        val tint = if (!isSocial) foregroundColor else null
        return getBitmapFromDrawable(context, drawableId, tint)
    }

    private fun getSocialLogoId(url: String?): Int? {
        val u = url?.lowercase() ?: return null
        return when {
            u.contains("wa.me") || u.contains("whatsapp.com") -> R.drawable.ic_whatsapp
            u.contains("facebook.com") || u.contains("fb.com") || u.contains("fb.watch") -> R.drawable.ic_facebook
            u.contains("instagram.com") -> R.drawable.ic_instagram
            u.contains("youtube.com") || u.contains("youtu.be") -> R.drawable.ic_youtube
            u.contains("twitter.com") || u.contains("t.co") || u.contains("x.com") -> R.drawable.ic_twitter_x
            u.contains("linkedin.com") -> R.drawable.ic_linkedin
            u.contains("tiktok.com") -> R.drawable.ic_tiktok
            else -> null
        }
    }

    private fun getBitmapFromDrawable(context: Context, drawableId: Int, tintColor: Int? = null): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, drawableId)?.mutate() ?: return null
        if (tintColor != null) {
            androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, tintColor)
        }
        if (drawable is BitmapDrawable && tintColor == null) return drawable.bitmap

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.takeIf { it > 0 } ?: 100,
            drawable.intrinsicHeight.takeIf { it > 0 } ?: 100,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun getBitmapFromVector(context: Context, drawableId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
