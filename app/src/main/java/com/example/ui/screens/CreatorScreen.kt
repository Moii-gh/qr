package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qr.QRType
import com.example.qr.QRParsedResult
import com.example.qr.QRTypeDetector
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.OutputStream

data class QRTemplate(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val placeholderValue: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Single unified smart input for automatic type detection
    var inputText by remember { mutableStateOf("") }

    // List of templates to help user easily create special QR types
    val qrTemplates = remember {
        listOf(
            QRTemplate("Ссылка", Icons.Default.Language, "https://google.com", "Адрес веб-сайта"),
            QRTemplate("Wi-Fi Сеть", Icons.Default.Wifi, "WIFI:S:MyNetworkName;T:WPA;P:myPassword;;", "Данные подключения к Wi-Fi"),
            QRTemplate("Телефон", Icons.Default.Phone, "+79991234567", "Прямой вызов номера телефона"),
            QRTemplate("SMS", Icons.AutoMirrored.Filled.Message, "sms:+79991234567?body=%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82", "Отправка готового сообщения"),
            QRTemplate("Email", Icons.Default.Email, "mailto:test@example.com?subject=%D0%A2%D0%B5%D0%BC%D0%B0&body=%D0%A2%D0%B5%D0%BA%D1%81%D1%82", "Отправка электронного письма"),
            QRTemplate("vCard Контакт", Icons.Default.ContactPage, "BEGIN:VCARD\nVERSION:3.0\nFN:Иван Петров\nTEL:+79991234567\nEMAIL:ivan@example.com\nADR:;;г. Москва, ул. Ленина, д. 1;;;;\nEND:VCARD", "Визитная карта vCard"),
            QRTemplate("Событие", Icons.Default.CalendarToday, "BEGIN:VEVENT\nSUMMARY:Встреча\nLOCATION:Офис\nDESCRIPTION:Обсуждение проекта\nEND:VEVENT", "Событие календаря vEvent"),
            QRTemplate("Геопозиция", Icons.Default.Map, "geo:55.7558,37.6173", "Координаты на карте")
        )
    }

    // Run real-time type detection and format standard keys
    val detectedResult = remember(inputText) {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            QRParsedResult(QRType.UNKNOWN, "", "Пустой QR-код")
        } else {
            QRTypeDetector.detect(trimmed)
        }
    }

    // Prepare optimized payload string to encode in QR matrix
    val derivedQrContent = remember(inputText, detectedResult) {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return@remember ""

        when (detectedResult.type) {
            QRType.URL -> {
                detectedResult.extractedData["url"] ?: trimmed
            }
            QRType.PHONE -> {
                if (trimmed.startsWith("tel:", ignoreCase = true)) {
                    trimmed
                } else {
                    "tel:${trimmed.replace(" ", "").replace("-", "")}"
                }
            }
            QRType.EMAIL -> {
                if (trimmed.startsWith("mailto:", ignoreCase = true)) {
                    trimmed
                } else {
                    "mailto:${trimmed.trim()}"
                }
            }
            QRType.GEO -> {
                if (trimmed.startsWith("geo:", ignoreCase = true)) {
                    trimmed
                } else {
                    "geo:${trimmed.replace(" ", "")}"
                }
            }
            else -> trimmed
        }
    }

    // Real-time generated QR matrix bitmap
    val qrBitmap = remember(derivedQrContent) {
        if (derivedQrContent.isNotEmpty()) {
            generateQrCode(derivedQrContent)
        } else {
            null
        }
    }

    GlassBackground(isDark = true) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        onClick = onBack,
                        isDark = true,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Умный QR Генератор",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Автоматическое определение типа данных",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main inputs card wrapper
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.88f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive dynamic configuration panel
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title for input field
                    Text(
                        text = "Введите ваши данные ниже",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    // Unified multi-line smart text field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp, max = 180.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        placeholder = {
                            Text(
                                "Вставьте ссылку, номер телефона, email, адрес или воспользуйтесь шаблонами ниже...",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 14.sp),
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(onClick = { inputText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Очистить",
                                        tint = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Default,
                            keyboardType = KeyboardType.Text
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color.White
                        )
                    )

                    // Autodetected Type Indicator Badge
                    if (inputText.trim().isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .align(Alignment.End)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = detectedResult.type.getIcon(),
                                contentDescription = detectedResult.type.labelRu,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Тип: ${detectedResult.type.labelRu}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    // Display the extracted structured details helper
                    AnimatedVisibility(
                        visible = inputText.trim().isNotEmpty() && detectedResult.type != QRType.UNKNOWN && detectedResult.type != QRType.TEXT,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        DetectedTypeDetails(parsed = detectedResult)
                    }
                }

                // Horizontal templates panel helper
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Быстрые шаблоны форматов:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(qrTemplates) { template ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .clickable {
                                        inputText = template.placeholderValue
                                        focusManager.clearFocus()
                                    }
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = template.icon,
                                        contentDescription = template.label,
                                        tint = Color.White.copy(alpha = 0.55f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = template.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Real-time generated Showcase Canvas Card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .border(
                            1.2.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            RoundedCornerShape(36.dp)
                        ),
                    cornerRadius = 36.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Generated QR Code",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White)
                                    .padding(12.dp)
                            )
                        } else {
                            // Empty State placeholder
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = Color.White.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Превью вашего QR-кода",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Введите данные выше для превью",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Export & Share buttons
                if (qrBitmap != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val success = saveQrToGallery(context, qrBitmap)
                                if (success) {
                                    Toast.makeText(context, "Изображение сохранено в галерею!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Не удалось сохранить файл", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Скачать",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Скачать", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Button(
                            onClick = {
                                shareQrBitmap(context, qrBitmap)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Поделиться",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Поделиться", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DetectedTypeDetails(parsed: QRParsedResult) {
    if (parsed.type == QRType.UNKNOWN || parsed.type == QRType.TEXT) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Распознанные параметры:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(6.dp))

        when (parsed.type) {
            QRType.URL -> {
                val url = parsed.extractedData["url"] ?: parsed.rawValue
                DetailRow("Ссылка", url)
            }
            QRType.PHONE -> {
                val phone = parsed.extractedData["phone"] ?: parsed.rawValue
                DetailRow("Телефон", phone)
            }
            QRType.SMS -> {
                val phone = parsed.extractedData["phone"] ?: ""
                val body = parsed.extractedData["body"] ?: ""
                DetailRow("Связь", phone)
                if (body.isNotEmpty()) DetailRow("Текст SMS", body)
            }
            QRType.EMAIL -> {
                val email = parsed.extractedData["email"] ?: ""
                val subject = parsed.extractedData["subject"] ?: ""
                val body = parsed.extractedData["body"] ?: ""
                DetailRow("Адрес", email)
                if (subject.isNotEmpty()) DetailRow("Тема", subject)
                if (body.isNotEmpty()) DetailRow("Письмо", body)
            }
            QRType.WIFI -> {
                val ssid = parsed.extractedData["ssid"] ?: ""
                val password = parsed.extractedData["password"] ?: ""
                val security = parsed.extractedData["security"] ?: ""
                DetailRow("Имя сети (SSID)", ssid)
                if (password.isNotEmpty()) DetailRow("Пароль", password)
                if (security.isNotEmpty()) DetailRow("Защита", security)
            }
            QRType.CONTACT -> {
                val name = parsed.extractedData["name"] ?: ""
                val phone = parsed.extractedData["phone"] ?: ""
                val email = parsed.extractedData["email"] ?: ""
                val address = parsed.extractedData["address"] ?: ""
                DetailRow("Имя контакта", name)
                if (phone.isNotEmpty()) DetailRow("Номер телефона", phone)
                if (email.isNotEmpty()) DetailRow("Эл. почта", email)
                if (address.isNotEmpty()) DetailRow("Физич. адрес", address)
            }
            QRType.CALENDAR -> {
                val summary = parsed.extractedData["summary"] ?: ""
                val location = parsed.extractedData["location"] ?: ""
                val description = parsed.extractedData["description"] ?: ""
                DetailRow("Событие", summary)
                if (location.isNotEmpty()) DetailRow("Место", location)
                if (description.isNotEmpty()) DetailRow("Описание", description)
            }
            QRType.GEO -> {
                val lat = parsed.extractedData["latitude"] ?: ""
                val lon = parsed.extractedData["longitude"] ?: ""
                DetailRow("Координаты", "$lat, $lon")
            }
            QRType.ADDRESS -> {
                val address = parsed.extractedData["address"] ?: parsed.rawValue
                DetailRow("Адрес", address)
            }
            else -> {}
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.weight(0.58f)
        )
    }
}

private fun generateQrCode(text: String, size: Int = 512): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val hints = java.util.EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java).apply {
            put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8")
            put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M)
        }
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

private fun saveQrToGallery(context: Context, bitmap: Bitmap): Boolean {
    val filename = "QR_${System.currentTimeMillis()}"
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.png")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRScanner")
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
    return try {
        val stream: OutputStream? = resolver.openOutputStream(uri)
        if (stream != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

private fun shareQrBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = java.io.File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = java.io.FileOutputStream("$cachePath/shared_qr_code.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imageFile = java.io.File(cachePath, "shared_qr_code.png")
        val contentUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            type = "image/png"
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться QR-кодом"))
    } catch (e: Exception) {
        Toast.makeText(context, "Не удалось поделиться QR: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
