package com.scannerpro.lectorqr.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.mlkit.vision.barcode.common.Barcode
import com.scannerpro.lectorqr.R

object BarcodeTypeUtils {
    fun getIconForType(type: Int): ImageVector {
        return when (type) {
            Barcode.TYPE_WIFI -> Icons.Default.Wifi
            Barcode.TYPE_URL -> Icons.Default.Link
            Barcode.TYPE_CONTACT_INFO -> Icons.Default.Person
            Barcode.TYPE_EMAIL -> Icons.Default.Email
            Barcode.TYPE_PHONE -> Icons.Default.Phone
            Barcode.TYPE_SMS -> Icons.Default.Sms
            Barcode.TYPE_GEO -> Icons.Default.LocationOn
            Barcode.TYPE_CALENDAR_EVENT -> Icons.Default.Event
            Barcode.TYPE_PRODUCT -> Icons.Default.QrCode
            Barcode.TYPE_ISBN -> Icons.Default.MenuBook
            Barcode.TYPE_CONTACT_INFO -> Icons.Default.Person // Mi código QR uses this type
            else -> Icons.Default.TextFields
        }
    }

    fun getDrawableForScan(type: Int, customName: String?): Int? {
        if (type == Barcode.TYPE_URL && customName != null) {
            return when (customName.lowercase()) {
                "whatsapp" -> R.drawable.ic_whatsapp
                "facebook" -> R.drawable.ic_facebook
                "instagram" -> R.drawable.ic_instagram
                "youtube" -> R.drawable.ic_youtube
                "twitter" -> R.drawable.ic_twitter_x
                "linkedin" -> R.drawable.ic_linkedin
                "tiktok" -> R.drawable.ic_tiktok
                else -> null
            }
        }
        return null
    }

    fun getTypeNameRes(type: Int): Int {
        return when (type) {
            Barcode.TYPE_WIFI -> R.string.type_wifi
            Barcode.TYPE_URL -> R.string.type_url
            Barcode.TYPE_CONTACT_INFO -> R.string.type_contact
            Barcode.TYPE_EMAIL -> R.string.type_email
            Barcode.TYPE_PHONE -> R.string.type_phone
            Barcode.TYPE_SMS -> R.string.type_sms
            Barcode.TYPE_GEO -> R.string.type_geo
            Barcode.TYPE_CALENDAR_EVENT -> R.string.type_calendar
            Barcode.TYPE_PRODUCT -> R.string.type_product
            Barcode.TYPE_ISBN -> R.string.type_isbn
            else -> R.string.type_text
        }
    }

    fun getTypeName(type: Int): String {
        return when (type) {
            Barcode.TYPE_WIFI -> "WIFI"
            Barcode.TYPE_URL -> "URL"
            Barcode.TYPE_CONTACT_INFO -> "CONTACT"
            Barcode.TYPE_EMAIL -> "EMAIL"
            Barcode.TYPE_PHONE -> "PHONE"
            Barcode.TYPE_SMS -> "SMS"
            Barcode.TYPE_GEO -> "GEO"
            Barcode.TYPE_CALENDAR_EVENT -> "CALENDAR"
            Barcode.TYPE_PRODUCT -> "PRODUCT"
            Barcode.TYPE_ISBN -> "ISBN"
            else -> "TEXT"
        }
    }

