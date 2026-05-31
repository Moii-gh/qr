package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qr.QRAction
import com.example.qr.QRActionFactory
import com.example.qr.QRType
import com.example.qr.QRTypeDetector
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    rawValue: String,
    type: Int, // Received from older navigation, but we'll use auto detector
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = true

    // Dynamically detect type and details
    val parsedResult = remember(rawValue) { QRTypeDetector.detect(rawValue) }
    val actions = remember(parsedResult) { QRActionFactory.getActions(parsedResult) }

    // State for Wi-Fi password visibility
    var passwordVisible by remember { mutableStateOf(false) }

    // Delay entry animations slightly
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    GlassBackground(isDark = isDark) {
        Column(
            modifier = Modifier
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
                        isDark = isDark,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Результат сканирования",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Автоматическое определение",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Details layout
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main Content Card
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(500)) + expandVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .border(
                                1.2.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                                ),
                                RoundedCornerShape(36.dp)
                            ),
                        cornerRadius = 36.dp,
                        isDark = isDark
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Category Badge with corresponding auto-detected type icon
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = parsedResult.type.getIcon(),
                                        contentDescription = parsedResult.type.labelRu,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = parsedResult.type.labelRu,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Nice formatted representation of parsed data fields
                            when (parsedResult.type) {
                                QRType.URL -> {
                                    DetailField(label = "Ссылка", value = parsedResult.title, isMonospace = true)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = rawValue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                QRType.SMS -> {
                                    val phone = parsedResult.extractedData["phone"] ?: ""
                                    val body = parsedResult.extractedData["body"] ?: ""
                                    DetailField(label = "Номер телефона", value = phone)
                                    if (body.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Текст сообщения", value = body)
                                    }
                                }
                                QRType.EMAIL -> {
                                    val email = parsedResult.extractedData["email"] ?: ""
                                    val subject = parsedResult.extractedData["subject"] ?: ""
                                    val body = parsedResult.extractedData["body"] ?: ""
                                    DetailField(label = "Email", value = email)
                                    if (subject.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Тема", value = subject)
                                    }
                                    if (body.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Письмо", value = body)
                                    }
                                }
                                QRType.WIFI -> {
                                    val ssid = parsedResult.extractedData["ssid"] ?: ""
                                    val password = parsedResult.extractedData["password"] ?: ""
                                    val security = parsedResult.extractedData["security"] ?: ""
                                    DetailField(label = "Сеть Wi-Fi (SSID)", value = ssid)
                                    if (security.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Защита", value = security)
                                    }
                                    if (password.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(
                                                text = "Пароль сети",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (passwordVisible) password else "••••••••",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                                    color = Color.White
                                                )
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Показать/Скрыть",
                                                    tint = Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable { passwordVisible = !passwordVisible }
                                                )
                                            }
                                        }
                                    }
                                }
                                QRType.CONTACT -> {
                                    val name = parsedResult.extractedData["name"] ?: ""
                                    val phone = parsedResult.extractedData["phone"] ?: ""
                                    val email = parsedResult.extractedData["email"] ?: ""
                                    val address = parsedResult.extractedData["address"] ?: ""
                                    DetailField(label = "Контактное лицо", value = name)
                                    if (phone.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Мобильный", value = phone)
                                    }
                                    if (email.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Эл. почта", value = email)
                                    }
                                    if (address.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Адрес", value = address)
                                    }
                                }
                                QRType.CALENDAR -> {
                                    val summary = parsedResult.extractedData["summary"] ?: ""
                                    val location = parsedResult.extractedData["location"] ?: ""
                                    val description = parsedResult.extractedData["description"] ?: ""
                                    val dtstart = parsedResult.extractedData["dtstart"] ?: ""
                                    val dtend = parsedResult.extractedData["dtend"] ?: ""

                                    DetailField(label = "Событие", value = summary)
                                    
                                    if (dtstart.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Начало", value = formatICalendarDate(dtstart))
                                    }
                                    if (dtend.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Окончание", value = formatICalendarDate(dtend))
                                    }
                                    if (location.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Место встречи", value = location)
                                    }
                                    if (description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        DetailField(label = "Описание", value = description)
                                    }
                                }
                                QRType.GEO -> {
                                    val lat = parsedResult.extractedData["latitude"] ?: ""
                                    val lon = parsedResult.extractedData["longitude"] ?: ""
                                    DetailField(label = "Географические координаты", value = "$lat, $lon")
                                }
                                QRType.PHONE -> {
                                    DetailField(label = "Номер телефона", value = parsedResult.title)
                                }
                                QRType.ADDRESS -> {
                                    DetailField(label = "Найденный адрес", value = parsedResult.title)
                                }
                                else -> {
                                    // Plain Text
                                    DetailField(label = "Текст", value = rawValue, isMonospace = true)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // High fidelity responsive buttons representing actions
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(700, 200)) + slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Flow row that spaces actions beautifully or dynamically
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            maxItemsInEachRow = 3
                        ) {
                            actions.forEach { action ->
                                ActionItemButton(
                                    action = action,
                                    onClick = { action.execute(context) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DetailField(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                fontSize = if (value.length > 50) 14.sp else 16.sp,
                lineHeight = 22.sp
            ),
            color = Color.White
        )
    }
}

@Composable
fun ActionItemButton(
    action: QRAction,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    if (action.isPrimary) Color.White else Color.White.copy(alpha = 0.08f)
                )
                .clickable { onClick() }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (action.isPrimary) 0.35f else 0.18f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                modifier = Modifier.size(20.dp),
                tint = if (action.isPrimary) Color.Black else Color.White
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp
            ),
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

private fun formatICalendarDate(dateStr: String): String {
    if (dateStr.isEmpty()) return ""
    try {
        val clean = dateStr.replace("T", "").replace("Z", "")
        if (clean.length >= 8) {
            val format = SimpleDateFormat(
                if (clean.length >= 14) "yyyyMMddHHmmss" else "yyyyMMdd",
                Locale("ru")
            )
            val date = format.parse(clean) ?: return dateStr
            val outputFormat = if (clean.length >= 14) {
                SimpleDateFormat("EEEE, d MMMM yyyy г., HH:mm", Locale("ru"))
            } else {
                SimpleDateFormat("EEEE, d MMMM yyyy г.", Locale("ru"))
            }
            return outputFormat.format(date).replaceFirstChar { it.uppercase() }
        }
    } catch (e: Exception) {
        // fallback
    }
    return dateStr
}
