package com.example.qr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

object QRActionFactory {

    fun getActions(result: QRParsedResult): List<QRAction> {
        val actions = mutableListOf<QRAction>()

        when (result.type) {
            QRType.URL -> {
                val url = result.extractedData["url"] ?: result.rawValue
                actions.add(QRAction("Открыть ссылку", Icons.Default.Language, isPrimary = true) { context ->
                    openUrl(context, url)
                })
                actions.add(QRAction("Копировать", Icons.Default.ContentCopy) { context ->
                    copyToClipboard(context, "URL", url)
                })
            }

            QRType.TEXT -> {
                actions.add(QRAction("Копировать", Icons.Default.ContentCopy, isPrimary = true) { context ->
                    copyToClipboard(context, "Текст", result.rawValue)
                })
                actions.add(QRAction("Поделиться", Icons.Default.Share) { context ->
                    shareText(context, result.rawValue)
                })
            }

            QRType.PHONE -> {
                val phone = result.extractedData["phone"] ?: result.rawValue
                actions.add(QRAction("Позвонить", Icons.Default.Phone, isPrimary = true) { context ->
                    dialPhone(context, phone)
                })
                actions.add(QRAction("В контакты", Icons.Default.ContactPage) { context ->
                    addContact(context, "", phone, "", "")
                })
                actions.add(QRAction("Копировать", Icons.Default.ContentCopy) { context ->
                    copyToClipboard(context, "Телефон", phone)
                })
            }

            QRType.SMS -> {
                val phone = result.extractedData["phone"] ?: ""
                val body = result.extractedData["body"] ?: ""
                actions.add(QRAction("Написать SMS", Icons.AutoMirrored.Filled.Message, isPrimary = true) { context ->
                    sendSms(context, phone, body)
                })
                actions.add(QRAction("Копировать", Icons.Default.ContentCopy) { context ->
                    copyToClipboard(context, "Текст SMS", body.ifEmpty { result.rawValue })
                })
            }

            QRType.EMAIL -> {
                val email = result.extractedData["email"] ?: ""
                val subject = result.extractedData["subject"] ?: ""
                val body = result.extractedData["body"] ?: ""
                actions.add(QRAction("Написать", Icons.Default.Email, isPrimary = true) { context ->
                    sendEmail(context, email, subject, body)
                })
                actions.add(QRAction("Копировать email", Icons.Default.ContentCopy) { context ->
                    copyToClipboard(context, "Email", email)
                })
            }

            QRType.WIFI -> {
                val ssid = result.extractedData["ssid"] ?: ""
                val password = result.extractedData["password"] ?: ""
                val security = result.extractedData["security"] ?: ""

                if (password.isNotEmpty()) {
                    actions.add(QRAction("Копировать пароль", Icons.Default.VpnKey, isPrimary = true) { context ->
                        copyToClipboard(context, "Wi-Fi Пароль", password)
                    })
                }
                actions.add(QRAction("Настройки Wi-Fi", Icons.Default.Wifi) { context ->
                    openWifiSettings(context)
                })
                actions.add(QRAction("Копировать данные", Icons.Default.ContentCopy) { context ->
                    val wifiData = "SSID: $ssid\nПароль: $password\nЗащита: $security"
                    copyToClipboard(context, "Wi-Fi Сеть", wifiData)
                })
            }

            QRType.CONTACT -> {
                val name = result.extractedData["name"] ?: ""
                val phone = result.extractedData["phone"] ?: ""
                val email = result.extractedData["email"] ?: ""
                val address = result.extractedData["address"] ?: ""

                actions.add(QRAction("Добавить контакт", Icons.Default.ContactPage, isPrimary = true) { context ->
                    addContact(context, name, phone, email, address)
                })
                if (phone.isNotEmpty()) {
                    actions.add(QRAction("Позвонить", Icons.Default.Phone) { context ->
                        dialPhone(context, phone)
                    })
                }
                if (email.isNotEmpty()) {
                    actions.add(QRAction("Написать email", Icons.Default.Email) { context ->
                        sendEmail(context, email, "", "")
                    })
                }
            }

            QRType.ADDRESS -> {
                val address = result.extractedData["address"] ?: result.rawValue
                actions.add(QRAction("Открыть в картах", Icons.Default.Map, isPrimary = true) { context ->
                    openInMap(context, address)
                })
                actions.add(QRAction("Копировать адрес", Icons.Default.ContentCopy) { context ->
                    copyToClipboard(context, "Адрес", address)
                })
            }

            QRType.CALENDAR -> {
                val summary = result.extractedData["summary"] ?: ""
                val dtstart = result.extractedData["dtstart"] ?: ""
                val dtend = result.extractedData["dtend"] ?: ""
                val location = result.extractedData["location"] ?: ""
                val description = result.extractedData["description"] ?: ""

                actions.add(QRAction("В календарь", Icons.Default.CalendarToday, isPrimary = true) { context ->
                    addCalendarEvent(context, summary, dtstart, dtend, location, description)
                })
                actions.add(QRAction("Копировать", Icons.Default.ContentCopy) { context ->
                    copyToClipboard(context, "Событие", summary)
                })
            }

            QRType.GEO -> {
                val lat = result.extractedData["latitude"] ?: ""
                val lon = result.extractedData["longitude"] ?: ""
                actions.add(QRAction("Открыть карты", Icons.Default.Map, isPrimary = true) { context ->
                    openCoords(context, lat, lon)
                })
                actions.add(QRAction("Копировать", Icons.Default.ContentCopy) { context ->
                    copyToClipboard(context, "Координаты", "$lat, $lon")
                })
            }

            QRType.UNKNOWN -> {
                actions.add(QRAction("Копировать", Icons.Default.ContentCopy, isPrimary = true) { context ->
                    copyToClipboard(context, "Данные", result.rawValue)
                })
                actions.add(QRAction("Поделиться", Icons.Default.Share) { context ->
                    shareText(context, result.rawValue)
                })
            }
        }

        return actions
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться"))
    }