    fun getFormattedValueWithLabels(type: Int, rawValue: String?): List<Pair<Int, String>> {
        if (rawValue == null) return emptyList()
        return when (type) {
            Barcode.TYPE_WIFI -> {
                val ssid = rawValue.substringAfter("S:", "").substringBefore(";", "")
                val encryption = rawValue.substringAfter("T:", "WPA").substringBefore(";", "")
                val password = rawValue.substringAfter("P:", "").substringBefore(";", "")
                listOfNotNull(
                    if (ssid.isNotEmpty()) R.string.field_network_name to ssid else null,
                    R.string.field_security to encryption,
                    if (password.isNotEmpty()) R.string.field_password to password else null
                )
            }
            Barcode.TYPE_CONTACT_INFO -> {
                val name = parseContactField(rawValue, "FN:", "N:", "NAME:")
                val org = parseContactField(rawValue, "ORG:")
                val tel = parseContactField(rawValue, "TEL:", "PHONE:")
                val email = parseContactField(rawValue, "EMAIL:", "MAIL:")
                
                val rawAdr = parseContactField(rawValue, "ADR:", "ADDRESS:")
                val adr = rawAdr.split(";")
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .trim()

                val note = parseContactField(rawValue, "NOTE:")
                val url = parseContactField(rawValue, "URL:")
                listOfNotNull(
                    if (name.isNotEmpty()) R.string.field_name to name else null,
                    if (org.isNotEmpty()) R.string.field_organization to org else null,
                    if (tel.isNotEmpty()) R.string.field_phone to tel else null,
                    if (email.isNotEmpty()) R.string.field_email to email else null,
                    if (adr.isNotEmpty()) R.string.field_address to adr else null,
                    if (note.isNotEmpty()) R.string.field_note to note else null,
                    if (url.isNotEmpty()) R.string.field_website to url else null
                )
            }
            Barcode.TYPE_EMAIL -> {
                val to = rawValue.substringAfter("MATMSG:TO:", "").substringBefore(";", "").ifEmpty {
                    rawValue.substringAfter("mailto:", "").substringBefore("?")
                }
                val subject = rawValue.substringAfter("SUB:", "").substringBefore(";", "").ifEmpty {
                    rawValue.substringAfter("subject=", "").substringBefore("&")
                }
                val body = rawValue.substringAfter("BODY:", "").substringBefore(";;", "").ifEmpty {
                    rawValue.substringAfter("body=", "").substringBefore("&")
                }
                listOfNotNull(
                    if (to.isNotEmpty()) R.string.field_to to to else null,
                    if (subject.isNotEmpty()) R.string.field_subject to subject else null,
                    if (body.isNotEmpty()) R.string.field_message to body else null
                )
            }
            Barcode.TYPE_SMS -> {
                val phone = rawValue.substringAfter("smsto:", "").substringBefore(":").ifEmpty {
                    rawValue.substringAfter("SMSTO:", "").substringBefore(":")
                }
                val message = if (rawValue.contains(":")) rawValue.substringAfterLast(":") else ""
                listOfNotNull(
                    if (phone.isNotEmpty()) R.string.field_phone to phone else null,
                    if (message.isNotEmpty()) R.string.field_message to message else null
                )
            }
            Barcode.TYPE_PHONE -> listOf(R.string.field_phone to rawValue)
            Barcode.TYPE_GEO -> {
                val coords = rawValue.substringAfter("geo:", "").substringBefore("?").ifEmpty { rawValue }
                listOf(R.string.field_coordinates to coords)
            }
            Barcode.TYPE_CALENDAR_EVENT -> {
                val summary = parseVCardField(rawValue, "SUMMARY:")
                val location = parseVCardField(rawValue, "LOCATION:")
                val description = parseVCardField(rawValue, "DESCRIPTION:")
                listOfNotNull(
                    if (summary.isNotEmpty()) R.string.field_event to summary else null,
                    if (location.isNotEmpty()) R.string.type_geo to location else null,
                    if (description.isNotEmpty()) R.string.field_description to description else null
                )
            }
            Barcode.TYPE_URL -> emptyList() // handled as content
            Barcode.TYPE_PRODUCT -> listOf(R.string.field_code to rawValue)
            Barcode.TYPE_ISBN -> listOf(R.string.type_isbn to rawValue)
            else -> emptyList()
        }
    }

    fun getFormattedValue(context: android.content.Context, type: Int, rawValue: String?): String {
        val items = getFormattedValueWithLabels(type, rawValue)
        return if (items.isEmpty()) rawValue ?: ""
        else items.joinToString("\n") { (labelRes, value) -> "${context.getString(labelRes)} $value" }
    }

    private fun parseContactField(raw: String, vararg tags: String): String {
        for (tag in tags) {
            val tagIndex = raw.indexOf(tag, ignoreCase = true)
            if (tagIndex != -1) {
                val start = tagIndex + tag.length
                var end = raw.indexOfAny(charArrayOf('\n', '\r'), start)
                val isMeCard = raw.contains("MECARD:", ignoreCase = true)
                if (isMeCard) {
                    val semiEnd = raw.indexOf(';', start)
                    if (semiEnd != -1 && (end == -1 || semiEnd < end)) {
                        end = semiEnd
                    }
                }
                val value = if (end == -1) raw.substring(start).trim()
                else raw.substring(start, end).trim()
                if (value.isNotEmpty()) return value
            }
        }
        return ""
    }

    private fun parseVCardField(raw: String, field: String): String {
        return parseContactField(raw, field)
    }
}
