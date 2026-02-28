package com.scannerpro.lectorqr.util

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileHelper @Inject constructor() {
    
    suspend fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, "saved_qrs")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, "$fileName.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveSharedFile(context: Context, content: String, fileName: String, extension: String): File? = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "exports")
            if (!cachePath.exists()) cachePath.mkdirs()
            val file = File(cachePath, "$fileName.$extension")
            FileOutputStream(file).use { stream ->
                stream.write(content.toByteArray())
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "Compartir") {
        try {
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = android.content.Intent.createChooser(intent, chooserTitle)
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteFile(filePath: String?) = withContext(Dispatchers.IO) {
        if (filePath == null) return@withContext
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
