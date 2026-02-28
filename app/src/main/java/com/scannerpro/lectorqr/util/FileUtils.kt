package com.scannerpro.lectorqr.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    private const val TAG = "FileUtils"

    fun saveFileToDownloads(context: Context, filename: String, mimeType: String, content: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { stream ->
                        stream.write(content.toByteArray())
                    }
                    Toast.makeText(context, "$filename guardado en Descargas", Toast.LENGTH_SHORT).show()
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, filename)
                FileOutputStream(file).use { stream ->
                    stream.write(content.toByteArray())
                }
                Toast.makeText(context, "$filename guardado en Descargas", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file: $filename", e)
            Toast.makeText(context, "Error al guardar el archivo", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(context: Context, filename: String, mimeType: String, content: String) {
        try {
            val cacheDir = File(context.cacheContext().cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val file = File(cacheDir, filename)
            FileOutputStream(file).use { it.write(content.toByteArray()) }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val shareIntent = Intent.createChooser(intent, "Compartir archivo")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file: $filename", e)
            Toast.makeText(context, "Error al compartir el archivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Context.cacheContext(): Context {
        // Use application context to avoid memory leaks
        return applicationContext ?: this
    }
}
