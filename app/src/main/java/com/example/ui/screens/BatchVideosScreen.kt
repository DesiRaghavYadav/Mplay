package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MPlayViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Theme Colors
private val TopBarBgColor = Color(0xFF1E2833)
private val ScreenBgColor = Color(0xFF0F1720)
private val SimpleCardBgColor = Color(0xFF1A232E)
private val ChipActiveBgColor = Color(0xFF2AABEE)
private val ChipInactiveBgColor = Color(0xFF233040)
private val AccentBlue = Color(0xFF2AABEE)
private val TextMuted = Color(0xFF90A4AE)

/**
 * Filter options requested by user:
 * Latest (default), Oldest, Longest, Size
 */
enum class BatchSortFilter(val label: String, val icon: ImageVector) {
    LATEST("Latest", Icons.Default.Schedule),
    OLDEST("Oldest", Icons.Default.History),
    LONGEST("Longest", Icons.Default.Timer),
    SIZE("Size", Icons.Default.Storage)
}

/**
 * Formats upload timestamp nicely into human-readable date/time.
 */
fun formatUploadTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Recently uploaded"
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp
    val diffSec = diffMs / 1000
    val diffMin = diffSec / 60
    val diffHours = diffMin / 60
    val diffDays = diffHours / 24

    return when {
        diffSec < 60 -> "Just now"
        diffMin < 60 -> "$diffMin min ago"
        diffHours in 1..23 -> "$diffHours hr ago"
        diffDays == 1L -> "Yesterday"
        diffDays in 2..6 -> "$diffDays days ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

fun formatUploadDateOnly(timestamp: Long): String {
    if (timestamp <= 0L) return "Recent"
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun BatchVideosScreen(
    viewModel: MPlayViewModel,
    onPlayVideo: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val allBatches by viewModel.allBatchVideos.collectAsStateWithLifecycle()
    val allBatchItems by viewModel.allBatchVideoItems.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingBatchVideos.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(BatchSortFilter.LATEST) }
    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 15

    var showMenu by remember { mutableStateOf(false) }
    val likedItems = remember { mutableStateMapOf<String, Int>() }

    BackHandler {
        viewModel.selectBatch(null)
        viewModel.selectTab(AppTab.HOME)
    }

    // Seed or sync if empty on launch
    LaunchedEffect(Unit) {
        if (allBatchItems.isEmpty()) {
            viewModel.syncBatchVideos()
        }
    }

    // Fallback sample items in case database has not synced yet
    val rawItems = remember(allBatchItems) {
        if (allBatchItems.isNotEmpty()) {
            allBatchItems
        } else {
            val now = System.currentTimeMillis()
            val sampleUrls = com.example.util.VideoUrlSanitizer.RELIABLE_STREAMS
            val samplePosters = listOf(
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80",
                "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=600&q=80"
            )
            val titlesPool = listOf(
                "Arkestra Dance Performance",
                "Arkestra Dance Stage Show",
                "Arkestra Folk Dance Episode",
                "Arkestra Live Stage Drama",
                "Night Stage Celebration Dance",
                "Arkestra Superhit Finale",
                "Bhojpuri Beats Stage Dhamaka",
                "Electrifying Fusion Folk Dance",
                "Midnight Moonlight Stage Performance",
                "Village Fair Special Gala Dance",
                "High Energy Arkestra Celebration",
                "Sensational Stage Showcase",
                "Enthralling Festive Arkestra",
                "Rhythm & Beats Stage Spectacle",
                "Mega Festival Special Night"
            )

            (1..45).map { ep ->
                val titlePrefix = titlesPool[(ep - 1) % titlesPool.size]
                val url = sampleUrls[(ep - 1) % sampleUrls.size]
                val poster = samplePosters[(ep - 1) % samplePosters.size]
                val min = (ep * 3 + 2) % 25
                val sec = (ep * 13) % 60
                val durStr = if (min == 0) "0:${String.format(Locale.US, "%02d", sec)}" else "$min:${String.format(Locale.US, "%02d", sec)}"
                val sizeMb = Math.round((1.2 + ((ep * 4.7) % 68.0)) * 10.0) / 10.0
                val uploadTime = now - (45 - ep) * 3600000L * 14L
                BatchVideoItem(
                    id = ep,
                    batchId = "arkestra_dance_batch_01",
                    videoIndex = ep,
                    title = "$titlePrefix Part $ep",
                    videoUrl = url,
                    imageUrl = poster,
                    duration = durStr,
                    description = "$sizeMb MB",
                    uploadTime = uploadTime,
                    durationSec = min * 60 + sec,
                    sizeMb = sizeMb
                )
            }
        }
    }

    // Sort videos based on chosen filter:
    // Latest (Default: newest uploaded first), Oldest, Longest, Size
    val sortedItems = remember(rawItems, selectedFilter) {
        when (selectedFilter) {
            BatchSortFilter.LATEST -> {
                rawItems.sortedWith(
                    compareByDescending<BatchVideoItem> { it.uploadTime }
                        .thenByDescending { it.videoIndex }
                        .thenByDescending { it.id }
                )
            }
            BatchSortFilter.OLDEST -> {
                rawItems.sortedWith(
                    compareBy<BatchVideoItem> { it.uploadTime }
                        .thenBy { it.videoIndex }
                        .thenBy { it.id }
                )
            }
            BatchSortFilter.LONGEST -> {
                rawItems.sortedWith(
                    compareByDescending<BatchVideoItem> { it.durationSec }
                        .thenByDescending { it.uploadTime }
                )
            }
            BatchSortFilter.SIZE -> {
                rawItems.sortedWith(
                    compareByDescending<BatchVideoItem> { it.sizeMb }
                        .thenByDescending { it.uploadTime }
                )
            }
        }
    }

    // Helper function to find the index of a selected batch in sortedItems
    fun findBatchEpisodeIndex(batch: BatchVideo?, items: List<BatchVideoItem>): Int {
        if (batch == null || items.isEmpty()) return -1
        // 1. Exact batchId match
        var idx = items.indexOfFirst {
            it.batchId.isNotBlank() && it.batchId.equals(batch.batchId, ignoreCase = true)
        }
        if (idx != -1) return idx

        // 2. Partial batchId match
        if (batch.batchId.isNotBlank()) {
            idx = items.indexOfFirst {
                it.batchId.isNotBlank() && (
                    it.batchId.contains(batch.batchId, ignoreCase = true) ||
                    batch.batchId.contains(it.batchId, ignoreCase = true)
                )
            }
            if (idx != -1) return idx
        }

        // 3. Title match (video title contains batch title or vice versa)
        if (batch.title.isNotBlank()) {
            val cleanTitle = batch.title.trim()
            idx = items.indexOfFirst {
                it.title.isNotBlank() && (
                    it.title.contains(cleanTitle, ignoreCase = true) ||
                    cleanTitle.contains(it.title, ignoreCase = true)
                )
            }
            if (idx != -1) return idx

            // 4. Description match
            idx = items.indexOfFirst {
                it.description.contains(cleanTitle, ignoreCase = true)
            }
            if (idx != -1) return idx
        }

        return -1
    }

    val selectedBatch = viewModel.selectedBatchDetail

    // Navigate to the exact page and scroll to the item when a batch is clicked from Home page
    LaunchedEffect(selectedBatch, sortedItems) {
        if (selectedBatch != null && sortedItems.isNotEmpty()) {
            val targetIdx = findBatchEpisodeIndex(selectedBatch, sortedItems)
            if (targetIdx >= 0) {
                val targetPage = (targetIdx / pageSize) + 1
                val indexOnPage = targetIdx % pageSize
                currentPage = targetPage
                coroutineScope.launch {
                    listState.scrollToItem(indexOnPage)
                }
            }
        }
    }

    // Reset or re-evaluate page whenever filter changes
    LaunchedEffect(selectedFilter) {
        val currentBatch = viewModel.selectedBatchDetail
        if (currentBatch != null && sortedItems.isNotEmpty()) {
            val targetIdx = findBatchEpisodeIndex(currentBatch, sortedItems)
            if (targetIdx >= 0) {
                val targetPage = (targetIdx / pageSize) + 1
                val indexOnPage = targetIdx % pageSize
                currentPage = targetPage
                listState.scrollToItem(indexOnPage)
                return@LaunchedEffect
            }
        }
        currentPage = 1
        listState.scrollToItem(0)
    }

    // Calculate Pagination (15 videos per page)
    val totalVideosCount = sortedItems.size
    val totalPages = maxOf(1, (totalVideosCount + pageSize - 1) / pageSize)
    val validatedCurrentPage = currentPage.coerceIn(1, totalPages)

    val pageStartIndex = (validatedCurrentPage - 1) * pageSize
    val pageEndIndex = minOf(pageStartIndex + pageSize, totalVideosCount)

    val pagedVideos = remember(sortedItems, validatedCurrentPage, pageStartIndex, pageEndIndex) {
        if (sortedItems.isNotEmpty() && pageStartIndex < sortedItems.size) {
            sortedItems.subList(pageStartIndex, pageEndIndex)
        } else {
            sortedItems
        }
    }

    val activeBatch = selectedBatch ?: allBatches.firstOrNull()
    val activeTitle = selectedBatch?.title?.ifBlank { activeBatch?.title ?: "Arkestra dance" }
        ?: (activeBatch?.title ?: "Arkestra dance")
    val avatarInitials = activeTitle.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "AR" }

    Scaffold(
        containerColor = ScreenBgColor,
        topBar = {
            Surface(
                color = TopBarBgColor,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back to Home (clears selected batch)
                        IconButton(
                            onClick = {
                                viewModel.selectBatch(null)
                                viewModel.selectTab(AppTab.HOME)
                            },
                            modifier = Modifier.size(48.dp).testTag("batch_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = Color.White
                            )
                        }

                        // Channel Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5653C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = avatarInitials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title & Subtitle Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (selectedBatch != null) {
                                    val countForBatch = sortedItems.count { item ->
                                        (item.batchId.isNotBlank() && item.batchId.equals(selectedBatch.batchId, ignoreCase = true)) ||
                                        (selectedBatch.title.isNotBlank() && item.title.contains(selectedBatch.title, ignoreCase = true))
                                    }
                                    if (countForBatch > 0) "$countForBatch episodes in series • Page $validatedCurrentPage of $totalPages"
                                    else "Series page $validatedCurrentPage of $totalPages"
                                } else {
                                    "$totalVideosCount videos • 905 subscribers"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // Refresh / Sync from Cloudflare
                        IconButton(
                            onClick = {
                                viewModel.syncBatchVideos {
                                    Toast.makeText(context, "Synced $totalVideosCount batch videos", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(48.dp).testTag("batch_sync_btn")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = AccentBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Videos",
                                    tint = Color.White
                                )
                            }
                        }

                        // Overflow menu
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(48.dp).testTag("batch_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options Menu",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(TopBarBgColor)
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.CloudSync, null, tint = AccentBlue) },
                                    text = { Text("Sync Cloudflare D1", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.syncBatchVideos {
                                            Toast.makeText(context, "Synced videos from Cloudflare", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Notifications, null, tint = Color.White) },
                                    text = { Text("Mute Notifications", color = Color.White) },
                                    onClick = {
                                        showMenu = false
                                        Toast.makeText(context, "Channel notifications muted", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    // Filters Row: [Latest] [Oldest] [Longest] [Size]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        BatchSortFilter.values().forEach { filter ->
                            val isActive = selectedFilter == filter
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isActive) ChipActiveBgColor else ChipInactiveBgColor,
                                border = if (!isActive) BorderStroke(1.dp, Color(0xFF37474F)) else null,
                                modifier = Modifier
                                    .clickable {
                                        selectedFilter = filter
                                    }
                                    .testTag("filter_${filter.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = filter.icon,
                                        contentDescription = filter.label,
                                        tint = if (isActive) Color.White else TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = filter.label,
                                        color = if (isActive) Color.White else Color(0xFFCFD8DC),
                                        fontSize = 12.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Series banner when a specific batch is selected from Home page
                    if (selectedBatch != null) {
                        Surface(
                            color = Color(0xFF142436),
                            border = BorderStroke(1.dp, Color(0xFF2AABEE).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Viewing: ${selectedBatch.title} (Page $validatedCurrentPage)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    color = AccentBlue.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable {
                                        viewModel.selectBatch(null)
                                        currentPage = 1
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(0)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Show All",
                                        color = AccentBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pagination Header Bar: e.g. "Showing 1-15 of 51 videos • Page 1 of 4" + Top Page Navigation
            Surface(
                color = Color(0xFF141D26),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (totalVideosCount > 0) {
                                "Showing ${pageStartIndex + 1}-$pageEndIndex of $totalVideosCount videos"
                            } else {
                                "No videos found"
                            },
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Surface(
                            color = Color(0xFF233040),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Page $validatedCurrentPage of $totalPages",
                                color = AccentBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (totalPages > 1) {
                        PaginationPageSelectorRow(
                            currentPage = validatedCurrentPage,
                            totalPages = totalPages,
                            tagPrefix = "top_pagination",
                            onPageSelected = { page ->
                                currentPage = page
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        )
                    }
                }
            }

            // Main Video List (Simple UI: Video thumbnail with play button on it, upload time, duration, size)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(pagedVideos, key = { "${it.batchId}_${it.id}_${it.videoIndex}" }) { item ->
                    val currentLikes = likedItems[item.id.toString()] ?: (10 + (item.videoIndex * 7))
                    val isTargetBatchEpisode = selectedBatch != null && (
                        (item.batchId.isNotBlank() && item.batchId.equals(selectedBatch.batchId, ignoreCase = true)) ||
                        (selectedBatch.batchId.isNotBlank() && item.batchId.isNotBlank() && (item.batchId.contains(selectedBatch.batchId, ignoreCase = true) || selectedBatch.batchId.contains(item.batchId, ignoreCase = true))) ||
                        (selectedBatch.title.isNotBlank() && item.title.isNotBlank() && item.title.contains(selectedBatch.title, ignoreCase = true))
                    )

                    SimpleBatchVideoCard(
                        item = item,
                        isHighlighted = isTargetBatchEpisode,
                        likeCount = currentLikes,
                        onLikeClick = {
                            likedItems[item.id.toString()] = currentLikes + 1
                        },
                        onPlay = {
                            val safeItem = item.copy(videoUrl = com.example.util.VideoUrlSanitizer.sanitize(item.videoUrl))
                            onPlayVideo(safeItem.toMediaItem())
                        },
                        onShare = {
                            val safeUrl = com.example.util.VideoUrlSanitizer.sanitize(item.videoUrl)
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Watch ${item.title}: $safeUrl")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                        },
                        onCopyUrl = {
                            val safeUrl = com.example.util.VideoUrlSanitizer.sanitize(item.videoUrl)
                            clipboardManager.setText(AnnotatedString(safeUrl))
                            Toast.makeText(context, "Video link copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onDownload = {
                            val safeItem = item.copy(videoUrl = com.example.util.VideoUrlSanitizer.sanitize(item.videoUrl))
                            viewModel.downloadItem(safeItem.toMediaItem(), context)
                            Toast.makeText(context, "Downloading video...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Bottom Pagination Navigation Controls (15 videos per page)
                if (totalPages > 1) {
                    item {
                        PaginationControls(
                            currentPage = validatedCurrentPage,
                            totalPages = totalPages,
                            onPageSelected = { page ->
                                currentPage = page
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple UI: Clean video thumbnail with centered play button right on it.
 * Sized according to video thumbnail aspect ratio and dimensions.
 */
@Composable
fun SimpleBatchVideoCard(
    item: BatchVideoItem,
    isHighlighted: Boolean = false,
    likeCount: Int,
    onLikeClick: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onCopyUrl: () -> Unit,
    onDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    // Dynamic aspect ratio based on actual thumbnail dimensions (default 16:9 widescreen)
    var thumbnailAspectRatio by remember(item.imageUrl) { mutableFloatStateOf(16f / 9f) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (thumbnailAspectRatio < 1.0f) 0.76f else 0.88f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isHighlighted) Color(0xFF162534) else SimpleCardBgColor)
                .then(
                    if (isHighlighted) Modifier.border(1.5.dp, AccentBlue, RoundedCornerShape(12.dp))
                    else Modifier
                )
                .padding(6.dp)
                .testTag("video_item_${item.id}")
        ) {
            // 1. Simple Video Thumbnail scaled according to natural video thumbnail ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(thumbnailAspectRatio)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPlay() }
                    .testTag("video_thumb_${item.videoIndex}")
            ) {
                // Poster Artwork
                AsyncImage(
                    model = item.imageUrl.ifBlank {
                        "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80"
                    },
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        val w = state.result.drawable.intrinsicWidth
                        val h = state.result.drawable.intrinsicHeight
                        if (w > 0 && h > 0) {
                            val ratio = w.toFloat() / h.toFloat()
                            thumbnailAspectRatio = ratio.coerceIn(0.65f, 2.2f)
                        }
                    }
                )

                // Scrim overlay for contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )

                // Top-Left Badge: Duration & Size
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download Info",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = item.duration.ifBlank { "0:30" },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (item.description.contains("MB", ignoreCase = true)) {
                                item.description
                            } else if (item.sizeMb > 0.0) {
                                "${String.format(Locale.US, "%.1f", item.sizeMb)} MB"
                            } else {
                                "5.3 MB"
                            },
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Top-Right Quick Options Menu
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Video Options",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(TopBarBgColor)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = AccentBlue) },
                            text = { Text("Play Video", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onPlay()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Download, null, tint = Color.White) },
                            text = { Text("Download Video", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onDownload()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = Color.White) },
                            text = { Text("Copy Video URL", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onCopyUrl()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Share, null, tint = Color.White) },
                            text = { Text("Share Video", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onShare()
                            }
                        )
                    }
                }

                // Translucent Circular Play Button directly in the center of thumbnail
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                        .align(Alignment.Center)
                        .clickable { onPlay() }
                        .testTag("play_button_${item.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Bottom-Left Overlay: Video Index pill
                Surface(
                    color = AccentBlue.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Part ${item.videoIndex}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isHighlighted) {
                Surface(
                    color = AccentBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★ Episode from Selected Series",
                        color = AccentBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 2. Video Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Time of Video Uploaded & Quick Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Upload Time display: e.g. "🕒 Uploaded: 2 hr ago" or formatted date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Upload Time",
                        tint = AccentBlue,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = formatUploadTime(item.uploadTime),
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Quick Actions: Heart Reaction, Download, Share, Play
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Heart Like Button
                    Surface(
                        color = Color(0xFF233040),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { onLikeClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "❤️", fontSize = 11.sp)
                            Text(
                                text = "$likeCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Copy Link
                    IconButton(
                        onClick = onCopyUrl,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Copy Link",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Download
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Video",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Share
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Share Video",
                            tint = AccentBlue,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Telegram Forward/Share Curved Arrow Button floating on the right
        IconButton(
            onClick = onShare,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = "Forward Video",
                tint = Color(0xFF2AABEE),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Reusable Horizontal Page Selector Row: Prev, Page Numbers (1, 2, 3...), and Next
 */
@Composable
fun PaginationPageSelectorRow(
    currentPage: Int,
    totalPages: Int,
    tagPrefix: String,
    onPageSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("${tagPrefix}_bar"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Page Button
        FilledTonalButton(
            onClick = {
                if (currentPage > 1) onPageSelected(currentPage - 1)
            },
            enabled = currentPage > 1,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0xFF233040),
                disabledContainerColor = Color(0xFF141D26)
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
                .height(36.dp)
                .testTag("${tagPrefix}_prev_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Page",
                tint = if (currentPage > 1) Color.White else Color.Gray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Prev",
                fontSize = 12.sp,
                color = if (currentPage > 1) Color.White else Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Page Number Buttons (48dp touch target wrapping crisp 34dp pill)
        for (p in 1..totalPages) {
            val isCurrent = p == currentPage
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPageSelected(p) }
                    .testTag("${tagPrefix}_page_$p"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrent) ChipActiveBgColor else Color(0xFF233040),
                    border = if (!isCurrent) BorderStroke(1.dp, Color(0xFF37474F)) else null,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$p",
                            color = if (isCurrent) Color.White else Color(0xFFB0BEC5),
                            fontSize = 13.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Next Page Button
        FilledTonalButton(
            onClick = {
                if (currentPage < totalPages) onPageSelected(currentPage + 1)
            },
            enabled = currentPage < totalPages,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0xFF233040),
                disabledContainerColor = Color(0xFF141D26)
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
                .height(36.dp)
                .testTag("${tagPrefix}_next_btn")
        ) {
            Text(
                text = "Next",
                fontSize = 12.sp,
                color = if (currentPage < totalPages) Color.White else Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Page",
                tint = if (currentPage < totalPages) Color.White else Color.Gray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Pagination Controls (15 videos per page)
 * Previous, Page Numbers (1, 2, 3...), and Next
 */
@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {
    Surface(
        color = Color(0xFF1A232E),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("pagination_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "15 videos per page • Page $currentPage of $totalPages",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            PaginationPageSelectorRow(
                currentPage = currentPage,
                totalPages = totalPages,
                tagPrefix = "pagination",
                onPageSelected = onPageSelected
            )
        }
    }
}
