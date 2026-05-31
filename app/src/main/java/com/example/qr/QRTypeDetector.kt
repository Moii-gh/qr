package com.example.qr

import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

object QRTypeDetector {

    private val cache = ConcurrentHashMap<String, QRParsedResult>()

    fun detect(rawValue: String?): QRParsedResult {
        if (rawValue.isNullOrBlank()) {
            return QRParsedResult(QRType.UNKNOWN, "", "Пустой QR-код")
        }

        return cache.getOrPut(rawValue) {
            val text = rawValue.trim()
            val lower = text.lowercase()

            when {
                // Wi-Fi Access
                lower.startsWith("wifi:") -> parseWifi(text)

                // Contact MeCard or vCard
                lower.startsWith("mecard:") || lower.contains("begin:vcard") -> parseContact(text)

                // Calendar Event
                lower.contains("begin:vevent") || lower.contains("begin:vcalendar") -> parseCalendar(text)

                // SMS message
                lower.startsWith("sms:") || lower.startsWith("smsto:") -> parseSms(text)

                // Email
                lower.startsWith("mailto:") || isEmail(text) -> parseEmail(text)

                // Phone
                lower.startsWith("tel:") || isPhone(text) -> parsePhone(text)

                // URL
                lower.startsWith("http://") || lower.startsWith("https://") || isUrl(text) -> parseUrl(text)

                // Geo Coordinates
                lower.startsWith("geo:") || isGeo(text) -> parseGeo(text)

                // Address Check
                isAddress(text) -> parseAddress(text)

                // Fallback plain text
                else -> parseText(text)
            }
        }
    }

    private fun isUrl(text: String): Boolean {
        if (text.startsWith("www.", ignoreCase = true)) {
            return true
        }
        val urlPattern = Regex("^(www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}(/.*)?$")
        return urlPattern.matches(text)
    }

