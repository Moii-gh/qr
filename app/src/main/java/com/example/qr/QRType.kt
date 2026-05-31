package com.example.qr

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class QRType(val labelRu: String) {
    TEXT("Текст"),
    URL("Ссылка URL"),
    PHONE("Номер телефона"),
    SMS("SMS-сообщение"),
    EMAIL("Электронная почта"),
    WIFI("Сеть Wi-Fi"),
    CONTACT("Контакт"),
    ADDRESS("Адрес"),
    CALENDAR("Календарь"),
    GEO("Геопозиция"),
    UNKNOWN("Текст / Неизвестно");

    fun getIcon(): ImageVector {
        return when (this) {
            TEXT -> Icons.Default.Description
            URL -> Icons.Default.Language
            PHONE -> Icons.Default.Phone
            SMS -> Icons.AutoMirrored.Filled.Message
            EMAIL -> Icons.Default.Email
            WIFI -> Icons.Default.Wifi
            CONTACT -> Icons.Default.ContactPage
            ADDRESS -> Icons.Default.Home
            CALENDAR -> Icons.Default.CalendarToday
            GEO -> Icons.Default.Map
            UNKNOWN -> Icons.Default.HelpOutline
        }
    }
}
