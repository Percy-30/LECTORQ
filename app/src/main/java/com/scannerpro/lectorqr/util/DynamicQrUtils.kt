package com.scannerpro.lectorqr.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DynamicQrUtils {

    /**
     * Genera un enlace corto utilizando la API gratuita de tinyurl.com
     * Esto simula un QR Dinámico (el enlace corto puede redirigirse en un backend real, 
     * aquí usamos un acortador para demostrar el concepto visual Premium).
     */
    suspend fun createDynamicUrl(originalUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedUrl = java.net.URLEncoder.encode(originalUrl, "UTF-8")
            val apiUrl = "https://tinyurl.com/api-create.php?url=$encodedUrl"
            
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                if (response.startsWith("http")) {
                    return@withContext response
                }
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