    private fun parseUrl(text: String): QRParsedResult {
        var url = text
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "https://$url"
        }
        val host = try {
            val uri = java.net.URI(url)
            uri.host ?: "Ссылка"
        } catch (e: Exception) {
            "Ссылка"
        }
        return QRParsedResult(
            type = QRType.URL,
            rawValue = text,
            title = host,
            extractedData = mapOf("url" to url)
        )
    }

    private fun isPhone(text: String): Boolean {
        val phonePattern = Regex("^[+]?[0-9\\s\\-()]{7,20}$")
        return phonePattern.matches(text)
    }

    private fun parsePhone(text: String): QRParsedResult {
        val phoneNum = if (text.startsWith("tel:", ignoreCase = true)) text.substring(4) else text
        return QRParsedResult(
            type = QRType.PHONE,
            rawValue = text,
            title = phoneNum,
            extractedData = mapOf("phone" to phoneNum)
        )
    }

    private fun parseSms(text: String): QRParsedResult {
        // sms:<phone>?body=<body>
        val content = if (text.startsWith("sms:", ignoreCase = true)) text.substring(4) else text.substring(6) // smsto:
        val questionMarkIndex = content.indexOf('?')
        val phone = if (questionMarkIndex != -1) content.substring(0, questionMarkIndex) else content
        var body = ""
        if (questionMarkIndex != -1) {
            val query = content.substring(questionMarkIndex + 1)
            val params = query.split('&')
            for (param in params) {
                val parts = param.split('=')
                if (parts.size >= 2 && parts[0].equals("body", ignoreCase = true)) {
                    body = try {
                        URLDecoder.decode(parts[1], "UTF-8")
                    } catch (e: Exception) {
                        parts[1]
                    }
                }
            }
        }
        return QRParsedResult(
            type = QRType.SMS,
            rawValue = text,
            title = "SMS на $phone",
            extractedData = mapOf("phone" to phone, "body" to body)
        )
    }

    private fun isEmail(text: String): Boolean {
        val emailPattern = Regex("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$")
        return emailPattern.matches(text)
    }

    private fun parseEmail(text: String): QRParsedResult {
        if (text.startsWith("mailto:", ignoreCase = true)) {
            val emailUri = text.substring(7)
            var email = emailUri
            var subject = ""
            var body = ""
            val questionMarkIndex = emailUri.indexOf('?')
            if (questionMarkIndex != -1) {
                email = emailUri.substring(0, questionMarkIndex)
                val query = emailUri.substring(questionMarkIndex + 1)
                val params = query.split('&')
                for (param in params) {
                    val parts = param.split('=')
                    if (parts.size >= 2) {
                        val key = parts[0].lowercase()
                        val value = try {
                            URLDecoder.decode(parts[1], "UTF-8")
                        } catch (e: Exception) {
                            parts[1]
                        }
                        if (key == "subject") subject = value
                        if (key == "body") body = value
                    }
                }
            }
            return QRParsedResult(
                type = QRType.EMAIL,
                rawValue = text,
                title = "Письмо на $email",
                extractedData = mapOf("email" to email, "subject" to subject, "body" to body)
            )
        } else {
            return QRParsedResult(
                type = QRType.EMAIL,
                rawValue = text,
                title = "Письмо на $text",
                extractedData = mapOf("email" to text)
            )
        }
    }

    private fun parseWifi(text: String): QRParsedResult {
        // WIFI:T:WPA;S:NetworkName;P:password;;
        val payload = text.substring(5)
        var ssid = ""
        var password = ""
        var security = ""
        
        val parts = payload.split(';')
        for (part in parts) {
            val colonIndex = part.indexOf(':')
            if (colonIndex != -1 && colonIndex < part.length - 1) {
                val key = part.substring(0, colonIndex).uppercase()
                val value = part.substring(colonIndex + 1)
                when (key) {
                    "S" -> ssid = value
                    "P" -> password = value
                    "T" -> security = value
                }
            }
        }
        return QRParsedResult(
            type = QRType.WIFI,
            rawValue = text,
            title = ssid.ifEmpty { "Wi-Fi Сеть" },
            extractedData = mapOf("ssid" to ssid, "password" to password, "security" to security)
        )
    }

    private fun parseContact(text: String): QRParsedResult {
        var name = ""
        var phone = ""
        var email = ""
        var address = ""

        if (text.startsWith("mecard:", ignoreCase = true)) {
            val payload = text.substring(7)
            val parts = payload.split(';')
            for (part in parts) {
                val colonIndex = part.indexOf(':')
                if (colonIndex != -1 && colonIndex < part.length - 1) {
                    val key = part.substring(0, colonIndex).uppercase()
                    val value = part.substring(colonIndex + 1)
                    when (key) {
                        "N" -> name = value.replace(",", " ").trim()
                        "TEL" -> phone = value.trim()
                        "EMAIL" -> email = value.trim()
                        "ADR" -> address = value.replace(",", " ").trim()
                    }
                }
            }
        } else {
            // vCard lines
            val lines = text.split('\n')
            for (line in lines) {
                val cleanLine = line.trim()
                val colonIndex = cleanLine.indexOf(':')
                if (colonIndex != -1 && colonIndex < cleanLine.length - 1) {
                    val keyPart = cleanLine.substring(0, colonIndex).uppercase()
                    val value = processVValue(keyPart, cleanLine.substring(colonIndex + 1))

                    if (keyPart.startsWith("FN")) {
                        name = value.trim()
                    } else if (keyPart.startsWith("N") && name.isEmpty()) {
                        val nameParts = value.split(';')
                        val last = nameParts.getOrNull(0) ?: ""
                        val first = nameParts.getOrNull(1) ?: ""
                        name = "$first $last".trim()
                    } else if (keyPart.startsWith("TEL")) {
                        phone = value.trim()
                    } else if (keyPart.startsWith("EMAIL")) {
                        email = value.trim()
                    } else if (keyPart.startsWith("ADR")) {
                        address = value.replace(";", " ").trim()
                    }
                }
            }
        }

        if (name.isEmpty()) name = "Новый контакт"

        return QRParsedResult(
            type = QRType.CONTACT,
            rawValue = text,
            title = name,
            extractedData = mapOf("name" to name, "phone" to phone, "email" to email, "address" to address)
        )
    }

    private fun parseCalendar(text: String): QRParsedResult {
        var summary = ""
        var dtstart = ""
        var dtend = ""
        var location = ""
        var description = ""

        val lines = text.split('\n')
        for (line in lines) {
            val cleanLine = line.trim()
            val colonIndex = cleanLine.indexOf(':')
            if (colonIndex != -1 && colonIndex < cleanLine.length - 1) {
                val key = cleanLine.substring(0, colonIndex).uppercase()
                val value = processVValue(key, cleanLine.substring(colonIndex + 1))
                when {
                    key.startsWith("SUMMARY") -> summary = value.trim()
                    key.startsWith("DTSTART") -> dtstart = value.trim()
                    key.startsWith("DTEND") -> dtend = value.trim()
                    key.startsWith("LOCATION") -> location = value.trim()
                    key.startsWith("DESCRIPTION") -> description = value.trim()
                }
            }
        }
        if (summary.isEmpty()) summary = "Календарное событие"
        return QRParsedResult(
            type = QRType.CALENDAR,
            rawValue = text,
            title = summary,
            extractedData = mapOf(
                "summary" to summary,
                "dtstart" to dtstart,
                "dtend" to dtend,
                "location" to location,
                "description" to description
            )
        )
    }

    private fun isGeo(text: String): Boolean {
        // geo:lat,lon
        val trimmed = text.substring(if (text.startsWith("geo:", ignoreCase = true)) 4 else 0)
        val latLon = trimmed.split('?')[0].split(',')
        if (latLon.size >= 2) {
            val lat = latLon[0].trim().toDoubleOrNull()
            val lon = latLon[1].trim().toDoubleOrNull()
            if (lat != null && lon != null) {
                return lat in -90.0..90.0 && lon in -180.0..180.0
            }
        }
        return false
    }

    private fun parseGeo(text: String): QRParsedResult {
        var lat = "0.0"
        var lon = "0.0"
        val trimmed = if (text.startsWith("geo:", ignoreCase = true)) text.substring(4) else text
        val parts = trimmed.split('?')[0].split(',')
        if (parts.size >= 2) {
            lat = parts[0].trim()
            lon = parts[1].trim()
        }
        return QRParsedResult(
            type = QRType.GEO,
            rawValue = text,
            title = "$lat, $lon",
            extractedData = mapOf("latitude" to lat, "longitude" to lon)
        )
    }

    private fun isAddress(text: String): Boolean {
        if (text.length > 150 || text.isEmpty() || text.contains('\n')) return false
        val lower = text.lowercase()
        val addrKeywords = listOf(
            "ул.", "улица", "проспект", "пр-кт", "пр.", "пер.", "переулок", "бульвар", "б-р", "шоссе", "ш.",
            "дом ", "д.", "корпус", "к.", "область", "обл.", "г.", "город ", "квартал", "площадь", "пл.",
            "street", "st.", "road", "rd.", "avenue", "ave.", "building", "house", "zip code", "city"
        )
        return addrKeywords.any { lower.contains(it) }
    }

    private fun parseAddress(text: String): QRParsedResult {
        return QRParsedResult(
            type = QRType.ADDRESS,
            rawValue = text,
            title = if (text.length > 25) text.take(25) + "..." else text,
            extractedData = mapOf("address" to text)
        )
    }

    private fun parseText(text: String): QRParsedResult {
        val title = if (text.length > 25) text.take(25) + "..." else text
        return QRParsedResult(
            type = QRType.TEXT,
            rawValue = text,
            title = title.ifEmpty { "Пустой текст" }
        )
    }

    private fun decodeQuotedPrintable(input: String, charsetName: String = "UTF-8"): String {
        val baos = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '=') {
                if (i + 1 < input.length && input[i + 1] == '\r') {
                    if (i + 2 < input.length && input[i + 2] == '\n') {
                        i += 3
                        continue
                    }
                } else if (i + 1 < input.length && input[i + 1] == '\n') {
                    i += 2
                    continue
                }
                if (i + 2 < input.length) {
                    val hex1 = input[i + 1]
                    val hex2 = input[i + 2]
                    val digit1 = java.lang.Character.digit(hex1, 16)
                    val digit2 = java.lang.Character.digit(hex2, 16)
                    if (digit1 != -1 && digit2 != -1) {
                        val b = ((digit1 shl 4) + digit2).toByte()
                        baos.write(b.toInt())
                        i += 3
                        continue
                    }
                }
            }
            baos.write(c.code)
            i++
        }
        return try {
            baos.toString(charsetName)
        } catch (e: Exception) {
            baos.toString()
        }
    }

    private fun processVValue(key: String, value: String): String {
        val isQP = key.contains("ENCODING=QUOTED-PRINTABLE") || key.contains("ENCODING=QP")
        val decoded = if (isQP) decodeQuotedPrintable(value) else value
        return decoded
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
            .trim()
    }
}
