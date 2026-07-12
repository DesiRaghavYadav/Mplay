package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NotificationAnalyticsMetric
import com.example.data.model.NotificationHistoryItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MPlayViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MPlayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val token by viewModel.fcmToken.collectAsStateWithLifecycle()
    val history by viewModel.notificationHistory.collectAsStateWithLifecycle()
    val analytics by viewModel.notificationAnalytics.collectAsStateWithLifecycle()
    val logs by viewModel.notificationDebugLogs.collectAsStateWithLifecycle()

    // Test Config Inputs
    var testTitle by remember { mutableStateOf("ReelsViral Hot Alert") }
    var testBody by remember { mutableStateOf("Deepika Padukone's latest Reel just went viral! Catch it before premium restrictions activate.") }
    var scheduleDelaySeconds by remember { mutableStateOf("5") }

    // Screen Sub-tabbing (Tester vs. System logs vs. Channels)
    var currentSubTab by remember { mutableStateOf(0) } // 0: Tester & History, 1: Analytics, 2: Channels & Logs

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_hero"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(RedPrimary, GoldTheme))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CircleNotifications,
                                contentDescription = "FCM System",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FCM Push Hub",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Production Push Notification Suite",
                                color = GrayText,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Device FCM Token",
                        color = GoldTheme,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141313))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = token,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(token))
                                Toast.makeText(context, "FCM Token Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                viewModel.addDebugLog("FCM Device Token copied to Clipboard.")
                            },
                            modifier = Modifier.size(32.dp).testTag("token_copy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Token",
                                tint = RedPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Sub Navigation Menu
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Tester & Hub", "Live Analytics", "Channels & Shell").forEachIndexed { index, label ->
                    val isActive = currentSubTab == index
                    Button(
                        onClick = { currentSubTab = index },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) RedPrimary else CardBackground,
                            contentColor = if (isActive) Color.White else GrayText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Branching Content Rendering based on SubTab selection
        when (currentSubTab) {
            0 -> {
                // TESTER INTERACTIVE CONTROL SECTION
                item {
                    Text(
                        text = "Custom Simulated Inputs",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = testTitle,
                                onValueChange = { testTitle = it },
                                label = { Text("Notification Title", color = GrayText) },
                                modifier = Modifier.fillMaxWidth().testTag("input_title"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedPrimary,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray
                                )
                            )

                            OutlinedTextField(
                                value = testBody,
                                onValueChange = { testBody = it },
                                label = { Text("Notification Body Text", color = GrayText) },
                                modifier = Modifier.fillMaxWidth().testTag("input_body"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedPrimary,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = scheduleDelaySeconds,
                                    onValueChange = { scheduleDelaySeconds = it },
                                    label = { Text("Delay (Sec)", color = GrayText) },
                                    modifier = Modifier.width(110.dp).testTag("input_delay"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RedPrimary,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray
                                    )
                                )

                                Button(
                                    onClick = {
                                        val secs = scheduleDelaySeconds.toIntOrNull() ?: 5
                                        viewModel.scheduleLocalReminder(
                                            delaySeconds = secs,
                                            title = testTitle,
                                            body = testBody,
                                            url = "reelsviral://details?tab=support"
                                        )
                                        Toast.makeText(context, "AlarmManager scheduled in $secs seconds!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldTheme),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(50.dp).testTag("schedule_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Alarm, contentDescription = "Alarm", tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Schedule Local", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Trigger Core Layout Layouts",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Grid of 18 Action Buttons
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2
                    ) {
                        val testerButtons = listOf(
                            Triple("basic", "Basic Text", Icons.Default.TextFields),
                            Triple("big_text", "Big Text Expanded", Icons.Default.FormatAlignLeft),
                            Triple("large_image", "Large Image URL", Icons.Default.Image),
                            Triple("image_text", "Image + Text Recap", Icons.Default.FilterFrames),
                            Triple("multiple_images", "Carousel Fallback", Icons.Default.ViewCarousel),
                            Triple("inbox", "Inbox Lines Style", Icons.Default.Inbox),
                            Triple("chat", "Chat Quick Reply", Icons.Default.QuestionAnswer),
                            Triple("progress", "Download Progress", Icons.Default.HourglassBottom),
                            Triple("media", "Media Controls Bar", Icons.Default.PlayCircle),
                            Triple("persistent", "Ongoing Lock", Icons.Default.Lock),
                            Triple("foreground_service", "Foreground Service", Icons.Default.RoomService),
                            Triple("heads_up", "Heads-Up High Priority", Icons.Default.Warning),
                            Triple("silent", "Silent Non-Intrusive", Icons.Default.NotificationsOff),
                            Triple("full_screen", "Full Screen Call", Icons.Default.Fullscreen),
                            Triple("deep_link", "Deep Link Explore", Icons.Default.Link),
                            Triple("action_button", "Interactive Actions", Icons.Default.SmartButton),
                            Triple("grouped", "Grouped alerts stack", Icons.Default.Splitscreen),
                            Triple("rich", "Rich Combo Bundle", Icons.Default.Stars)
                        )

                        testerButtons.forEach { (type, label, icon) ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val img = when (type) {
                                            "large_image", "image" -> "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=600&q=80"
                                            "image_text" -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80"
                                            "multiple_images" -> "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=600&q=80"
                                            "rich" -> "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80"
                                            else -> null
                                        }
                                        val action = when (type) {
                                            "deep_link" -> "deep_link"
                                            "action_button" -> "in_app_url"
                                            else -> "open_app"
                                        }
                                        val targetUrl = when (type) {
                                            "deep_link" -> "reelsviral://details?tab=explore"
                                            "action_button" -> "https://reelsviral.example.com/promotions"
                                            else -> null
                                        }

                                        viewModel.triggerLocalSimulatorNotification(
                                            type = type,
                                            title = "$label Layout",
                                            body = testBody,
                                            imageUrl = img,
                                            action = action,
                                            url = targetUrl
                                        )
                                    }
                                    .testTag("test_trigger_$type"),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = RedPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // DATABASE PERSISTENT NOTIFICATION HISTORY REGISTERED
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Room Persistent History (${history.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (history.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.clearNotificationHistory() },
                                modifier = Modifier.testTag("clear_history_btn")
                            ) {
                                Text("Clear All", color = RedPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (history.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141313)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = "Empty",
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No recorded pushes in Room database yet. Trigger a simulated test notification layout above!",
                                        color = GrayText,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(history) { item ->
                        HistoryItemRow(
                            item = item,
                            onUpdateStatus = { id, status -> viewModel.updateHistoryStatus(id, status) }
                        )
                    }
                }
            }

            1 -> {
                // LIVE ANALYTICS METRICS DISPLAY
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notification Lifetime Metrics",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.clearNotificationAnalytics() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Analytics", tint = RedPrimary)
                        }
                    }
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val metricsMap = analytics.associate { it.metricId to it.count }
                        val d = metricsMap["delivered"] ?: 0
                        val o = metricsMap["opened"] ?: 0
                        val click = metricsMap["action_clicked"] ?: 0
                        val dis = metricsMap["dismissed"] ?: 0
                        val f = metricsMap["failed"] ?: 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AnalyticCard(title = "Delivered", count = d, color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
                            AnalyticCard(title = "Opened", count = o, color = Color(0xFF2196F3), modifier = Modifier.weight(1f))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AnalyticCard(title = "Action Click", count = click, color = Color(0xFFFF9800), modifier = Modifier.weight(1f))
                            AnalyticCard(title = "Swiped Out", count = dis, color = Color(0xFF9E9E9E), modifier = Modifier.weight(1f))
                        }

                        AnalyticCard(title = "Failed Requests", count = f, color = Color(0xFFE91E63), modifier = Modifier.fillMaxWidth())

                        // Mini visual presentation percentages
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Interactivity Conversions (CTR)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val ctr = if (d > 0) ((o + click).toFloat() / d.toFloat() * 100).coerceAtMost(100f) else 0f
                                Text(
                                    text = "Active click/open rate is ${String.format("%.1f", ctr)}%",
                                    color = GoldTheme,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                LinearProgressIndicator(
                                    progress = ctr / 100f,
                                    color = GoldTheme,
                                    trackColor = Color.DarkGray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // SYSTEM CHANNELS & REALTIME DEBUG SHELL
                item {
                    Text(
                        text = "OS Notification Channels Registered",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val channels = listOf(
                                Pair("General Alerts", "Standard system updates and status messages"),
                                Pair("Promotions & Offers", "Deals, premium releases, and trending content"),
                                Pair("Downloads", "Offline media download progress"),
                                Pair("Uploads", "Creator file uploads and assets progress"),
                                Pair("Chat Messages", "Direct chat requests and replies"),
                                Pair("Now Playing", "Active media control notifications"),
                                Pair("Important Alerts", "Immediate account alerts, critical updates")
                            )

                            channels.forEach { (name, desc) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(text = desc, color = GrayText, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "FCM Realtime Console Shell",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        border = BorderStroke(1.dp, Color(0xFF221F1F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "active_adb_shell_listener",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    logs.forEach { log ->
                                        Text(
                                            text = log,
                                            color = Color.LightGray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun AnalyticCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = GrayText, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                color = color,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HistoryItemRow(
    item: NotificationHistoryItem,
    onUpdateStatus: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF221F1F)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (item.type) {
                            "chat", "messaging" -> Icons.Default.Chat
                            "image", "large_image", "image_text" -> Icons.Default.Image
                            "media" -> Icons.Default.PlayArrow
                            "progress" -> Icons.Default.Download
                            "scheduled" -> Icons.Default.AccessTime
                            else -> Icons.Default.Notifications
                        }
                        Icon(imageVector = icon, contentDescription = "notif_type", tint = GoldTheme, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.body,
                            color = GrayText,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    val timeStr = remember(item.timestamp) {
                        try {
                            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))
                        } catch (e: Exception) {
                            "06:15"
                        }
                    }
                    Text(
                        text = timeStr,
                        color = GrayText,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    item.status.startsWith("Delivered") -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    item.status.contains("Opened") -> Color(0xFF2196F3).copy(alpha = 0.15f)
                                    item.status.contains("Replied") -> Color(0xFFE91E63).copy(alpha = 0.15f)
                                    item.status.contains("Action") -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                    else -> Color.DarkGray
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.status,
                            fontSize = 9.sp,
                            color = when {
                                item.status.startsWith("Delivered") -> Color(0xFF4CAF50)
                                item.status.contains("Opened") -> Color(0xFF2196F3)
                                item.status.contains("Replied") -> Color(0xFFE91E63)
                                item.status.contains("Action") -> Color(0xFFFF9800)
                                else -> Color.LightGray
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expose the raw JSON FCM Payload on click/expansion or as helper detail
            var isPayloadExpanded by remember { mutableStateOf(false) }
            if (isPayloadExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black)
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.payloadJson,
                        color = Color(0xFF4CAF50),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFF221F1F))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isPayloadExpanded = !isPayloadExpanded },
                    colors = ButtonDefaults.textButtonColors(contentColor = GoldTheme),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (isPayloadExpanded) "Hide Details (FCM JSON)" else "Show Details (FCM JSON)", fontSize = 10.sp)
                }

                if (item.status.startsWith("Delivered")) {
                    TextButton(
                        onClick = { onUpdateStatus(item.id, "Opened") },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF2196F3)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Mark Read", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
