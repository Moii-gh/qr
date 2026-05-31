package com.example.qr

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object QRContentBuilder {

    fun buildUrl(url: String): String {
        return if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            url.trim()
        } else {
            "https://${url.trim()}"
        }
    }

    fun buildPlain(text: String): String = text

    fun buildPhone(phone: String): String {
        val cleanPhone = phone.replace(" ", "").replace("-", "")
        return "tel:$cleanPhone"
    }

    fun buildSms(phone: String, body: String): String {
        val cleanPhone = phone.replace(" ", "").replace("-", "")
        val encodedBody = try {
            java.net.URLEncoder.encode(body, "UTF-8").replace("+", "%20")
        } catch (e: Exception) {
            body
        }
        return "sms:$cleanPhone?body=$encodedBody"
    }

    fun buildEmail(email: String, subject: String, body: String): String {
        val encodedSubject = try { java.net.URLEncoder.encode(subject, "UTF-8").replace("+", "%20") } catch (e: Exception) { subject }
        val encodedBody = try { java.net.URLEncoder.encode(body, "UTF-8").replace("+", "%20") } catch (e: Exception) { body }
        
        return "mailto:${email.trim()}?subject=$encodedSubject&body=$encodedBody"
    }

    fun buildWifi(ssid: String, pass: String, security: String): String {
        // WIFI:T:WPA;S:SSID;P:password;;
        val cleanSsid = ssid.replace(";", "\\;").replace(",", "\\,")
        val cleanPass = pass.replace(";", "\\;").replace(",", "\\,")
        return "WIFI:T:$security;S:$cleanSsid;P:$cleanPass;;"
    }

    fun buildContact(name: String, phone: String, email: String, address: String): String {
        return "BEGIN:VCARD\n" +
                "VERSION:3.0\n" +
                "FN;CHARSET=UTF-8:${name.trim()}\n" +
                "N;CHARSET=UTF-8:;${name.trim()};;;\n" +
                "TEL:${phone.trim()}\n" +
                "EMAIL:${email.trim()}\n" +
                "ADR;CHARSET=UTF-8:;;${address.trim()};;;;\n" +
                "END:VCARD"
    }

    fun buildAddress(address: String): String = address.trim()

    fun buildCalendar(
        summary: String,
        dtstartMillis: Long,
        dtendMillis: Long,
        location: String,
        description: String
    ): String {
        val format = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val startStr = format.format(Date(dtstartMillis))
        val endStr = format.format(Date(dtendMillis))

        return "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "SUMMARY:${summary.trim()}\n" +
                "DTSTART:$startStr\n" +
                "DTEND:$endStr\n" +
                "LOCATION:${location.trim()}\n" +
                "DESCRIPTION:${description.trim()}\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR"
    }

    fun buildGeo(latitude: String, longitude: String): String {
        val cleanLat = latitude.trim()
        val cleanLon = longitude.trim()
        return "geo:$cleanLat,$cleanLon"
    }
}
