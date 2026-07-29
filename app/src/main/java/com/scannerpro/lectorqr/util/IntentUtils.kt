package com.scannerpro.lectorqr.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

object IntentUtils {
    fun startActivitySafe(context: Context, intent: Intent, errorMessage: String = "No app found to handle this action") {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("IntentUtils", "Failed to start activity for intent: $intent", e)
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