    private fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка открытия ссылки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dialPhone(context: Context, phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть номеронабиратель", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSms(context: Context, phone: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                if (body.isNotEmpty()) {
                    putExtra("sms_body", body)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось отправить SMS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail(context: Context, email: String, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                if (subject.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, subject)
                if (body.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Почтовые приложения не найдены", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addContact(context: Context, name: String, phone: String, email: String, address: String) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                if (name.isNotEmpty()) {
                    putExtra(ContactsContract.Intents.Insert.NAME, name)
                }
                if (phone.isNotEmpty()) {
                    putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                }
                if (email.isNotEmpty()) {
                    putExtra(ContactsContract.Intents.Insert.EMAIL, email)
                }
                if (address.isNotEmpty()) {
                    putExtra(ContactsContract.Intents.Insert.POSTAL, address)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть добавление контакта", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInMap(context: Context, query: String) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedQuery"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Карты не найдены на устройстве", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCoords(context: Context, lat: String, lon: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Карты не найдены на устройстве", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWifiSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось запустить настройки Wi-Fi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addCalendarEvent(context: Context, summary: String, dtstart: String, dtend: String, location: String, description: String) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, summary)
                if (location.isNotEmpty()) putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                if (description.isNotEmpty()) putExtra(CalendarContract.Events.DESCRIPTION, description)

                val startMillis = parseCalendarDate(dtstart) ?: System.currentTimeMillis()
                val endMillis = parseCalendarDate(dtend) ?: (startMillis + 60 * 60 * 1000)

                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Приложение календаря не найдено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseCalendarDate(dateStr: String): Long? {
        if (dateStr.isEmpty()) return null
        return try {
            val clean = dateStr.replace("T", "").replace("Z", "")
            if (clean.length >= 8) {
                val format = SimpleDateFormat(
                    if (clean.length >= 14) "yyyyMMddHHmmss" else "yyyyMMdd",
                    Locale.US
                )
                format.parse(clean)?.time
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
