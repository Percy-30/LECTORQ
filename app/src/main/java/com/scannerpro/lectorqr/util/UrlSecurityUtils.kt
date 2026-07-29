package com.scannerpro.lectorqr.util

import android.net.Uri

object UrlSecurityUtils {

    // Lista de acortadores de URL comunes que suelen usarse para ocultar destinos maliciosos
    private val SUSPICIOUS_SHORTENERS = listOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", 
        "buff.ly", "adf.ly", "bit.do", "mcaf.ee", "su.pr"
    )

    // Expresión regular básica para detectar si el host es una dirección IP (IPv4)
    private val IPV4_REGEX = Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$")

    /**
     * Evalúa si una URL tiene características típicas de phishing o enlaces maliciosos.
     * @param urlString La URL en formato String.
     * @return true si es sospechosa, false si parece segura.
     */
    fun isSuspiciousUrl(urlString: String): Boolean {
        if (urlString.isBlank()) return false
        
        try {
            // Asegurarnos de que tenga un esquema válido para el Uri parser
            val urlToParse = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                "http://$urlString"
            } else {
                urlString
            }
            
            val uri = Uri.parse(urlToParse)
            val host = uri.host?.lowercase() ?: return false

            // 1. Detección de uso de dirección IP directa en lugar de dominio (ej. http://192.168.1.5/login)
            if (IPV4_REGEX.matches(host)) {
                return true
            }

            // 2. Detección de acortadores de URL populares (frecuentes en SMS phishing)
            if (SUSPICIOUS_SHORTENERS.any { host.contains(it) }) {
                return true
            }

            // 3. Detección de múltiples subdominios engañosos (ej. www.paypal.com.secure-login.net)
            // Si tiene más de 3 puntos, puede ser un intento de ocultar el dominio real
            val dotCount = host.count { it == '.' }
            if (dotCount > 3) {
                return true
            }

            // 4. HTTP en lugar de HTTPS (No bloqueante por sí solo, pero sumado a otros factores es riesgoso,
            // por ahora lo dejaremos pasar a menos que queramos ser muy estrictos).
            // if (uri.scheme == "http") return true

            return false
        } catch (e: Exception) {
            // Si la URL está malformada de una forma que el parser falla, es mejor tratarla con precaución
            return true
        }
    }
}
