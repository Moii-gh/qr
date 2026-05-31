package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ScanResult
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassClickableCard
import com.example.ui.components.GlassIconButton
import com.google.mlkit.vision.barcode.common.Barcode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    scans: List<ScanResult>,
    onBack: () -> Unit,
    onItemClick: (ScanResult) -> Unit,
    onToggleFavorite: (Int, Boolean) -> Unit,
    onDelete: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Dynamically filter results in real-time
    val filteredScans = remember(scans, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            scans
        } else {
            scans.filter {
                val parsed = com.example.qr.QRTypeDetector.detect(it.rawValue)
                it.rawValue.contains(searchQuery, ignoreCase = true) ||
                        parsed.title.contains(searchQuery, ignoreCase = true) ||
                        parsed.type.labelRu.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Auto-request keyboard focus when search expands
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(vertical = 12.dp)
        ) {
            
            // Header Row holding back, title, and search panel on the same level
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Animated header content transitions between regular view and search state
                AnimatedContent(
                    targetState = isSearchActive,
                    transitionSpec = {
                        if (targetState) {
                            // Expand right-to-left slide animation
                            (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(200))
                            )
                        } else {
                            // Collapse back left-to-right
                            (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(200))
                            )
                        }
                    },
                    label = "HeaderTransition"
                ) { searchActive ->
                    if (searchActive) {
                        // FULL WIDTH search panel text field (На все поле панель поиска)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .focusRequester(focusRequester)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.22f),
                                            Color.White.copy(alpha = 0.05f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            placeholder = {
                                Text(
                                    text = "Поиск истории...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            leadingIcon = {
                                IconButton(
                                    onClick = {
                                        isSearchActive = false
                                        searchQuery = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Закрыть поиск",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Очистить",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        // Normal mode header with matching height parameters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // High-end smaller back button (size 34.dp)
                            GlassIconButton(
                                onClick = onBack,
                                isDark = true,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            // History and count labels
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "История",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Всего отсканировано: ${scans.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            
                            // Beautiful circular search button on the right
                            GlassIconButton(
                                onClick = { isSearchActive = true },
                                isDark = true,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Открыть поиск",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Log Entries
            if (filteredScans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "История пуста",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Попробуйте изменить запрос" else "Сканируйте коды при помощи камеры",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredScans, key = { it.id }) { scan ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    )
                                )
                        ) {
                            ScanHistoryItem(
                                scan = scan,
                                onClick = { onItemClick(scan) },
                                onToggleFavorite = { onToggleFavorite(scan.id, scan.isFavorite) },
                                onDelete = { onDelete(scan.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanHistoryItem(
    scan: ScanResult,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    // Automatically parse raw QR string on the fly for high fidelity categorisation
    val parsedResult = remember(scan.rawValue) { com.example.qr.QRTypeDetector.detect(scan.rawValue) }
    val icon = parsedResult.type.getIcon()
    val typeLabel = parsedResult.type.labelRu
    val displayTitle = parsedResult.title

    val date = remember(scan.createdAt, scan.timestamp) {
        val actualTime = if (scan.createdAt > 0L) scan.createdAt else scan.timestamp
        formatScanDate(actualTime)
    }

    // Centering Box wrapper that sets the pill narrower (not as wide: fillMaxWidth(0.76f) or 0.78f)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        GlassClickableCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth(0.78f) // Highly sleek and narrow premium look
                .height(72.dp),
            cornerRadius = 36.dp // Perfectly round pill edges for 72.dp height
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact icon bubble
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Raw information payload
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Quick compact actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (scan.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (scan.isFavorite) Color(0xFFEF4444) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatScanDate(timestamp: Long): String {
    val timeToUse = if (timestamp <= 0L) System.currentTimeMillis() else timestamp
    val date = java.util.Date(timeToUse)
    
    val now = java.util.Calendar.getInstance()
    val scanTime = java.util.Calendar.getInstance().apply { time = date }
    
    val isToday = now.get(java.util.Calendar.YEAR) == scanTime.get(java.util.Calendar.YEAR) &&
                  now.get(java.util.Calendar.DAY_OF_YEAR) == scanTime.get(java.util.Calendar.DAY_OF_YEAR)
                  
    val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(java.util.Calendar.YEAR) == scanTime.get(java.util.Calendar.YEAR) &&
                      yesterday.get(java.util.Calendar.DAY_OF_YEAR) == scanTime.get(java.util.Calendar.DAY_OF_YEAR)
                      
    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale("ru"))
    val fullDateFormat = java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm", java.util.Locale("ru"))
    
    return when {
        isToday -> "Сегодня, ${timeFormat.format(date)}"
        isYesterday -> "Вчера, ${timeFormat.format(date)}"
        else -> fullDateFormat.format(date)
    }
}
