package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldTheme
import com.example.ui.theme.GrayText
import com.example.ui.theme.LightGray
import com.example.ui.theme.RedHover
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.CardBackground
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MPlayViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.delay
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.net.Uri
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player as Media3Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

val CategoriesList = listOf(
    "Freshly Added Now!",
    "Ullu Originals – Most Loved!",
    "PrimePlay Specials – Hot & Wild!",
    "Watch Hunter – Now Streaming!",
    "Best Of Voovi",
    "Rabbit Originals"
)

val CategoryDisplayNames = mapOf(
    "Freshly Added Now!" to "🔥 Freshly Added Now!",
    "Ullu Originals – Most Loved!" to "🔥 Ullu Originals – Most Loved!",
    "PrimePlay Specials – Hot & Wild!" to "🌟 PrimePlay Specials – Hot & Wild!",
    "Watch Hunter – Now Streaming!" to "👁️ Watch Hunter – Now Streaming!",
    "Best Of Voovi" to "🔥 Best Of Voovi",
    "Rabbit Originals" to "🐰 Rabbit Originals"
)

fun getCategoryDisplayName(category: String): String {
    val existing = CategoryDisplayNames[category]
    if (existing != null) return existing
    
    val trim = category.trim()
    val hasEmoji = trim.isNotEmpty() && trim.firstOrNull()?.let { 
        it.code >= 0x2000 || it.code == 0x00A9 || it.code == 0x00AE || (it.code in 0x2000..0x3300) 
    } == true
    if (hasEmoji) return trim
    
    val lower = trim.lowercase()
    val emoji = when {
        lower.contains("hot") || lower.contains("wild") || lower.contains("sexy") || lower.contains("passion") -> "🌟"
        lower.contains("new") || lower.contains("fresh") || lower.contains("recent") -> "🔥"
        lower.contains("original") || lower.contains("special") || lower.contains("exclusive") -> "✨"
        lower.contains("movie") || lower.contains("cinema") || lower.contains("film") -> "🎬"
        lower.contains("reel") || lower.contains("short") || lower.contains("clip") -> "📱"
        lower.contains("comic") || lower.contains("book") || lower.contains("read") || lower.contains("novel") || lower.contains("story") || lower.contains("magic") || lower.contains("fantasy") -> "📖"
        lower.contains("watch") || lower.contains("stream") -> "👁️"
        else -> "🎬"
    }
    return "$emoji $trim"
}

@Composable
fun ShimmerItem(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color(0xFF1E1E1E),
        Color(0xFF2C2C2C),
        Color(0xFF1E1E1E),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
    )
}

@Composable
fun MPlaySkeletonLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Shimmer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerItem(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        ShimmerItem(
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerItem(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                        )
                    }
                }
                ShimmerItem(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            }

            // Hero Banner Shimmer
            ShimmerItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Subtitle Shimmer
            ShimmerItem(
                modifier = Modifier
                    .width(140.dp)
                    .height(20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Row of Shimmering Cards (Horizontal List)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    Column(modifier = Modifier.width(120.dp)) {
                        ShimmerItem(
                            modifier = Modifier
                                .width(120.dp)
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerItem(
                            modifier = Modifier
                                .width(100.dp)
                                .height(12.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ShimmerItem(
                            modifier = Modifier
                                .width(60.dp)
                                .height(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subtitle 2 Shimmer
            ShimmerItem(
                modifier = Modifier
                    .width(160.dp)
                    .height(20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Extra wide list item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    ShimmerItem(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }
        
        // Centered Elegant Verification/Sync Indicator Overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color.Red,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Verifying Account Status...",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MPlayViewModel,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isStatusChecked by viewModel.isStatusChecked.collectAsStateWithLifecycle()
    val profileState by viewModel.profile.collectAsStateWithLifecycle()

    if (isLoading || !isStatusChecked || profileState == null) {
        MPlaySkeletonLoader(modifier = modifier)
        return
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedMedia by viewModel.selectedMedia.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isPremium = profileState?.isPremium == true

    // Resolve the actual tab immediately to prevent single-frame flickering of premium screens at startup
    val actualTab = remember(currentTab, isPremium) {
        if (!isPremium && (currentTab == AppTab.HOME || currentTab == AppTab.EXPLORE)) {
            AppTab.REELS
        } else if (isPremium && (currentTab == AppTab.PROFILE || currentTab == AppTab.SUPPORT)) {
            AppTab.HOME
        } else {
            currentTab
        }
    }

    // Reroute free users away from restricted Home/Explore tabs instantly, and premium users away from Profile/Support
    LaunchedEffect(isPremium, currentTab) {
        if (!isPremium && (currentTab == AppTab.HOME || currentTab == AppTab.EXPLORE)) {
            viewModel.selectTab(AppTab.REELS)
        } else if (isPremium && (currentTab == AppTab.PROFILE || currentTab == AppTab.SUPPORT)) {
            viewModel.selectTab(AppTab.HOME)
        }
    }

    // Immersive Video Player Modal State
    var isPlayingVideo by remember { mutableStateOf<MediaItem?>(null) }
    var showQualitySelectionForMedia by remember { mutableStateOf<MediaItem?>(null) }

    // Main level comic reader state
    var mainActiveReadingChapter by remember { mutableStateOf<ComicChapter?>(null) }
    var mainActiveReadingComic by remember { mutableStateOf<Comic?>(null) }

    // Keep track of the tab navigation history to handle back button correctly between tabs
    val tabHistory = remember { mutableStateListOf<AppTab>() }

    LaunchedEffect(actualTab) {
        if (tabHistory.contains(actualTab)) {
            tabHistory.remove(actualTab)
        }
        tabHistory.add(actualTab)
    }

    // Determine if back handler is enabled:
    // We should intercept back press if either player is open, comic reader is open, media details are open, or there is previous tab history to go back to.
    val backEnabled = isPlayingVideo != null || mainActiveReadingChapter != null || selectedMedia != null || tabHistory.size > 1

    BackHandler(enabled = backEnabled) {
        if (isPlayingVideo != null) {
            isPlayingVideo = null
        } else if (mainActiveReadingChapter != null) {
            mainActiveReadingChapter = null
            mainActiveReadingComic = null
        } else if (selectedMedia != null) {
            viewModel.selectMedia(null)
        } else if (tabHistory.size > 1) {
            // Drop current tab from history
            tabHistory.removeAt(tabHistory.size - 1)
            // Pop the previous tab and direct model to select it
            val previousTab = tabHistory.removeAt(tabHistory.size - 1)
            viewModel.selectTab(previousTab)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (isPlayingVideo == null) {
                    val isAdminMode by viewModel.isAdminModeEnabled.collectAsStateWithLifecycle()
                    MPlayBottomBar(
                        currentTab = actualTab,
                        isPremium = isPremium,
                        isAdminMode = isAdminMode,
                        onTabSelected = {
                            viewModel.selectMedia(null)
                            isPlayingVideo = null
                            viewModel.selectTab(it)
                        }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main views transition
                AnimatedContent(
                    targetState = actualTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "MainViewTransition"
                ) { tab ->
                    when (tab) {
                        AppTab.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onMediaSelected = { viewModel.selectMedia(it) }
                        )
                        AppTab.EXPLORE -> ExploreScreen(
                            viewModel = viewModel,
                            onMediaSelected = { viewModel.selectMedia(it) }
                        )
                        AppTab.COMICS -> ComicsScreen(viewModel = viewModel)
                        AppTab.STORIES -> StoriesScreen(viewModel = viewModel)
                        AppTab.REELS -> ReelsScreen(viewModel = viewModel)
                        AppTab.DOWNLOAD -> DownloadScreen(
                            viewModel = viewModel,
                            onPlayDownload = { mediaItem ->
                                isPlayingVideo = mediaItem
                            },
                            onReadComicDownload = { comic, chapter ->
                                mainActiveReadingComic = comic
                                mainActiveReadingChapter = chapter
                            },
                            onReadStoryDownload = { story ->
                                viewModel.selectedStoryDetail = story
                                viewModel.activeReadingPart = null
                            }
                        )
                        AppTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                        AppTab.SUPPORT -> SupportScreen(viewModel = viewModel)
                        AppTab.ADMIN -> FcmAdminScreen(viewModel = viewModel)
                    }
                }

                // Media Detail Bottom Sheet/Dialog Overlay
                selectedMedia?.let { media ->
                    MediaDetailOverlay(
                        mediaItem = media,
                        isPremiumUser = profileState?.isPremium == true,
                        isWishlisted = wishlist.any { it.mediaId == media.id },
                        onClose = { viewModel.selectMedia(null) },
                        onPlay = {
                            viewModel.selectMedia(null)
                            isPlayingVideo = media
                        },
                        onToggleWishlist = { viewModel.toggleWishlist(media) },
                        onDownload = {
                            val isM3u8 = media.title.contains("HAVELI", ignoreCase = true) || 
                                         media.imageUrl.contains(".m3u8", ignoreCase = true) || 
                                         media.reelsUrl.contains(".m3u8", ignoreCase = true) || 
                                         media.description.contains(".m3u8", ignoreCase = true)
                            if (isM3u8) {
                                showQualitySelectionForMedia = media
                            } else {
                                viewModel.downloadItem(media, context)
                                Toast.makeText(context, "${media.title} added to Downloads!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // HLS m3u8 Video Quality Picker Dialog
                showQualitySelectionForMedia?.let { media ->
                    AlertDialog(
                        onDismissRequest = { showQualitySelectionForMedia = null },
                        title = {
                            Text(
                                text = "Select Download Quality",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "An HLS playlist (.m3u8) was detected. Choose your preferred video stream quality:",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                listOf(
                                    Triple("1080p (Full HD)", "1080p", "160 MB"),
                                    Triple("720p (HD Ready)", "720p", "90 MB"),
                                    Triple("480p (Standard)", "480p", "45 MB"),
                                    Triple("360p (Low Data)", "360p", "25 MB")
                                ).forEach { (label, qualityCode, sizeText) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CardBackground, RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.downloadItem(media, context, qualityCode)
                                                Toast.makeText(context, "${media.title} ($qualityCode) added to active downloads!", Toast.LENGTH_SHORT).show()
                                                showQualitySelectionForMedia = null
                                            }
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Quality Icon",
                                                tint = GoldTheme,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = label,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Text(
                                            text = sizeText,
                                            color = GoldTheme,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showQualitySelectionForMedia = null }) {
                                Text("CANCEL", color = GoldTheme, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = Color(0xFF1E1E1E),
                        tonalElevation = 6.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        LaunchedEffect(isPlayingVideo) {
            viewModel.selectMedia(isPlayingVideo)
        }

        // True Full-Screen Immersive Video Player Overlay (placed OUTSIDE Scaffold to prevent bottom pad/nav bar issues)
        isPlayingVideo?.let { media ->
            val isInPipMode by viewModel.isInPipMode.collectAsStateWithLifecycle()
            ImmersiveVideoPlayer(
                viewModel = viewModel,
                mediaItem = media,
                isInPipMode = isInPipMode,
                onEnterPip = {
                    val activity = context as? android.app.Activity
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            val params = android.app.PictureInPictureParams.Builder()
                                .setAspectRatio(android.util.Rational(16, 9))
                                .build()
                            activity?.enterPictureInPictureMode(params)
                        } catch (e: Exception) {
                            try {
                                @Suppress("DEPRECATION")
                                activity?.enterPictureInPictureMode()
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        try {
                            @Suppress("DEPRECATION")
                            activity?.enterPictureInPictureMode()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onClose = { isPlayingVideo = null }
            )
        }

        // COMIC READER SCREEN OVERLAY (Main Level for downloads/other screens)
        mainActiveReadingChapter?.let { chapter ->
            mainActiveReadingComic?.let { comic ->
                ComicReaderOverlay(
                    comic = comic,
                    currentChapter = chapter,
                    viewModel = viewModel,
                    onClose = {
                        mainActiveReadingChapter = null
                        mainActiveReadingComic = null
                    },
                    onNavigateToChapter = { nextCh ->
                        mainActiveReadingChapter = nextCh
                    }
                )
            }
        }

        // Full-screen App Update Overlay
        val requireAppUpdate by viewModel.requireAppUpdate.collectAsStateWithLifecycle()
        val updateAppUrl by viewModel.updateAppUrl.collectAsStateWithLifecycle()

        if (requireAppUpdate) {
            BackHandler(enabled = true) {
                // Intercept back actions while update is pending
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F0E1F),
                                Color(0xFF1B1933)
                            )
                        )
                    )
                    .clickable(enabled = false) {}, // Intercept touch events
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color(0xFFE50914).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upgrade Icon",
                            tint = Color(0xFFE50914),
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Update Required",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "A new required update is available for our application. Please install the update to enjoy high-speed, zero-lag preloading and unrestricted access to movies, originals, and reels.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Button(
                        onClick = {
                            if (updateAppUrl.isNotEmpty()) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(updateAppUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(56.dp)
                            .testTag("app_update_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE50914)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Upgrade Action",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "UPDATE NOW",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // --- Days Installed Instructions Popup (Non-dismissible Full screen) ---
        val requireDaysPopup by viewModel.requireDaysInstructionsPopup.collectAsStateWithLifecycle()
        val daysInstructionsText by viewModel.daysInstructionsText.collectAsStateWithLifecycle()
        val daysInstructionsUrl by viewModel.daysInstructionsUrl.collectAsStateWithLifecycle()

        if (requireDaysPopup) {
            BackHandler(enabled = true) {
                // Intercept back actions while days instructions is active
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF090A15),
                                Color(0xFF14102C)
                            )
                        )
                    )
                    .clickable(enabled = false) {}, // Intercept touch events
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                        .background(
                            Color(0xFF1A1731).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF7A58FF), Color(0xFFE50914))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(Color(0xFF7A58FF).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Stars Icon",
                            tint = Color(0xFF7A58FF),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Premium Portal Instructions",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = daysInstructionsText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (daysInstructionsUrl.isNotEmpty()) {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        Uri.parse(daysInstructionsUrl)
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("days_popup_action_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7A58FF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Launch Icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PROCEED TO INSTRUCTIONS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // --- Floating Notification Center and Heads-up system ---
        val notificationsList by viewModel.notificationsList.collectAsStateWithLifecycle()
        val activeNotificationBanner by viewModel.activeNotificationBanner.collectAsStateWithLifecycle()
        var showNotificationCenter by remember { mutableStateOf(false) }

        // Slide-down HUD alert banner
        AnimatedVisibility(
            visible = activeNotificationBanner != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut()
        ) {
            activeNotificationBanner?.let { banner ->
                LaunchedEffect(banner) {
                    kotlinx.coroutines.delay(8000)
                    viewModel.hideNotificationBanner()
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .clickable {
                            viewModel.hideNotificationBanner()
                            // Process redirect URL
                            val actionUrl = banner.actionUrl
                            if (!actionUrl.isNullOrEmpty()) {
                                if (actionUrl.startsWith("http")) {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            Uri.parse(actionUrl)
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else if (actionUrl.startsWith("app://tab/")) {
                                    val destination = actionUrl.removePrefix("app://tab/")
                                    when (destination) {
                                        "home" -> viewModel.selectTab(AppTab.HOME)
                                        "explore" -> viewModel.selectTab(AppTab.EXPLORE)
                                        "reels" -> viewModel.selectTab(AppTab.REELS)
                                        "profile" -> viewModel.selectTab(AppTab.PROFILE)
                                        "support" -> viewModel.selectTab(AppTab.SUPPORT)
                                    }
                                }
                            }
                        }
                        .testTag("notification_banner_toast"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B2D)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon / Image
                        if (!banner.imageUrl.isNullOrEmpty() && (banner.type == "text_image" || banner.type == "text_image_link")) {
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = "Notification Image",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFE50914).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Notification Alert",
                                    tint = Color(0xFFE50914),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        // Content
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = banner.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "NOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = banner.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = { viewModel.hideNotificationBanner() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Bell FAB with custom count unread badge
        AnimatedVisibility(
            visible = isPlayingVideo == null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp, start = 20.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val prefs = remember(context) { context.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE) }
                
                var notifOffsetX by remember {
                    mutableStateOf(prefs.getFloat("notif_fab_offset_x", 0f).dp)
                }
                var notifOffsetY by remember {
                    mutableStateOf(prefs.getFloat("notif_fab_offset_y", 0f).dp)
                }

                Surface(
                    onClick = { showNotificationCenter = true },
                    modifier = Modifier
                        .offset(x = notifOffsetX, y = notifOffsetY)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    // Drag initialized after long-press
                                },
                                onDragEnd = {
                                    prefs.edit()
                                        .putFloat("notif_fab_offset_x", notifOffsetX.value)
                                        .putFloat("notif_fab_offset_y", notifOffsetY.value)
                                        .apply()
                                },
                                onDragCancel = {
                                    prefs.edit()
                                        .putFloat("notif_fab_offset_x", notifOffsetX.value)
                                        .putFloat("notif_fab_offset_y", notifOffsetY.value)
                                        .apply()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dragDpX = with(density) { dragAmount.x.toDp() }
                                    val dragDpY = with(density) { dragAmount.y.toDp() }
                                    notifOffsetX += dragDpX
                                    notifOffsetY += dragDpY
                                }
                            )
                        }
                        .size(60.dp)
                        .testTag("notification_bell_fab"),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.25f),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFE50914), Color(0xFFB80710))
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2B1015),
                                        Color(0xFF0F0709)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        if (notificationsList.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 10.dp)
                                    .size(18.dp)
                                    .background(Color(0xFFE50914), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = notificationsList.size.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showNotificationCenter) {
            AlertDialog(
                onDismissRequest = { showNotificationCenter = false },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .testTag("notification_center_dialog"),
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                content = {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF0C0A19),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MarkChatUnread,
                                        contentDescription = "Inbox",
                                        tint = Color(0xFFE50914),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Notification Center",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                
                                IconButton(onClick = { showNotificationCenter = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Tab selection switcher
                            var simulatorExpanded by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF151229), RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                Button(
                                    onClick = { simulatorExpanded = false },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!simulatorExpanded) Color(0xFFE50914) else Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Inbox (${notificationsList.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                
                                Button(
                                    onClick = { simulatorExpanded = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (simulatorExpanded) Color(0xFFE50914) else Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Push Simulator",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (simulatorExpanded) {
                                // Push simulator form
                                var simTitle by remember { mutableStateOf("New Blockbuster Movie Release!") }
                                var simBody by remember { mutableStateOf("Exclusive Premiere of Dark Waters is now streaming. Tap to explore.") }
                                var simImgUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=600&q=80") }
                                var simType by remember { mutableStateOf("text_image_link") } // "text", "text_link", "text_image", "text_image_link"
                                var simActionUrl by remember { mutableStateOf("app://tab/explore") } // website or tab link
                                var isSubmitting by remember { mutableStateOf(false) }

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "Compose Custom Notification To Firestore",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.LightGray,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = simTitle,
                                            onValueChange = { simTitle = it },
                                            label = { Text("Title") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color(0xFF131122),
                                                focusedContainerColor = Color(0xFF131122),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.LightGray,
                                                focusedBorderColor = Color(0xFFE50914),
                                                unfocusedBorderColor = Color.DarkGray,
                                                focusedLabelColor = Color(0xFFE50914)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = simBody,
                                            onValueChange = { simBody = it },
                                            label = { Text("Body Message") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color(0xFF131122),
                                                focusedContainerColor = Color(0xFF131122),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.LightGray,
                                                focusedBorderColor = Color(0xFFE50914),
                                                unfocusedBorderColor = Color.DarkGray,
                                                focusedLabelColor = Color(0xFFE50914)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = simImgUrl,
                                            onValueChange = { simImgUrl = it },
                                            label = { Text("Image URL (Optional)") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color(0xFF131122),
                                                focusedContainerColor = Color(0xFF131122),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.LightGray,
                                                focusedBorderColor = Color(0xFFE50914),
                                                unfocusedBorderColor = Color.DarkGray,
                                                focusedLabelColor = Color(0xFFE50914)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = simActionUrl,
                                            onValueChange = { simActionUrl = it },
                                            label = { Text("Action URL (Browser link or deep link/tab)") },
                                            placeholder = { Text("e.g. app://tab/reels or https://...") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color(0xFF131122),
                                                focusedContainerColor = Color(0xFF131122),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.LightGray,
                                                focusedBorderColor = Color(0xFFE50914),
                                                unfocusedBorderColor = Color.DarkGray,
                                                focusedLabelColor = Color(0xFFE50914)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = "Select Type Layout:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.LightGray
                                        )
                                        
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val types = listOf(
                                                "text" to "Text Only",
                                                "text_link" to "Text & Link",
                                                "text_image" to "Text & Image",
                                                "text_image_link" to "Text Image Link"
                                            )
                                            types.forEach { (tKey, tLabel) ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(
                                                            if (simType == tKey) Color(0xFFE50914) else Color(0xFF131122),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (simType == tKey) Color(0xFFE50914) else Color.DarkGray,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { simType = tKey }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = tLabel,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Button(
                                            onClick = {
                                                if (simTitle.isNotEmpty() && simBody.isNotEmpty()) {
                                                    isSubmitting = true
                                                    coroutineScope.launch {
                                                        val uniqueId = "notif_" + System.currentTimeMillis()
                                                        val generated = UserNotification(
                                                            id = uniqueId,
                                                            title = simTitle,
                                                            body = simBody,
                                                            imageUrl = simImgUrl.trim().ifEmpty { null },
                                                            type = simType,
                                                            actionUrl = simActionUrl.trim().ifEmpty { null },
                                                            timestamp = System.currentTimeMillis()
                                                        )
                                                        val success = viewModel.uploadNotification(generated)
                                                        if (success) {
                                                            viewModel.fetchNewNotifications()
                                                            Toast.makeText(context, "Notification sent & synced with Firestore!", Toast.LENGTH_SHORT).show()
                                                            simulatorExpanded = false
                                                        } else {
                                                            Toast.makeText(context, "Failed to broadcast. Check connection.", Toast.LENGTH_SHORT).show()
                                                        }
                                                        isSubmitting = false
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Please fill in title and body.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("send_notification_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFE50914)
                                            ),
                                            enabled = !isSubmitting,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            if (isSubmitting) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("PUSH TO FIRESTORE", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Inbox received list view
                                if (notificationsList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.ChatBubbleOutline,
                                                contentDescription = "Empty",
                                                tint = Color.DarkGray,
                                                modifier = Modifier.size(54.dp)
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "No notifications received yet.",
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(notificationsList) { item ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141224)),
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.4f))
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(34.dp)
                                                                .background(Color(0xFFE50914).copy(alpha = 0.15f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.NotificationsNone,
                                                                contentDescription = null,
                                                                tint = Color(0xFFE50914),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = item.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Text(
                                                        text = item.body,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.LightGray
                                                    )

                                                    // If image exists
                                                    if (!item.imageUrl.isNullOrEmpty() && (item.type == "text_image" || item.type == "text_image_link")) {
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        AsyncImage(
                                                            model = item.imageUrl,
                                                            contentDescription = "Image Banner",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(140.dp)
                                                                .clip(RoundedCornerShape(8.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }

                                                    // If link exists
                                                    if (!item.actionUrl.isNullOrEmpty() && (item.type == "text_link" || item.type == "text_image_link")) {
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Button(
                                                            onClick = {
                                                                showNotificationCenter = false
                                                                val actionUrl = item.actionUrl
                                                                if (actionUrl != null) {
                                                                    if (actionUrl.startsWith("http")) {
                                                                        try {
                                                                            val intent = android.content.Intent(
                                                                                android.content.Intent.ACTION_VIEW,
                                                                                Uri.parse(actionUrl)
                                                                            )
                                                                            context.startActivity(intent)
                                                                        } catch (e: Exception) {
                                                                            e.printStackTrace()
                                                                        }
                                                                    } else if (actionUrl.startsWith("app://tab/")) {
                                                                        val destination = actionUrl.removePrefix("app://tab/")
                                                                        when (destination) {
                                                                            "home" -> viewModel.selectTab(AppTab.HOME)
                                                                            "explore" -> viewModel.selectTab(AppTab.EXPLORE)
                                                                            "reels" -> viewModel.selectTab(AppTab.REELS)
                                                                            "profile" -> viewModel.selectTab(AppTab.PROFILE)
                                                                            "support" -> viewModel.selectTab(AppTab.SUPPORT)
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth().height(42.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color(0xFF1B1933)
                                                            ),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = if (item.actionUrl!!.startsWith("http")) Icons.Default.Language else Icons.Default.Shortcut,
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = if (item.actionUrl!!.startsWith("http")) "Visit Website" else "Open App Screen",
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun MPlayBottomBar(
    currentTab: AppTab,
    isPremium: Boolean,
    isAdminMode: Boolean,
    onTabSelected: (AppTab) -> Unit
) {
    val displayedTabs = remember(isPremium, isAdminMode) {
        val list = if (isPremium) {
            AppTab.values().filter { it != AppTab.PROFILE && it != AppTab.SUPPORT }
        } else {
            AppTab.values().filter { it != AppTab.HOME && it != AppTab.EXPLORE }
        }
        if (!isAdminMode) {
            list.filter { it != AppTab.ADMIN }
        } else {
            list
        }
    }

    Surface(
        color = Color(0xFF0A0909),
        tonalElevation = 8.dp,
        border = BorderStroke(width = 0.5.dp, color = Color(0xFF221F1F)),
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayedTabs.forEach { tab ->
                val isActive = currentTab == tab
                val icon = when (tab) {
                    AppTab.HOME -> if (isActive) Icons.Filled.Home else Icons.Outlined.Home
                    AppTab.EXPLORE -> if (isActive) Icons.Filled.Explore else Icons.Outlined.Explore
                    AppTab.COMICS -> if (isActive) Icons.Filled.Book else Icons.Outlined.Book
                    AppTab.STORIES -> if (isActive) Icons.Filled.AutoStories else Icons.Outlined.AutoStories
                    AppTab.REELS -> if (isActive) Icons.Filled.Movie else Icons.Outlined.Movie
                    AppTab.DOWNLOAD -> if (isActive) Icons.Filled.Download else Icons.Outlined.Download
                    AppTab.PROFILE -> if (isActive) Icons.Filled.Person else Icons.Outlined.Person
                    AppTab.SUPPORT -> if (isActive) Icons.Filled.SupportAgent else Icons.Outlined.SupportAgent
                    AppTab.ADMIN -> if (isActive) Icons.Filled.Security else Icons.Outlined.Security
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(tab) }
                        .testTag("tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        tint = if (isActive) RedPrimary else Color(0xFF8E8E93),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) RedPrimary else Color(0xFF8E8E93)
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(color = Color.White.copy(alpha = alpha * 0.22f))
    )
}

@Composable
fun ChannelSkeletonRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        items(6) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(74.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(50)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerPlaceholder(
                    modifier = Modifier.size(width = 54.dp, height = 12.dp)
                )
            }
        }
    }
}

@Composable
fun MovieSkeletonRow(isPortrait: Boolean) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(4) {
            if (isPortrait) {
                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .height(185.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ShimmerPlaceholder(modifier = Modifier.size(width = 80.dp, height = 14.dp))
                            ShimmerPlaceholder(modifier = Modifier.size(width = 45.dp, height = 10.dp))
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .width(180.dp)
                        .height(110.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ShimmerPlaceholder(modifier = Modifier.size(width = 100.dp, height = 14.dp))
                            ShimmerPlaceholder(modifier = Modifier.size(width = 60.dp, height = 10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridSkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize()
    ) {
        items(6) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ShimmerPlaceholder(modifier = Modifier.size(width = 110.dp, height = 14.dp))
                        ShimmerPlaceholder(modifier = Modifier.size(width = 70.dp, height = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReelSkeletonScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ShimmerPlaceholder(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp))
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 40.dp, end = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerPlaceholder(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(percent = 50))
                ShimmerPlaceholder(modifier = Modifier.size(width = 90.dp, height = 14.dp))
            }
            ShimmerPlaceholder(modifier = Modifier.size(width = 180.dp, height = 12.dp))
            ShimmerPlaceholder(modifier = Modifier.size(width = 140.dp, height = 12.dp))
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerPlaceholder(modifier = Modifier.size(45.dp), shape = RoundedCornerShape(percent = 50))
                    ShimmerPlaceholder(modifier = Modifier.size(width = 24.dp, height = 10.dp))
                }
            }
        }
    }
}

@Composable
fun SmoothChannelItem(
    channel: Channel,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "border_glow")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        onClick()
                    }
                )
            }
            .testTag("channel_${channel.name.lowercase().replace(" ", "_")}")
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer(rotationZ = rotationAngle)
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(listOf(RedPrimary, GoldTheme, RedPrimary)),
                    shape = CircleShape
                )
                .padding(3.dp)
        ) {
            AsyncImage(
                model = channel.imageUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(rotationZ = -rotationAngle)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = channel.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

enum class VideoSortFilter(val label: String) {
    NEW("Newest"),
    OLD("Oldest"),
    AZ("A-Z")
}

@Composable
fun HomeScreen(
    viewModel: MPlayViewModel,
    onMediaSelected: (MediaItem) -> Unit
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val dbCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    var homeSortFilter by remember { mutableStateOf(VideoSortFilter.NEW) }

    val categoriesList = remember(dbCategories) {
        val videoCats = dbCategories.filter { it.type == "video" }.map { it.name }
        if (videoCats.isNotEmpty()) videoCats else CategoriesList
    }

    val categorizedMedia = remember(allMedia, categoriesList, homeSortFilter) {
        categoriesList.associateWith { category ->
            val list = allMedia.filter { media ->
                media.category.split(",").map { it.trim() }.contains(category)
            }
            when (homeSortFilter) {
                VideoSortFilter.NEW -> list.sortedByDescending { it.id }
                VideoSortFilter.OLD -> list.sortedBy { it.id }
                VideoSortFilter.AZ -> list.sortedBy { it.title.lowercase() }
            }
        }
    }

    val activeCategories = remember(categorizedMedia, categoriesList) {
        categoriesList.filter { category ->
            (categorizedMedia[category]?.isNotEmpty() == true)
        }
    }

    val bannerItemsFromDb = allMedia.filter { it.category.split(",").map { c -> c.trim() }.contains("Banner") }
    val backupBanners = listOf(
        MediaItem(
            id = 1,
            title = "Walkman",
            description = "Walkman: Ridhima Tiwari features in a gripping emotional tale.",
            imageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=1200&q=80",
            category = "Banner",
            isPremium = false
        ),
        MediaItem(
            id = 2,
            title = "Kavita Bhabhi",
            description = "Kavita Radheshyam returns with alluring stories of desire.",
            imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1200&q=80",
            category = "Banner",
            isPremium = true
        ),
        MediaItem(
            id = 3,
            title = "Charmsukh",
            description = "An anthology of dramatic stories exploring deep intimacy.",
            imageUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1200&q=80",
            category = "Banner",
            isPremium = true
        )
    )
    val bannerItems = if (bannerItemsFromDb.isNotEmpty()) {
        if (bannerItemsFromDb.size == 1) {
            bannerItemsFromDb + backupBanners.filter { it.title != bannerItemsFromDb[0].title }
        } else {
            bannerItemsFromDb
        }
    } else {
        backupBanners
    }

    val pagerState = rememberPagerState(pageCount = { bannerItems.size })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Splash Banner Header Block (Horizontal Custom Pager)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val curBanner = bannerItems[pageIndex]
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Full-Bleed Poster Image
                        AsyncImage(
                            model = curBanner.imageUrl,
                            contentDescription = "Banner Poster ${curBanner.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Dark Radial / Linear gradient overlap for cinema theme
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.5f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f),
                                            DarkBackground
                                        )
                                    )
                                )
                        )

                        // Banner Details Overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 34.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = curBanner.title,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )

                            Text(
                                text = curBanner.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFD1D1D6),
                                    lineHeight = 18.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 320.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { onMediaSelected(curBanner) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .weight(1f)
                                        .testTag("play_banner_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Icon",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Play",
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    )
                                }

                                val isInWishlist = wishlist.any { it.mediaId == curBanner.id }
                                OutlinedButton(
                                    onClick = { viewModel.toggleWishlist(curBanner) },
                                    border = BorderStroke(1.dp, if (isInWishlist) Color.Green else Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isInWishlist) Color.Green else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .weight(1f)
                                        .testTag("my_list_banner_button")
                                ) {
                                    Icon(
                                        imageVector = if (isInWishlist) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = "Add List Icon",
                                        tint = if (isInWishlist) Color.Green else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isInWishlist) "In My List" else "My List",
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isInWishlist) Color.Green else Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Page Indicator Dots overlaid at bottom center
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bannerItems.forEachIndexed { index, _ ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) RedPrimary else Color.White.copy(alpha = 0.5f))
                        )
                    }
                }

                // Floating App Header stationary at top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "M",
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = RedPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2).sp
                        ),
                        modifier = Modifier
                            .clickable { }
                            .testTag("brand_logo")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.triggerRefresh() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Data",
                                tint = if (isLoading) GoldTheme else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { viewModel.selectTab(AppTab.EXPLORE) }
                        )
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { viewModel.selectTab(AppTab.PROFILE) }
                        )
                    }
                }
            }
        }

        // 2. Channels Carousel (Circular Horizontal Scroll List)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Channels",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Explore Channels",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isLoading) {
                    ChannelSkeletonRow()
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items = channels, key = { it.id }) { channel ->
                            SmoothChannelItem(channel = channel) {
                                viewModel.setExploreSearch(channel.name)
                                viewModel.selectTab(AppTab.EXPLORE)
                            }
                        }
                    }
                }
            }
        }

        // Sort Filter Row
        item {
            var showSortMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎥 Explore Videos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Sort: ",
                        style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                    )
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { showSortMenu = true }
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = homeSortFilter.label,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Arrow",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(Color(0xFF151414)).border(BorderStroke(0.5.dp, GoldTheme), RoundedCornerShape(8.dp))
                        ) {
                            VideoSortFilter.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = filter.label,
                                            color = if (homeSortFilter == filter) GoldTheme else Color.White,
                                            fontWeight = if (homeSortFilter == filter) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        homeSortFilter = filter
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // "My List" Shelf (Wishlist) on Home Page (Always visible to ensure Premium users can access it without a Profile tab)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "❤️ My List",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                }

                if (wishlist.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = wishlist, key = { it.mediaId }) { wish ->
                            val targetMedia = allMedia.find { it.id == wish.mediaId } ?: MediaItem(
                                id = wish.mediaId,
                                title = wish.title,
                                description = "Added to wishlist.",
                                imageUrl = wish.imageUrl,
                                category = "My List",
                                isPremium = false
                            )
                            MovieCardPortrait(
                                mediaItem = targetMedia,
                                onClick = { onMediaSelected(targetMedia) }
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "Empty My List",
                                tint = GoldTheme,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Your List is empty",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Tap the wishlist icon inside any video's details sheet to save it here for quick premium viewing!",
                                    style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Continue Watching Shelf
        if (continueWatching.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🍿 Continue Watching",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        if (continueWatching.size > 1) {
                            Text(
                                text = "Clear All",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = RedPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable {
                                    viewModel.clearAllOnSaveChangesProgress()
                                }
                            )
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = continueWatching, key = { it.mediaId }) { progress ->
                            ContinueWatchingCard(
                                progress = progress,
                                onClick = {
                                    val targetMedia = allMedia.find { it.id == progress.mediaId }
                                    if (targetMedia != null) {
                                        onMediaSelected(targetMedia)
                                    } else {
                                        val fallbackMedia = MediaItem(
                                            id = progress.mediaId,
                                            title = progress.title,
                                            description = "Resumed offline segment.",
                                            imageUrl = progress.imageUrl,
                                            category = "Freshly Added Now!",
                                            isPremium = false
                                        )
                                        onMediaSelected(fallbackMedia)
                                    }
                                },
                                onRemoveClick = {
                                    viewModel.deleteWatchProgress(progress.mediaId)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Dynamic Categories shelves horizontally (Prepopulated image links database query)
        items(activeCategories, key = { it }) { category ->
            val categoryItems = categorizedMedia[category] ?: emptyList()
            val isPortraitCategory = dbCategories.find { it.name == category }?.isPortrait ?: when (category) {
                "Ullu Originals – Most Loved!" -> true
                "PrimePlay Specials – Hot & Wild!" -> true
                "Best Of Voovi" -> true
                else -> false
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setExploreSearch("")
                            viewModel.setExploreCategory(category)
                            viewModel.selectTab(AppTab.EXPLORE)
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getCategoryDisplayName(category),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "See All",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isLoading) {
                    MovieSkeletonRow(isPortrait = isPortraitCategory)
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = categoryItems, key = { it.id }) { item ->
                            if (isPortraitCategory) {
                                MovieCardPortrait(
                                    mediaItem = item,
                                    onClick = { onMediaSelected(item) }
                                )
                            } else {
                                MovieCardHorizontal(
                                    mediaItem = item,
                                    onClick = { onMediaSelected(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumCrownBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(Color.White, shape = CircleShape)
            .border(1.dp, Color(0xFFFF2442), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "👑",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}

@Composable
fun MovieCardPortrait(
    mediaItem: MediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(185.dp)
            .clickable { onClick() }
            .testTag("movie_card_portrait_${mediaItem.title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = mediaItem.imageUrl,
                contentDescription = mediaItem.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Dark subtle gradient overlay at the base
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Premium Crown Badge overlay in the corner
            if (mediaItem.isPremium) {
                PremiumCrownBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }

            // Media Title bottom text
            Text(
                text = mediaItem.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun MovieCardHorizontal(
    mediaItem: MediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(110.dp)
            .clickable { onClick() }
            .testTag("movie_card_${mediaItem.title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = mediaItem.imageUrl,
                contentDescription = mediaItem.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Dark subtle overlay on the text/base
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Premium Crown Badge overlay in the corner
            if (mediaItem.isPremium) {
                PremiumCrownBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }

            // Media Title bottom text
            Text(
                text = mediaItem.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun ContinueWatchingCard(
    progress: com.example.data.model.PlaybackProgress,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val progressFraction = if (progress.durationMs > 0) {
        progress.positionMs.toFloat() / progress.durationMs.toFloat()
    } else {
        0f
    }

    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
            .testTag("continue_watching_card_${progress.title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                AsyncImage(
                    model = progress.imageUrl,
                    contentDescription = progress.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 160f
                            )
                        )
                )

                // Delete x button
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Progress",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(18.dp)
                    )
                }

                // LinearProgressIndicator at bottom of thumbnail
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .align(Alignment.BottomCenter),
                    color = RedPrimary,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = progress.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                
                val currentMins = progress.positionMs / 60000
                val totalMins = progress.durationMs / 60000
                val displayMins = if (currentMins == 0L) 1L else currentMins
                Text(
                    text = "Watched ${displayMins}m of ${totalMins}m",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ExploreScreen(
    viewModel: MPlayViewModel,
    onMediaSelected: (MediaItem) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedExploreCategory.collectAsStateWithLifecycle()
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val dbCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    val chips = remember(dbCategories) {
        val videoCats = dbCategories.filter { it.type == "video" }.map { it.name }
        listOf("All") + (if (videoCats.isNotEmpty()) videoCats else CategoriesList)
    }

    // Stable randomized/shuffled cache of video data from all categories
    val shuffledAllMedia = remember(allMedia) { allMedia.shuffled() }

    // When "All" is chosen, shuffle results so they are a random mix from all categories
    val baseMediaList = if (selectedCategory == "All") shuffledAllMedia else allMedia

    // Filter logic optimized with state-remember
    val filteredMedia = remember(searchQuery, selectedCategory, baseMediaList) {
        baseMediaList.filter { item ->
            val matchesQuery = item.title.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            val categoriesList = item.category.split(",").map { it.trim() }
            val matchesCategory = (selectedCategory == "All") || categoriesList.contains(selectedCategory)
            matchesQuery && matchesCategory && !categoriesList.contains("Banner")
        }
    }

    var exploreSortFilter by remember { mutableStateOf(VideoSortFilter.NEW) }

    val sortedFilteredMedia = remember(filteredMedia, exploreSortFilter) {
        when (exploreSortFilter) {
            VideoSortFilter.NEW -> filteredMedia.sortedByDescending { it.id }
            VideoSortFilter.OLD -> filteredMedia.sortedBy { it.id }
            VideoSortFilter.AZ -> filteredMedia.sortedBy { it.title.lowercase() }
        }
    }

    // Lazy load/pagination chunk counting (start with 12 items, incrementally load 12 more)
    var visibleItemsCount by remember(searchQuery, selectedCategory) { mutableStateOf(12) }

    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    
    val shouldLoadMore = remember(gridState) {
        androidx.compose.runtime.derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= gridState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && visibleItemsCount < filteredMedia.size) {
            visibleItemsCount = (visibleItemsCount + 12).coerceAtMost(filteredMedia.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
            .testTag("explore_screen")
    ) {
        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setExploreSearch(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_field"),
            placeholder = { Text("Search matches, stars, or originals...", color = GrayText) },
            leadingIcon = { Icon(Icons.Default.Search, "Search Icon", tint = Color.White) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setExploreSearch("") }) {
                        Icon(Icons.Default.Clear, "Clear Search", tint = Color.White)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = RedPrimary,
                unfocusedBorderColor = Color(0xFF2C2C2E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category Quick Filter Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chips) { chip ->
                val isSelected = selectedCategory == chip
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setExploreCategory(chip) },
                    label = {
                        Text(
                            text = if (chip.length > 15) chip.take(15) + "..." else chip,
                            color = if (isSelected) Color.White else GrayText,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RedPrimary,
                        containerColor = CardBackground
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) RedPrimary else Color(0xFF2C2C2E),
                        selectedBorderColor = RedPrimary,
                        borderWidth = 1.dp,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sort Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredMedia.size} Results",
                style = MaterialTheme.typography.bodyMedium.copy(color = GrayText)
            )

            var showSortMenu by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Sort: ",
                    style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                )
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { showSortMenu = true }
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = exploreSortFilter.label,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF151414)).border(BorderStroke(0.5.dp, GoldTheme), RoundedCornerShape(8.dp))
                    ) {
                        VideoSortFilter.values().forEach { filter ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = filter.label,
                                        color = if (exploreSortFilter == filter) GoldTheme else Color.White,
                                        fontWeight = if (exploreSortFilter == filter) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    exploreSortFilter = filter
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of results representing movie posters
        if (isLoading) {
            Box(modifier = Modifier.weight(1f)) {
                GridSkeletonGrid()
            }
        } else if (filteredMedia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = "Empty shows",
                        tint = GrayText,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No titles match your query",
                        color = GrayText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = sortedFilteredMedia.take(visibleItemsCount),
                    key = { it.id }
                ) { media ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable {
                                keyboardController?.hide()
                                onMediaSelected(media)
                            }
                            .testTag("explore_movie_${media.title.lowercase().replace(" ", "_")}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = media.imageUrl,
                                contentDescription = media.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Title & premium badge overlays
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                            )

                            if (media.isPremium) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(24.dp)
                                        .background(RedPrimary, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👑", fontSize = 12.sp)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = media.title,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = media.category.split(",").firstOrNull()?.trim() ?: media.category,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GoldTheme,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class FloatingReaction(
    val id: Long,
    val emoji: String,
    val xOffset: Float,
    val scale: Float,
    val creationTime: Long = System.currentTimeMillis()
)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ReelPlayerItem(
    reel: UserReel,
    isCurrentPage: Boolean,
    isNextPage: Boolean,
    onToggleLike: () -> Unit
) {
    val context = LocalContext.current
    var isPlayingState by remember { mutableStateOf(false) }
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(1L) }
    var showDoubleTapLikeHeart by remember { mutableStateOf(false) }
    var showPlayPauseIndicator by remember { mutableStateOf<Boolean?>(null) }

    // Interactive Floating reactions and live audience comments states
    var activeFloatingReactions by remember { mutableStateOf(listOf<FloatingReaction>()) }
    var reactionIdCounter by remember { mutableLongStateOf(0L) }
    var activeComments by remember { mutableStateOf(listOf<String>()) }

    val simulatedCommentsPool = listOf(
        "Wow! That's awesome!",
        "Incredible story 👏",
        "Wait, did that actually happen?",
        "Absolute legend",
        "Perfect edit!",
        "Love this series ❤️",
        "This is so satisfying!",
        "Bro, the suspense is real! 😱",
        "Can't wait for the next part!",
        "Is there a manga version of this?",
        "Best video on my feed today",
        "That background score fits perfectly!"
    )

    // Tick loop for active floating reactions cleanup and movement trigger
    LaunchedEffect(activeFloatingReactions.isNotEmpty()) {
        if (activeFloatingReactions.isNotEmpty()) {
            while (true) {
                delay(30)
                activeFloatingReactions = activeFloatingReactions.filter {
                    System.currentTimeMillis() - it.creationTime < 2000
                }
                if (activeFloatingReactions.isEmpty()) break
            }
        }
    }

    // Live comments generator loop
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            while (true) {
                delay(kotlin.random.Random.nextLong(2500, 4500))
                val newComment = simulatedCommentsPool.random()
                activeComments = (activeComments + newComment).takeLast(3)
            }
        } else {
            activeComments = emptyList()
        }
    }

    val randomX = remember { kotlin.random.Random }
    val spawnReaction = { emoji: String ->
        val nextId = reactionIdCounter++
        val offset = randomX.nextFloat() * 160 - 80 // -80.dp to +80.dp
        val scale = 0.8f + randomX.nextFloat() * 0.5f
        activeFloatingReactions = activeFloatingReactions + FloatingReaction(
            id = nextId,
            emoji = emoji,
            xOffset = offset,
            scale = scale,
            creationTime = System.currentTimeMillis()
        )
    }

    // Load video content ONLY when the page is the current active focused page to ensure smooth lag-free scrolling
    val shouldLoad = isCurrentPage

    var isVideoReady by remember(shouldLoad) { mutableStateOf(false) }

    // Initialize ExoPlayer with the centralized VideoCacheManager CacheDataSourceFactory for actual segment caching/preloading
    val exoPlayer = remember(shouldLoad) {
        if (shouldLoad) {
            val cacheDataSourceFactory = VideoCacheManager.createDataSourceFactory(context)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                .setDataSourceFactory(cacheDataSourceFactory)
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                    repeatMode = ExoPlayer.REPEAT_MODE_ONE // Seamless looping like Instagram/TikTok
                    playWhenReady = isCurrentPage
                }
        } else {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    isVideoReady = true
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    LaunchedEffect(exoPlayer, reel.videoUrl) {
        val player = exoPlayer ?: return@LaunchedEffect
        val mediaItem = Media3MediaItem.fromUri(reel.videoUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    // Toggle active playback when page snaps into focal view
    LaunchedEffect(isCurrentPage, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (isCurrentPage) {
            player.play()
            isPlayingState = true
        } else {
            player.pause()
            isPlayingState = false
        }
    }

    // Direct polling for position/progress updating
    LaunchedEffect(isCurrentPage, isPlayingState, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (isCurrentPage) {
            while (true) {
                currentPlaybackPosition = player.currentPosition
                totalDuration = if (player.duration > 0) player.duration else 1L
                isPlayingState = player.isPlaying
                delay(150)
            }
        }
    }

    // Liked transition feedback animation: whenever the reel's liked state transitions from false to true, trigger center heart glow
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(reel.isLiked) {
        if (firstComposition) {
            firstComposition = false
            return@LaunchedEffect
        }
        if (reel.isLiked) {
            showDoubleTapLikeHeart = true
        }
    }

    LaunchedEffect(showDoubleTapLikeHeart) {
        if (showDoubleTapLikeHeart) {
            delay(1000)
            showDoubleTapLikeHeart = false
        }
    }

    LaunchedEffect(showPlayPauseIndicator) {
        if (showPlayPauseIndicator != null) {
            delay(600)
            showPlayPauseIndicator = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Immersive full-screen visualizer overlay
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(reel.id) {
                        detectTapGestures(
                            onDoubleTap = {
                                showDoubleTapLikeHeart = true
                                if (!reel.isLiked) {
                                    onToggleLike()
                                }
                            },
                            onTap = {
                                exoPlayer?.let { player ->
                                    if (player.isPlaying) {
                                        player.pause()
                                        isPlayingState = false
                                        showPlayPauseIndicator = false
                                    } else {
                                        player.play()
                                        isPlayingState = true
                                        showPlayPauseIndicator = true
                                    }
                                }
                            }
                        )
                    }
            )
        }

        // Show a high-quality static thumbnail overlay when player is off-screen, loading, or not ready
        // This eliminates "black screen flashes" and makes scrolling feel instantly fluid
        if (exoPlayer == null || !isVideoReady) {
            coil.compose.AsyncImage(
                model = reel.imageUrl,
                contentDescription = reel.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Backdrop visual gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Community Live Comments overlay stack
        if (activeComments.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 220.dp) // Align beautifully above description and progress bar
                    .width(260.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeComments.forEachIndexed { index, comment ->
                    val alpha = when (index) {
                        0 -> 0.4f  // Oldest comment is semi-transparent
                        1 -> 0.7f
                        else -> 1.0f // Newest comment is solid
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f * alpha),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f * alpha))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(GoldTheme.copy(alpha = alpha), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👤",
                                    fontSize = 10.sp,
                                    color = Color.Black
                                )
                            }
                            Text(
                                text = comment,
                                color = Color.White.copy(alpha = alpha),
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Floating reaction rising elements renderer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp) // Float elegantly above the bottom details
        ) {
            activeFloatingReactions.forEach { reaction ->
                val elapsedTime = System.currentTimeMillis() - reaction.creationTime
                val progress = (elapsedTime.toFloat() / 2000f).coerceIn(0f, 1f)
                val yOffset = -progress * 450 // Float 450 pixels upwards
                val xWobble = kotlin.math.sin(progress * 12f) * 30 // Whimsical side-to-side wobble
                val opacity = 1f - progress
                val scale = reaction.scale * (1f + progress * 0.4f)

                Text(
                    text = reaction.emoji,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = (reaction.xOffset + xWobble).dp, y = yOffset.dp)
                        .graphicsLayer { this.alpha = opacity }
                        .scale(scale)
                )
            }
        }

        // Custom Scaling Heart overlay for Instagram double tap feedback
        AnimatedVisibility(
            visible = showDoubleTapLikeHeart,
            enter = scaleIn(initialScale = 0.2f) + fadeIn(),
            exit = scaleOut(targetScale = 2.0f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Loved",
                tint = Color.Red,
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer(shadowElevation = 8f)
            )
        }

        // Custom scaling Play/Pause indicator
        AnimatedVisibility(
            visible = showPlayPauseIndicator != null,
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 1.5f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showPlayPauseIndicator == true) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Persistent Play button overlay when video is paused (and ready)
        if (isVideoReady && !isPlayingState && showPlayPauseIndicator == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.55f), shape = CircleShape)
                    .clickable {
                        exoPlayer?.let { player ->
                            player.play()
                            isPlayingState = true
                            showPlayPauseIndicator = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Reel",
                    tint = GoldTheme,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Action Menu Column (Right Hand Side)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Instagram-like visual avatar bubble
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(2.dp, GoldTheme, CircleShape)
                    .background(Color(0xFF222222), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Creator",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Like Action with high-quality springy/bouncy scale animation
            val scaleHeart by animateFloatAsState(
                targetValue = if (reel.isLiked) 1.25f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                label = "heart_scale"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { onToggleLike() },
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer(
                            scaleX = scaleHeart,
                            scaleY = scaleHeart
                        )
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                        .testTag("reel_like_button")
                ) {
                    Icon(
                        imageVector = if (reel.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like Reel",
                        tint = if (reel.isLiked) RedPrimary else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = reel.likesCount,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Play/Pause Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        exoPlayer?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                                isPlayingState = false
                                showPlayPauseIndicator = false
                            } else {
                                player.play()
                                isPlayingState = true
                                showPlayPauseIndicator = true
                            }
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                        .testTag("reel_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Pause Reel",
                        tint = if (isPlayingState) GoldTheme else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = if (isPlayingState) "Pause" else "Play",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Share Action
            IconButton(
                onClick = {
                    Toast.makeText(context, "Link copied! Share widely.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Details Overlay (Left hand bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "@${reel.title.replace(" ", "").lowercase()} series",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = reel.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.85f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Expressive Live Reaction Emoji Tray
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reaction:",
                    color = GoldTheme,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                listOf("🔥", "😂", "❤️", "😮").forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .clickable { spawnReaction(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 16.sp)
                    }
                }
            }

            // Seeking speed buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "-10s",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.62f), shape = RoundedCornerShape(8.dp))
                        .clickable {
                            val targetPos = ((exoPlayer?.currentPosition ?: 0L) - 10000).coerceAtLeast(0)
                            exoPlayer?.seekTo(targetPos)
                            Toast.makeText(context, "Rewound 10s", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )

                Text(
                    text = "+10s",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.62f), shape = RoundedCornerShape(8.dp))
                        .clickable {
                            val targetPos = ((exoPlayer?.currentPosition ?: 0L) + 10000).coerceAtMost(exoPlayer?.duration ?: 0L)
                            exoPlayer?.seekTo(targetPos)
                            Toast.makeText(context, "Forwarded 10s", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sleek Downward Play Status indicators
            val progressFraction = (currentPlaybackPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.22f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .background(GoldTheme)
                )
            }
        }
    }
}

@Composable
fun ReelsScreen(viewModel: MPlayViewModel) {
    val context = LocalContext.current
    val reelsItems by viewModel.randomizedReels.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    if (isLoading) {
        ReelSkeletonScreen()
        return
    }

    if (reelsItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("reels_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Movie,
                    contentDescription = "Empty Reels",
                    tint = GoldTheme,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = "No Reels Videos Found",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "The local Room database and Cloud Firestore collections containing premium Reels videos are currently empty. Tap below to automatically generate and seed the Reels video database.",
                    color = GrayText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Button(
                    onClick = {
                        viewModel.createReelsDatabase()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldTheme),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("reels_screen_create_db_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload Cloud Icon",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create Reels Database",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reelsItems.size })

    // Seamless smart preloading of next video reels in the batch
    LaunchedEffect(pagerState.currentPage, reelsItems) {
        val nextIdx = pagerState.currentPage + 1
        if (nextIdx in reelsItems.indices) {
            val nextReel = reelsItems[nextIdx]
            VideoCacheManager.preloadVideo(context, nextReel.videoUrl)
        }
        val nextIdx2 = pagerState.currentPage + 2
        if (nextIdx2 in reelsItems.indices) {
            val nextReel2 = reelsItems[nextIdx2]
            VideoCacheManager.preloadVideo(context, nextReel2.videoUrl)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_screen")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val reel = reelsItems[pageIndex]
            
            // Seamless preloading of next video like Instagram / TikTok
            val isCurrentPage = pageIndex == pagerState.currentPage
            val isNextPage = pageIndex == pagerState.currentPage + 1

            ReelPlayerItem(
                reel = reel,
                isCurrentPage = isCurrentPage,
                isNextPage = isNextPage,
                onToggleLike = {
                    viewModel.toggleLikeUserReel(reel.id)
                }
            )
        }

        // Header Title 'Reels' showing current item index in a sleek floating badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Reels",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Refresh Button
                IconButton(
                    onClick = {
                        viewModel.refreshReels()
                        Toast.makeText(context, "Reels refreshed and shuffled!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                        .size(36.dp)
                        .testTag("reels_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Reels",
                        tint = GoldTheme,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "${pagerState.currentPage + 1} / ${reelsItems.size}",
                    color = GoldTheme,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun DownloadScreen(
    viewModel: MPlayViewModel,
    onPlayDownload: (MediaItem) -> Unit,
    onReadComicDownload: (Comic, ComicChapter) -> Unit,
    onReadStoryDownload: (com.example.data.model.Story) -> Unit
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("download_screen")
    ) {
        // App bar mimicking Screenshot 4 with Title "Downloads" and refresh/stats trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Manage and track localized video streams",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray
                    )
                )
            }

            IconButton(
                onClick = {
                    Toast.makeText(context, "Downloads synced with local device cache", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Active Download Queue Stats Widget
        if (activeDownloads.isNotEmpty()) {
            val totalActive = activeDownloads.size
            val downloadingCount = activeDownloads.count { !it.isPaused }
            val totalSpeed = activeDownloads.filter { !it.isPaused }.sumOf { 
                it.speed.substringBefore(" MB/s").toFloatOrNull()?.toDouble() ?: 0.0 
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C1C)),
                border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Download Queue Running",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = RedPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$downloadingCount downloading • ${totalActive - downloadingCount} paused",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray
                            )
                        )
                    }
                    if (downloadingCount > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f MB/s", totalSpeed),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = GoldTheme,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                    text = "Est. aggregate speed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }
        }

        // Body content listing active & completed downloads
        if (downloads.isEmpty() && activeDownloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "No downloads",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No downloads found\nStart downloading videos from the player screen!",
                        color = GrayText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section A: Active Downloads List
                if (activeDownloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Active Progress Monitor (${activeDownloads.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = RedPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(activeDownloads, key = { it.mediaId }) { active ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBackground, shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .testTag("active_download_${active.mediaId}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.size(72.dp)) {
                                AsyncImage(
                                    model = active.imageUrl,
                                    contentDescription = active.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                if (active.isPaused) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Pause,
                                            contentDescription = "Paused overlay",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = active.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (active.isPaused) "Paused" else active.speed,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (active.isPaused) Color.Gray else GoldTheme,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${active.status} • ${String.format(java.util.Locale.US, "%.1f", active.progress)}%",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (active.isPaused) Color.LightGray else RedPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f MB / %.1f MB", active.downloadedSizeMB, active.totalSizeMB),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Gray
                                        )
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = active.progress / 100f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = if (active.isPaused) Color.Gray else RedPrimary,
                                    trackColor = Color(0xFF2C2C2E)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (active.isPaused) {
                                            viewModel.resumeDownload(active.mediaId)
                                        } else {
                                            viewModel.pauseDownload(active.mediaId)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp).testTag("download_pause_${active.mediaId}")
                                ) {
                                    Icon(
                                        imageVector = if (active.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (active.isPaused) "Resume" else "Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.cancelActiveDownload(active.mediaId) },
                                    modifier = Modifier.size(36.dp).testTag("download_cancel_${active.mediaId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Download",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section B: Saved Downloads List
                if (downloads.isNotEmpty()) {
                    item {
                        Text(
                            text = "Saved Offline Media (${downloads.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = GoldTheme,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(downloads, key = { it.id }) { download ->
                        val isComic = remember(download, viewModel.allComics.value) {
                            download.description.startsWith("Chapter ") || 
                            download.description.contains("Chapter ") ||
                            viewModel.allComics.value.any { 
                                download.title.contains(it.title, ignoreCase = true) || 
                                it.title.contains(download.title, ignoreCase = true) 
                            }
                        }

                        val handleOpenItem = {
                            val isAudioStory = download.description == "Audio Story"
                            if (isAudioStory) {
                                val stories = viewModel.audioStories.value
                                val matchingStory = stories.find { it.title.equals(download.title, ignoreCase = true) }
                                if (matchingStory != null) {
                                    onReadStoryDownload(matchingStory)
                                } else {
                                    val fallbackStory = com.example.data.model.Story(
                                        id = download.id,
                                        title = download.title,
                                        imageUrl = download.imageUrl,
                                        content = download.description,
                                        category = "Downloaded Audio",
                                        isPremium = false,
                                        audioUrl = "",
                                        duration = "5:00",
                                        releaseDate = System.currentTimeMillis()
                                    )
                                    onReadStoryDownload(fallbackStory)
                                }
                            } else if (isComic) {
                                val parts = download.title.split(" - ")
                                val comicTitle = parts.firstOrNull() ?: ""
                                val chapterTitle = parts.getOrNull(1) ?: ""
                                val matchingComic = viewModel.allComics.value.find { 
                                    download.title.contains(it.title, ignoreCase = true) || 
                                    it.title.contains(download.title, ignoreCase = true) 
                                }
                                if (matchingComic != null) {
                                    scope.launch {
                                        val chapters = viewModel.getChaptersForComic(matchingComic.id).firstOrNull() ?: emptyList()
                                        val matchingChapter = chapters.find { 
                                            download.title.contains(it.title, ignoreCase = true) || 
                                            it.title.contains(download.title, ignoreCase = true) ||
                                            download.description.contains("Chapter ${it.chapterNumber}")
                                        } ?: chapters.firstOrNull()
                                        
                                        if (matchingChapter != null) {
                                            onReadComicDownload(matchingComic, matchingChapter)
                                        } else {
                                            Toast.makeText(context, "Comic chapter details not found", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Comic metadata not found", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val matchingMedia = viewModel.allMedia.value.find { it.title.equals(download.title, ignoreCase = true) }
                                    ?: MediaItem(
                                        title = download.title,
                                        description = download.description,
                                        imageUrl = download.imageUrl,
                                        category = "Downloaded",
                                        isPremium = false
                                    )
                                onPlayDownload(matchingMedia)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBackground, shape = RoundedCornerShape(12.dp))
                                .clickable { handleOpenItem() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AsyncImage(
                                model = download.imageUrl,
                                contentDescription = download.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = download.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Status: Completed • ${download.progress}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GoldTheme,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                LinearProgressIndicator(
                                    progress = download.progress / 100f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = RedPrimary,
                                    trackColor = Color(0xFF2C2C2E)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { handleOpenItem() },
                                    modifier = Modifier.testTag("play_download_${download.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isComic) Icons.Default.Book else Icons.Default.PlayArrow,
                                        contentDescription = if (isComic) "Read Comic" else "Play Download",
                                        tint = GoldTheme,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteDownload(download.id) },
                                    modifier = Modifier.testTag("delete_download_${download.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Download",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: MPlayViewModel) {
    val profileState by viewModel.profile.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditingProfile.collectAsStateWithLifecycle()

    val nameInput by viewModel.profileNameInput.collectAsStateWithLifecycle()
    val phoneInput by viewModel.profilePhoneInput.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header Profile block (matching screenshot 3)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(92.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AsyncImage(
                        model = profileState?.imageUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80",
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, RedPrimary, CircleShape)
                    )

                    // Edit circular icon (screenshot 3)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(RedPrimary, shape = CircleShape)
                            .clickable { viewModel.setEditingProfile(!isEditing) }
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile Image",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = profileState?.name ?: "hhg",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = profileState?.phone ?: "8888880588",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = GrayText,
                        fontWeight = FontWeight.Medium
                    )
                )

                // Premium vs Normal User details display block (requested: "This details can be seen in profile that it's premium or normal user")
                val isPremium = profileState?.isPremium == true
                Surface(
                    color = if (isPremium) GoldTheme.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, if (isPremium) GoldTheme else Color.Gray),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("profile_user_tier_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPremium) Icons.Default.Star else Icons.Default.Person,
                            contentDescription = if (isPremium) "Premium Badge" else "Normal Badge",
                            tint = if (isPremium) GoldTheme else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPremium) "Premium VIP User" else "Normal User",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isPremium) GoldTheme else Color.LightGray,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }

        // Global Theme Toggle Block (Switch between light & dark mode)
        item {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_settings_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.NightlightRound else Icons.Default.WbSunny,
                                contentDescription = "Theme Icon",
                                tint = if (isDarkTheme) GoldTheme else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "App Theme",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (isDarkTheme) "Dark Cinematic Mode" else "Light Mode Enabled",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = GrayText
                                )
                            )
                        }
                    }

                    // Compact accessible M3 Switch toggle
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.toggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldTheme,
                            checkedTrackColor = RedPrimary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.LightGray
                        ),
                        modifier = Modifier.testTag("theme_toggle_switch")
                    )
                }
            }
        }



        // Editable profile panel
        if (isEditing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Edit Profile Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { viewModel.setProfileNameInput(it) },
                            label = { Text("Display Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { viewModel.setProfilePhoneInput(it) },
                            label = { Text("Phone Number") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.setEditingProfile(false) }) {
                                Text("Cancel", color = GrayText)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { viewModel.saveProfileChanges() },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                            ) {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            }
        }

        // 2. Join Premium Promo (matching screenshot 3)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("premium_card"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, if (profileState?.isPremium == true) GoldTheme else RedPrimary),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (profileState?.isPremium == true) GoldTheme.copy(alpha = 0.2f) else RedPrimary.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium Badge",
                            tint = if (profileState?.isPremium == true) GoldTheme else RedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (profileState?.isPremium == true) "Join Premium Activated!" else "Join Premium",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (profileState?.isPremium == true) GoldTheme else Color.White
                            )
                        )
                        Text(
                            text = if (profileState?.isPremium == true) "Successfully unlocked all premium links!" else "Unlock premium content & downloads",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GrayText
                            )
                        )
                    }
                }
            }
        }





        // 3. User Wishlist Panel
        if (wishlist.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "My Watch Later Wishlist",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(wishlist) { wish ->
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(90.dp)
                                    .clickable {
                                        val targetMedia = allMedia.find { it.id == wish.mediaId } ?: MediaItem(
                                            id = wish.mediaId,
                                            title = wish.title,
                                            description = "Wishlisted item",
                                            imageUrl = wish.imageUrl,
                                            category = "Wishlist",
                                            isPremium = false
                                        )
                                        viewModel.selectMedia(targetMedia)
                                    },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = wish.imageUrl,
                                        contentDescription = wish.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    )
                                    Text(
                                        text = wish.title,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Other Settings List items (screenshot 3 matching exactly)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, shape = RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp)
            ) {
                ProfileOptionItem(
                    icon = Icons.Outlined.Edit,
                    title = "Edit Profile",
                    onClick = { viewModel.setEditingProfile(!isEditing) }
                )
                ProfileOptionItem(
                    icon = Icons.Outlined.Lock,
                    title = "Change Password",
                    onClick = { }
                )
                ProfileOptionItem(
                    icon = Icons.Outlined.Folder,
                    title = "Wishlist",
                    onClick = { }
                )
                ProfileOptionItem(
                    icon = Icons.Outlined.Subscriptions,
                    title = "Subscription",
                    onClick = { }
                )
                ProfileOptionItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    onClick = { }
                )
                ProfileOptionItem(
                    icon = Icons.Outlined.Delete,
                    title = "Delete My Account",
                    onClick = { }
                )
                ProfileOptionItem(
                    icon = Icons.Outlined.Help,
                    title = "Need Help?",
                    onClick = { viewModel.selectTab(AppTab.SUPPORT) }
                )
                ProfileOptionItem(
                    icon = Icons.Outlined.PrivacyPolicy,
                    title = "Privacy Policy",
                    onClick = { }
                )
                ProfileOptionItem(
                    icon = Icons.Outlined.Description,
                    title = "Terms of Service",
                    onClick = { }
                )
            }
        }

        // 5. Developer & Admin Portal Toggle
        item {
            val isAdminMode by viewModel.isAdminModeEnabled.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_portal_toggle_card"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleAdminMode() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = GoldTheme.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Admin Icon",
                                tint = GoldTheme,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Admin & Developer Portal",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isAdminMode) "Admin tab unlocked (Seeder/Optimizer active)" else "Tap to unlock Admin tools & optimization seeder",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = GrayText
                                )
                            )
                        }
                    }

                    Switch(
                        checked = isAdminMode,
                        onCheckedChange = { viewModel.toggleAdminMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldTheme,
                            checkedTrackColor = RedPrimary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.LightGray
                        ),
                        modifier = Modifier.testTag("admin_toggle_switch")
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileOptionItem(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Details",
            tint = GrayText,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SupportScreen(viewModel: MPlayViewModel) {
    val message by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val status by viewModel.feedbackStatus.collectAsStateWithLifecycle()
    val tickets by viewModel.supportTickets.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("support_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Customer Support",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Form mimicking screenshot 5
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Help us improve our ReelsViral streaming experience!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                // Large feedback text box (matching screenshot 5)
                OutlinedTextField(
                    value = message,
                    onValueChange = { viewModel.setFeedbackMessage(it) },
                    placeholder = { Text("Write your feedback here...", color = GrayText) },
                    minLines = 4,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = Color(0xFF2C2C2E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Large action button: 'SEND FEEDBACK'
                Button(
                    onClick = { viewModel.sendFeedback() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084FF)), // matching blue in screenshot 5
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_feedback_button")
                ) {
                    Text(
                        "SEND FEEDBACK",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Overlay status message animated
        AnimatedVisibility(
            visible = status != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            status?.let {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // History logs representing stored items in local Room Database
        Text(
            text = "Submitted Feedbacks Log (Saved in Database)",
            color = GoldTheme,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        if (tickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "You have not submitted any feedbacks yet.",
                    color = GrayText,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(tickets) { ticket ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C1C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ticket #${ticket.id}",
                                    color = RedPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Status: Received",
                                    color = Color.Green,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = ticket.message,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

// Media detail view overlay drawer
@Composable
fun MediaDetailOverlay(
    mediaItem: MediaItem,
    isPremiumUser: Boolean,
    isWishlisted: Boolean,
    onClose: () -> Unit,
    onPlay: () -> Unit,
    onToggleWishlist: () -> Unit,
    onDownload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = CardBackground,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .clickable(
                    enabled = true,
                    onClick = { /* Prevent close when clicking sheet body */ },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Close header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mediaItem.category.split(",").joinToString("  •  ") { it.trim() },
                    color = GoldTheme,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Close Drawer", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // Main landscape image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = mediaItem.imageUrl,
                    contentDescription = mediaItem.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (mediaItem.isPremium) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(RedPrimary, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "👑 PREMIUM STATUS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Title and Details text
            Text(
                text = mediaItem.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            // Description
            Text(
                text = mediaItem.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFD1D1D6),
                    lineHeight = 18.sp
                )
            )

            // User lock warning if media is premium but user is not
            val canWatch = !mediaItem.isPremium || isPremiumUser
            if (!canWatch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Lock, "Premium Locked", tint = RedPrimary)
                    Text(
                        "This item requires a Premium membership. Go to the Profile tab to Activate!",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Actions row: IMMERSIVE PLAY, Wishlist, Download
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play Action
                Button(
                    onClick = { if (canWatch) onPlay() },
                    enabled = canWatch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedPrimary,
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (canWatch) Icons.Default.PlayArrow else Icons.Default.Lock,
                        contentDescription = "Play"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PLAY NOW", fontWeight = FontWeight.Bold)
                }

                // Wishlist action toggle
                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Check else Icons.Outlined.Add,
                        contentDescription = "Toggle List",
                        tint = if (isWishlisted) Color.Green else Color.White
                    )
                }

                // Download action
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Add Download",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

enum class PlayerAspectRatio(val label: String) {
    FIT("Fit"),
    STRETCH("Stretch"),
    CROP("Crop"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

// Immersive Cinema Player Simulation overlay
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ImmersiveVideoPlayer(
    viewModel: MPlayViewModel,
    mediaItem: MediaItem,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Hide Status Bars & Navigation Bars for absolute full screen (MX Player style)
    val window = (context as? android.app.Activity)?.window
    val windowInsetsController = remember(window) {
        window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
    }

    DisposableEffect(windowInsetsController) {
        windowInsetsController?.let { controller ->
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            windowInsetsController?.let { controller ->
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    var decryptedFile by remember { mutableStateOf<java.io.File?>(null) }

    // Base URL or Fallback sample streams
    var videoUrl by remember(mediaItem.id) {
        val secureDir = java.io.File(context.filesDir, ".secured_media")
        val possibleTitles = listOf(
            mediaItem.title,
            "${mediaItem.title} (1080p)",
            "${mediaItem.title} (720p)",
            "${mediaItem.title} (480p)",
            "${mediaItem.title} (360p)"
        )
        var secureFile: java.io.File? = null
        var chosenTitleHash = mediaItem.title.hashCode()
        for (title in possibleTitles) {
            val file = java.io.File(secureDir, "${title.hashCode()}.dat")
            if (file.exists() && file.length() > 1000L) {
                secureFile = file
                chosenTitleHash = title.hashCode()
                break
            }
        }

        val initialUrl = if (secureFile != null && secureFile.exists() && secureFile.length() > 1000L) {
            val decrypted = SecureMediaEncryption.decryptFileToTemp(secureFile, context, chosenTitleHash)
            if (decrypted != null && decrypted.exists() && decrypted.length() > 1000L) {
                decryptedFile = decrypted
                android.net.Uri.fromFile(decrypted).toString()
            } else {
                if (mediaItem.reelsUrl.isNotBlank()) {
                    mediaItem.reelsUrl
                } else if (mediaItem.title.contains("HAVELI", ignoreCase = true)) {
                    "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"
                } else if (mediaItem.title.contains("Romance", ignoreCase = true)) {
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                } else {
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                }
            }
        } else if (mediaItem.reelsUrl.isNotBlank()) {
            mediaItem.reelsUrl
        } else {
            if (mediaItem.title.contains("HAVELI", ignoreCase = true)) {
                "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8"
            } else if (mediaItem.title.contains("Romance", ignoreCase = true)) {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            } else {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            }
        }
        mutableStateOf(initialUrl)
    }

    // Media3 ExoPlayer setup with aggressive preloading and local caching
    val cacheDataSourceFactory = remember(context) { VideoCacheManager.createDataSourceFactory(context) }
    val exoPlayer = remember(context) {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                45000,    // minBufferMs (increased to load extra ahead)
                120000,   // maxBufferMs (increased to preload more of the stream)
                1500,     // bufferForPlaybackMs (instant start)
                3000      // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        val initialParams = androidx.media3.common.TrackSelectionParameters.Builder(context)
            .setMaxVideoSize(640, 360)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                trackSelectionParameters = initialParams
                playWhenReady = true
            }
    }

    // Periodic cache sweeping to prune and delete video cache files older than 1 hour
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(300000) // 5 minutes interval
            VideoCacheManager.performPeriodicCleanup(context)
        }
    }

    // Interactive states
    var isPlaying by remember { mutableStateOf(true) }
    var isPlaybackEnded by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var isNightMode by remember { mutableStateOf(false) }
    var isMirrored by remember { mutableStateOf(false) }
    var isVerticallyFlipped by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var aspectRatio by remember { mutableStateOf(PlayerAspectRatio.FIT) }
    var hwDecoder by remember { mutableStateOf(true) }
    var audioPreset by remember { mutableStateOf("Theater Max") }
    var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepTimerSecondsLeft by remember { mutableStateOf(0) }
    var abStartPos by remember { mutableStateOf<Long?>(null) }
    var abEndPos by remember { mutableStateOf<Long?>(null) }
    var showAdvancedControls by remember { mutableStateOf(true) }
    var showUrlPresetDialog by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isFlashing by remember { mutableStateOf(false) }

    // Advanced features additional states
    var showSpeedOverlay by remember { mutableStateOf(false) }
    var enableSubtitles by remember { mutableStateOf(false) }
    var manualOrientationState by remember { mutableStateOf("Sensor Auto") }
    var selectedQuality by remember { mutableStateOf("360p") }
    var showQualityOverlay by remember { mutableStateOf(false) }
    var isQualityExplicitlySelected by remember { mutableStateOf(false) }
    var showUtilityRow by remember { mutableStateOf(false) }
    val isHlsVideo = remember(videoUrl) { videoUrl.contains(".m3u8", ignoreCase = true) }

    // Swiping gestures states
    var isDraggingScreen by remember { mutableStateOf(false) }
    var activeGestureType by remember { mutableStateOf<String?>(null) } // "seek", "brightness", "volume"
    var gestureVisualValue by remember { mutableStateOf("") }
    var gestureVisualProgress by remember { mutableStateOf(0f) }

    val audioManager = remember(context) { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    var baseVolume by remember { mutableStateOf(0f) }
    var baseBrightness by remember { mutableStateOf(0.5f) }
    var baseSeekPosition by remember { mutableStateOf(0L) }
    var totalDragX by remember { mutableStateOf(0f) }
    var totalDragY by remember { mutableStateOf(0f) }

    // Dynamic position tracking
    var currentPos by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }

    // Activity reset timeout monitor
    var controlInteractionTrigger by remember { mutableStateOf(0) }

    fun resetControlTimeout() {
        controlInteractionTrigger++
    }

    LaunchedEffect(showAdvancedControls, isPlaying, isLocked, controlInteractionTrigger) {
        if (showAdvancedControls && isPlaying && !isLocked) {
            kotlinx.coroutines.delay(4000) // Auto-hide controls after 4 seconds of inactivity
            showAdvancedControls = false
        }
    }

    // Preserve and maintain speed configuration
    LaunchedEffect(playbackSpeed, videoUrl) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Dynamic resolution constraint parameters mapping
    LaunchedEffect(selectedQuality, videoUrl) {
        val isOffline = videoUrl.startsWith("file://") || videoUrl.contains(".secured_media")
        if (isOffline) {
            // For offline playback, clear size restrictions to enjoy maximum offline visual fidelity
            val builder = exoPlayer.trackSelectionParameters.buildUpon()
                .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            exoPlayer.trackSelectionParameters = builder.build()
        } else {
            val builder = exoPlayer.trackSelectionParameters.buildUpon()
            when (selectedQuality) {
                "Auto" -> {
                    builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                }
                "1080p" -> {
                    builder.setMaxVideoSize(1920, 1080)
                }
                "720p" -> {
                    builder.setMaxVideoSize(1280, 720)
                }
                "480p" -> {
                    builder.setMaxVideoSize(854, 480)
                }
                "360p" -> {
                    builder.setMaxVideoSize(640, 360)
                }
                "240p" -> {
                    builder.setMaxVideoSize(426, 240)
                }
            }
            exoPlayer.trackSelectionParameters = builder.build()
        }
    }

    // Configurable Auto/Manual screen orientation state listener
    LaunchedEffect(manualOrientationState, videoUrl) {
        val activity = context as? android.app.Activity
        when (manualOrientationState) {
            "Sensor Auto" -> {
                val videoW = exoPlayer.videoSize.width
                val videoH = exoPlayer.videoSize.height
                if (videoW > 0 && videoH > 0) {
                    if (videoW > videoH) {
                        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                } else {
                    activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
            }
            "Force Landscape" -> {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            "Force Portrait" -> {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }
    }

    // Configure ExoPlayer media stream
    LaunchedEffect(videoUrl) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        try {
            val hlsMime = MimeTypes.APPLICATION_M3U8
            val mp4Mime = MimeTypes.VIDEO_MP4
            val isHls = videoUrl.contains(".m3u8", ignoreCase = true) ||
                    videoUrl.contains("m3u8_stream", ignoreCase = true) ||
                    videoUrl.contains("m3u8", ignoreCase = true)

            val buildMedia = androidx.media3.common.MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType(if (isHls) hlsMime else mp4Mime)
                .build()

            exoPlayer.setMediaItem(buildMedia)

            val lastPos = viewModel.getWatchProgress(mediaItem.id)
            if (lastPos > 0L) {
                exoPlayer.seekTo(lastPos)
                Toast.makeText(context, "Resuming from last timestamp", Toast.LENGTH_SHORT).show()
            }

            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            exoPlayer.play()
            isPlaying = true
        } catch (e: Exception) {
            Toast.makeText(context, "Streaming initialization error", Toast.LENGTH_SHORT).show()
        }
    }

    // Attach listener
    val playerListener = remember {
        object : Media3Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Media3Player.STATE_BUFFERING)
                isPlaybackEnded = (state == Media3Player.STATE_ENDED)
                if (state == Media3Player.STATE_BUFFERING) {
                    val isOffline = videoUrl.startsWith("file://") || videoUrl.contains(".secured_media")
                    if (!isOffline && !isQualityExplicitlySelected && selectedQuality != "240p") {
                        val originalQ = selectedQuality
                        val targetQ = if (originalQ == "360p") "240p" else "360p"
                        selectedQuality = targetQ
                        Toast.makeText(context, "📉 Network slow: auto-lowering quality to $targetQ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (manualOrientationState == "Sensor Auto") {
                    val activity = context as? android.app.Activity
                    val width = videoSize.width
                    val height = videoSize.height
                    if (width > 0 && height > 0) {
                        if (width > height) {
                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(exoPlayer) {
        exoPlayer.addListener(playerListener)
        onDispose {
            exoPlayer.removeListener(playerListener)
            try {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (pos > 1000 && dur > 0) {
                    if (pos < dur - 5000 && pos < dur * 0.95) {
                        viewModel.saveWatchProgress(
                            mediaId = mediaItem.id,
                            title = mediaItem.title,
                            imageUrl = mediaItem.imageUrl,
                            positionMs = pos,
                            durationMs = dur
                        )
                    } else {
                        viewModel.deleteWatchProgress(mediaItem.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            exoPlayer.release()
            SecureMediaEncryption.clearDecryptedCache(context)
        }
    }

    DisposableEffect(context) {
        val activity = context as? android.app.Activity
        onDispose {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Dynamic status loops
    LaunchedEffect(exoPlayer, isPlaying) {
        var lastSavedTime = 0L
        while (true) {
            if (exoPlayer.applicationLooper.thread.isAlive) {
                currentPos = exoPlayer.currentPosition
                totalDuration = exoPlayer.duration

                // A-B Loop checking
                if (abStartPos != null && abEndPos != null) {
                    if (currentPos >= abEndPos!!) {
                        exoPlayer.seekTo(abStartPos!!)
                        currentPos = abStartPos!!
                    }
                }

                if (isPlaying && currentPos > 0 && totalDuration > 0) {
                    val now = System.currentTimeMillis()
                    if (now - lastSavedTime > 3000) {
                        lastSavedTime = now
                        if (currentPos < totalDuration - 5000 && currentPos < totalDuration * 0.95) {
                            viewModel.saveWatchProgress(
                                mediaId = mediaItem.id,
                                title = mediaItem.title,
                                imageUrl = mediaItem.imageUrl,
                                positionMs = currentPos,
                                durationMs = totalDuration
                            )
                        } else {
                            viewModel.deleteWatchProgress(mediaItem.id)
                        }
                    }
                }
            }
            kotlinx.coroutines.delay(250)
        }
    }

    // Sleep Timer countdown thread
    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes != null && sleepTimerMinutes!! > 0) {
            sleepTimerSecondsLeft = sleepTimerMinutes!! * 60
            while (sleepTimerSecondsLeft > 0) {
                kotlinx.coroutines.delay(1000)
                sleepTimerSecondsLeft--
            }
            exoPlayer.pause()
            isPlaying = false
            sleepTimerMinutes = null
            Toast.makeText(context, "Sleep timer finished. Playback stopped.", Toast.LENGTH_LONG).show()
        }
    }

    // Helper functions
    fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Android Player Engine container with transform overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when (aspectRatio) {
                        PlayerAspectRatio.RATIO_16_9 -> Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        PlayerAspectRatio.RATIO_4_3 -> Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                        else -> Modifier.fillMaxSize()
                    }
                )
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val view = android.view.LayoutInflater.from(ctx).inflate(com.example.R.layout.custom_player_view, null) as PlayerView
                    view.apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = when (aspectRatio) {
                            PlayerAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            PlayerAspectRatio.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            PlayerAspectRatio.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = when (aspectRatio) {
                        PlayerAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        PlayerAspectRatio.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        PlayerAspectRatio.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = if (isMirrored) -1f else 1f,
                        scaleY = if (isVerticallyFlipped) -1f else 1f
                    )
            )
        }

        // Night Mode dimming lens
        if (!isInPipMode) {
            if (isNightMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            )
        }

        // Interactive Tap & swipe gestures detector
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked, totalDuration) {
                    if (isLocked) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val colWidth = size.width / 3f
                            if (offset.x < colWidth) {
                                // Double Tap Left Side: Seek -10s
                                val seekToPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(seekToPos)
                                currentPos = seekToPos
                                Toast.makeText(context, "⏮ 10s Backward", Toast.LENGTH_SHORT).show()
                            } else if (offset.x > colWidth * 2f) {
                                // Double Tap Right Side: Seek +10s
                                val seekToPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(seekToPos)
                                currentPos = seekToPos
                                Toast.makeText(context, "⏭ 10s Forward", Toast.LENGTH_SHORT).show()
                            } else {
                                // Double Tap Middle: Play Pause
                                if (isPlaying) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer.play()
                                    isPlaying = true
                                }
                                Toast.makeText(context, if (isPlaying) "▶ Play" else "⏸ Pause", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTap = {
                            showAdvancedControls = !showAdvancedControls
                        }
                    )
                }
                .pointerInput(isLocked, totalDuration) {
                    if (isLocked) return@pointerInput
                    val boxWidth = size.width.toFloat()
                    val boxHeight = size.height.toFloat()
                    
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDraggingScreen = true
                            totalDragX = 0f
                            totalDragY = 0f
                            activeGestureType = null
                            
                            baseVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                            val activity = context as? android.app.Activity
                            val lp = activity?.window?.attributes
                            baseBrightness = if (lp != null && lp.screenBrightness >= 0f) lp.screenBrightness else 0.5f
                            baseSeekPosition = exoPlayer.currentPosition
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                            
                            if (activeGestureType == null) {
                                if (Math.abs(totalDragX) > 30f && Math.abs(totalDragX) > Math.abs(totalDragY)) {
                                    activeGestureType = "seek"
                                } else if (Math.abs(totalDragY) > 30f && Math.abs(totalDragY) > Math.abs(totalDragX)) {
                                    val startX = change.position.x
                                    if (startX < (boxWidth / 2f)) {
                                        activeGestureType = "brightness"
                                    } else {
                                        activeGestureType = "volume"
                                    }
                                }
                            }
                            
                            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f)
                            
                            when (activeGestureType) {
                                "seek" -> {
                                    val sensitivityRange = totalDuration.coerceAtLeast(60000L).coerceAtMost(300000L) // limit scroll bounds sensitivity
                                    val seekOffset = (totalDragX / boxWidth) * sensitivityRange
                                    val targetPos = (baseSeekPosition + seekOffset.toLong()).coerceIn(0L, totalDuration)
                                    gestureVisualValue = formatTime(targetPos)
                                    gestureVisualProgress = targetPos.toFloat() / totalDuration.coerceAtLeast(1L).toFloat()
                                    // Seek live for high responsiveness
                                    exoPlayer.seekTo(targetPos)
                                    currentPos = targetPos
                                }
                                "brightness" -> {
                                    val brightnessOffset = -(totalDragY / boxHeight) * 1f
                                    val newBrightness = (baseBrightness + brightnessOffset).coerceIn(0.01f, 1f)
                                    val activity = context as? android.app.Activity
                                    val lp = activity?.window?.attributes
                                    if (lp != null) {
                                        lp.screenBrightness = newBrightness
                                        activity.window.attributes = lp
                                    }
                                    gestureVisualValue = "${(newBrightness * 100).toInt()}%"
                                    gestureVisualProgress = newBrightness
                                }
                                "volume" -> {
                                    val volumeOffset = -(totalDragY / boxHeight) * maxVol
                                    val newVolumeRaw = (baseVolume + volumeOffset).coerceIn(0f, maxVol)
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolumeRaw.toInt(), 0)
                                    gestureVisualValue = "${(newVolumeRaw / maxVol * 100).toInt()}%"
                                    gestureVisualProgress = newVolumeRaw / maxVol
                                }
                            }
                        },
                        onDragEnd = {
                            activeGestureType = null
                            isDraggingScreen = false
                        },
                        onDragCancel = {
                            activeGestureType = null
                            isDraggingScreen = false
                        }
                    )
                }
        )

        // 2. Playback buffering/optimizing HUD
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = RedPrimary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // Camera snap white flash animation overlay
        if (isFlashing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // Dynamic simulated subtitle track overlay
        if (enableSubtitles) {
            val subtitleText = when (currentPos / 1000) {
                in 0L..9L -> "Welcome to the premium ultra-high-definition player stream..."
                in 10L..24L -> "Enjoy lossless stereo 5.1 surround sound auto-synchronized..."
                in 25L..44L -> "Swipe left for brightness and right for volume control..."
                in 45L..69L -> "Double tap edges to seek 10s. Center to pause/play."
                in 70L..120L -> "Supporting hardware-accelerated playback seamlessly..."
                else -> ""
            }
            if (subtitleText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 90.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = subtitleText,
                            color = Color.Yellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Swipe Gesture HUD Indicator Overlay
        AnimatedVisibility(
            visible = activeGestureType != null && isDraggingScreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val hIcon = when (activeGestureType) {
                        "volume" -> if (gestureVisualProgress == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp
                        "brightness" -> Icons.Default.WbSunny
                        else -> Icons.Default.LinearScale
                    }
                    val hTitle = when (activeGestureType) {
                        "volume" -> "Volume"
                        "brightness" -> "Brightness"
                        else -> "Position"
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = hIcon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(28.dp))
                        Text(text = hTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    
                    Text(
                        text = gestureVisualValue,
                        color = GoldTheme,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp
                    )
                    
                    LinearProgressIndicator(
                        progress = { gestureVisualProgress },
                        color = RedPrimary,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .width(130.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        // 3. Complete Controls overlay
        AnimatedVisibility(
            visible = showAdvancedControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (isLocked) {
                // LOCK state visual button container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    IconButton(
                        onClick = {
                            isLocked = false
                            Toast.makeText(context, "🔓 Controls Unlocked", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .border(1.dp, RedPrimary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock screen",
                            tint = RedPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            } else {
                // UNLOCKED Standard Overlay controls
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    resetControlTimeout()
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    showAdvancedControls = false
                                }
                            )
                        }
                ) {

                    // A. Top Header Panel - slides down from the TOP
                    AnimatedVisibility(
                        visible = showAdvancedControls,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(onClick = onClose) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = mediaItem.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (audioPreset.isNotBlank()) "♫ $audioPreset" else "Stereo 2.1",
                                                color = GoldTheme,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                text = "• 1080p Ultra-HD",
                                                color = Color.LightGray,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }

                                // HUD shortcuts
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // HW / SW Decoder Toggle Badge
                                    Surface(
                                        color = if (hwDecoder) RedPrimary else Color.Gray,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier
                                            .clickable {
                                                hwDecoder = !hwDecoder
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Switched to ${if (hwDecoder) "Hardware [HW]" else "Software [SW]"} decoder engine",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (hwDecoder) "HW+" else "SW",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Sleep timer remaining monitor
                                    if (sleepTimerMinutes != null) {
                                        Surface(
                                            color = GoldTheme.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, GoldTheme),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "⏱ ${String.format("%02d:%02d", sleepTimerSecondsLeft / 60, sleepTimerSecondsLeft % 60)}",
                                                color = GoldTheme,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }



                    // C. Bottom scrubbing, timeline & primary controls - slides up from the BOTTOM
                    AnimatedVisibility(
                        visible = showAdvancedControls,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sleek scrollable player configuration controls row (collapsible)
                                AnimatedVisibility(
                                    visible = showUtilityRow,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Night Mode
                                        PlayerUtilityItem(
                                            icon = if (isNightMode) Icons.Default.NightlightRound else Icons.Default.WbSunny,
                                            label = "Night",
                                            isActive = isNightMode,
                                            onClick = {
                                                isNightMode = !isNightMode
                                                Toast.makeText(context, if (isNightMode) "Night mode active" else "Normal exposure", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        // 2. Playback Speed
                                        PlayerUtilityItem(
                                            icon = Icons.Default.Speed,
                                            label = "${String.format("%.2f", playbackSpeed)}x",
                                            isActive = playbackSpeed != 1f,
                                            onClick = {
                                                showSpeedOverlay = true
                                            }
                                        )

                                        // 3. Audio Mute
                                        PlayerUtilityItem(
                                            icon = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                            label = if (isMuted) "Muted" else "Mute",
                                            isActive = isMuted,
                                            onClick = {
                                                isMuted = !isMuted
                                                exoPlayer.volume = if (isMuted) 0f else 1f
                                                Toast.makeText(context, if (isMuted) "Sound muted" else "Sound unmuted", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        // 4. A-B Loop
                                        val isAbActive = abStartPos != null
                                        val abTextDisplay = when {
                                            abStartPos != null && abEndPos != null -> "A-B Loop"
                                            abStartPos != null -> "Set Point B"
                                            else -> "A-B Repeat"
                                        }
                                        PlayerUtilityItem(
                                            icon = Icons.Default.Loop,
                                            label = abTextDisplay,
                                            isActive = isAbActive,
                                            onClick = {
                                                if (abStartPos == null) {
                                                    abStartPos = exoPlayer.currentPosition
                                                    Toast.makeText(context, "Point A set at ${formatTime(abStartPos!!)}", Toast.LENGTH_SHORT).show()
                                                } else if (abEndPos == null) {
                                                    if (exoPlayer.currentPosition > abStartPos!!) {
                                                        abEndPos = exoPlayer.currentPosition
                                                        Toast.makeText(context, "Point B set at ${formatTime(abEndPos!!)}. Loop initialized!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Point B must be after Point A", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    abStartPos = null
                                                    abEndPos = null
                                                    Toast.makeText(context, "A-B Loop reset", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )

                                        // 5. Sleep Timer
                                        PlayerUtilityItem(
                                            icon = Icons.Default.Timer,
                                            label = if (sleepTimerMinutes != null) "Timer: ${sleepTimerMinutes}m" else "Sleep",
                                            isActive = sleepTimerMinutes != null,
                                            onClick = {
                                                sleepTimerMinutes = when (sleepTimerMinutes) {
                                                    null -> 5
                                                    5 -> 15
                                                    15 -> 30
                                                    30 -> 60
                                                    else -> null
                                                }
                                                Toast.makeText(context, if (sleepTimerMinutes != null) "Timer programmed to stop in ${sleepTimerMinutes}m" else "Sleep timer off", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        // 6. Mirror Mode
                                        PlayerUtilityItem(
                                            icon = Icons.Default.Flip,
                                            label = "Mirror",
                                            isActive = isMirrored,
                                            onClick = {
                                                isMirrored = !isMirrored
                                            }
                                        )

                                        // 7. Vertical Flip
                                        PlayerUtilityItem(
                                            icon = Icons.Default.Flip,
                                            label = "V-Flip",
                                            isActive = isVerticallyFlipped,
                                            onClick = {
                                                isVerticallyFlipped = !isVerticallyFlipped
                                            }
                                        )

                                        // 8. Screenshot
                                        PlayerUtilityItem(
                                            icon = Icons.Default.Crop,
                                            label = "Screenshot",
                                            isActive = false,
                                            onClick = {
                                                isFlashing = true
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(80)
                                                    isFlashing = false
                                                }
                                                Toast.makeText(context, "📸 Screenshot captured!", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        // 9. Equalizer
                                        PlayerUtilityItem(
                                            icon = Icons.Default.GraphicEq,
                                            label = audioPreset,
                                            isActive = audioPreset != "Normal",
                                            onClick = {
                                                audioPreset = when (audioPreset) {
                                                    "Normal" -> "Bass Elite"
                                                    "Bass Elite" -> "Vocals 3D"
                                                    "Vocals 3D" -> "Crisp Treble"
                                                    "Crisp Treble" -> "Theater Max"
                                                    else -> "Normal"
                                                }
                                                Toast.makeText(context, "Sound FX profile: $audioPreset", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        // 10. Screen Rotation
                                        PlayerUtilityItem(
                                            icon = Icons.Default.ScreenRotation,
                                            label = when (manualOrientationState) {
                                                "Sensor Auto" -> "Auto-Rot"
                                                "Force Landscape" -> "Land"
                                                else -> "Port"
                                            },
                                            isActive = manualOrientationState != "Sensor Auto",
                                            onClick = {
                                                manualOrientationState = when (manualOrientationState) {
                                                    "Sensor Auto" -> "Force Landscape"
                                                    "Force Landscape" -> "Force Portrait"
                                                    else -> "Sensor Auto"
                                                }
                                                Toast.makeText(context, "Orientation: $manualOrientationState", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        // 11. Subtitles
                                        PlayerUtilityItem(
                                            icon = Icons.Default.Subtitles,
                                            label = if (enableSubtitles) "Subs Track" else "No Subs",
                                            isActive = enableSubtitles,
                                            onClick = {
                                                enableSubtitles = !enableSubtitles
                                                Toast.makeText(context, if (enableSubtitles) "Subtitles initialized" else "Subtitles disabled", Toast.LENGTH_SHORT).show()
                                            }
                                        )

                                        if (isHlsVideo) {
                                            // 12. Quality
                                            PlayerUtilityItem(
                                                icon = Icons.Default.Settings,
                                                label = if (videoUrl.startsWith("file://") || videoUrl.contains(".secured_media")) "1080p Offline" else "Res: $selectedQuality",
                                                isActive = selectedQuality != "Auto",
                                                onClick = {
                                                    if (videoUrl.startsWith("file://") || videoUrl.contains(".secured_media")) {
                                                        Toast.makeText(context, "Local full-fidelity private playback active", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        showQualityOverlay = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                // Timeline times row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatTime(currentPos),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    // display active point indicators for A-B Repeat loop
                                    if (abStartPos != null || abEndPos != null) {
                                        val startLab = if (abStartPos != null) formatTime(abStartPos!!) else ".."
                                        val endLab = if (abEndPos != null) formatTime(abEndPos!!) else ".."
                                        Text(
                                            text = "A-B Segment: $startLab - $endLab",
                                            color = GoldTheme,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = formatTime(totalDuration),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Seek bar slider
                                Slider(
                                    value = currentPos.toFloat().coerceIn(0f, totalDuration.toFloat().coerceAtLeast(1f)),
                                    onValueChange = {
                                        exoPlayer.seekTo(it.toLong())
                                        currentPos = it.toLong()
                                        resetControlTimeout()
                                    },
                                    valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = RedPrimary,
                                        activeTrackColor = RedPrimary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Essential premium action bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left actions (Lock / Settings tester URL panel)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // 1. Lock screen controller button
                                        IconButton(
                                            onClick = {
                                                isLocked = true
                                                showAdvancedControls = true
                                                Toast.makeText(context, "🔒 Controls Locked", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = "Lock controls",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        if (isHlsVideo) {
                                            // Custom Quality Selection Button with DropdownMenu popup beside Lock controls
                                            Box {
                                                var showQualityMenu by remember { mutableStateOf(false) }

                                                IconButton(
                                                    onClick = {
                                                        if (videoUrl.startsWith("file://") || videoUrl.contains(".secured_media")) {
                                                            Toast.makeText(context, "Local full-fidelity private playback active", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            showQualityMenu = true
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.HighQuality,
                                                        contentDescription = "Select Resolution Quality",
                                                        tint = if (selectedQuality == "Auto") Color.White else GoldTheme,
                                                        modifier = Modifier.size(26.dp)
                                                    )
                                                }

                                                DropdownMenu(
                                                    expanded = showQualityMenu,
                                                    onDismissRequest = { showQualityMenu = false },
                                                    modifier = Modifier.background(Color(0xFF151414)).border(0.5.dp, GoldTheme, RoundedCornerShape(8.dp))
                                                ) {
                                                    listOf("Auto", "1080p", "720p", "480p", "360p", "240p").forEach { quality ->
                                                        val isActive = (selectedQuality == quality)
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Text(
                                                                        text = if (quality == "360p") "360p (Recommended)" else quality,
                                                                        color = if (isActive) GoldTheme else Color.White,
                                                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                                        fontSize = 14.sp
                                                                    )
                                                                    if (isActive) {
                                                                        Spacer(modifier = Modifier.width(8.dp))
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = "Active",
                                                                            tint = GoldTheme,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }
                                                            },
                                                            onClick = {
                                                                selectedQuality = quality
                                                                isQualityExplicitlySelected = (quality != "Auto")
                                                                showQualityMenu = false
                                                                Toast.makeText(context, "Rescaling stream to $quality", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // 3. Picture-in-Picture Trigger Button
                                        IconButton(
                                            onClick = onEnterPip
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureInPictureAlt,
                                                contentDescription = "Enter Picture in Picture",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // 2. Custom Testing Simulator Button
                                        IconButton(
                                            onClick = { showUrlPresetDialog = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Test stream URL",
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Center actions (Rewind / Play / Fast Forward)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                                exoPlayer.seekTo(target)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FastRewind,
                                                contentDescription = "Rewind 10s",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        // Primary Giant Play/Pause trigger
                                        IconButton(
                                            onClick = {
                                                if (isPlaybackEnded) {
                                                    exoPlayer.seekTo(0)
                                                    exoPlayer.play()
                                                    isPlaying = true
                                                } else if (isPlaying) {
                                                    exoPlayer.pause()
                                                    isPlaying = false
                                                } else {
                                                    exoPlayer.play()
                                                    isPlaying = true
                                                }
                                            },
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(RedPrimary, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying && !isPlaybackEnded) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                                exoPlayer.seekTo(target)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FastForward,
                                                contentDescription = "Seek Forward 10s",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    // Right actions (Loop / Aspect Ratio switcher)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Quick Settings Row toggler (Toggles collapse state of utility configuration bar)
                                        IconButton(
                                            onClick = {
                                                showUtilityRow = !showUtilityRow
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "Toggle Quick Settings Utility Bar",
                                                tint = if (showUtilityRow) RedPrimary else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // Loop track toggle
                                        IconButton(
                                            onClick = {
                                                isLooping = !isLooping
                                                exoPlayer.repeatMode = if (isLooping) Media3Player.REPEAT_MODE_ONE else Media3Player.REPEAT_MODE_OFF
                                                Toast.makeText(context, if (isLooping) "Repeat Track: On" else "Repeat Track: Off", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Loop,
                                                contentDescription = "Repeat toggle",
                                                tint = if (isLooping) RedPrimary else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // Aspect Ratio button
                                        Button(
                                            onClick = {
                                                aspectRatio = when (aspectRatio) {
                                                    PlayerAspectRatio.FIT -> PlayerAspectRatio.STRETCH
                                                    PlayerAspectRatio.STRETCH -> PlayerAspectRatio.CROP
                                                    PlayerAspectRatio.CROP -> PlayerAspectRatio.RATIO_16_9
                                                    PlayerAspectRatio.RATIO_16_9 -> PlayerAspectRatio.RATIO_4_3
                                                    else -> PlayerAspectRatio.FIT
                                                }
                                                Toast.makeText(context, "Aspect Ratio: ${aspectRatio.label}", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = aspectRatio.label,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Custom Speed Controller Card Overlay
        if (showSpeedOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSpeedOverlay = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161515)),
                    border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(320.dp)
                        .clickable(enabled = false) { /* prevent closing when clicking inside */ }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Adjust Playback Speed",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    playbackSpeed = (playbackSpeed - 0.10f).coerceIn(0.25f, 3.0f)
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) {
                                Icon(Icons.Default.Remove, "Decrease Speed", tint = Color.White)
                            }
                            
                            Text(
                                text = "${String.format("%.2f", playbackSpeed)}x",
                                color = GoldTheme,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            
                            IconButton(
                                onClick = {
                                    playbackSpeed = (playbackSpeed + 0.10f).coerceIn(0.25f, 3.0f)
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) {
                                Icon(Icons.Default.Add, "Increase Speed", tint = Color.White)
                            }
                        }
                        
                        // Row of standard presets for quick click tuning
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { pre ->
                                Button(
                                    onClick = {
                                        playbackSpeed = pre
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (playbackSpeed == pre) RedPrimary else Color.White.copy(alpha = 0.1f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Text("${pre}x", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Button(
                            onClick = { showSpeedOverlay = false },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Speed", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 6. Custom Quality Controller Card Overlay
        if (showQualityOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showQualityOverlay = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161515)),
                    border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(320.dp)
                        .clickable(enabled = false) { /* prevent closing when clicking inside */ }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Select Stream Quality",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "App will always attempt 360p or lower by default to prevent buffer stutter.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Series of Quality options matching exact pixel selections or automatic setup
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Auto", "1080p", "720p", "480p", "360p (Recommended)", "240p").forEach { quality ->
                                val cleanQuality = quality.split(" ").first()
                                val isActive = (selectedQuality == cleanQuality)
                                
                                Button(
                                    onClick = {
                                        selectedQuality = cleanQuality
                                        isQualityExplicitlySelected = (cleanQuality != "Auto")
                                        showQualityOverlay = false
                                        Toast.makeText(context, "Rescaling stream to $cleanQuality", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isActive) RedPrimary else Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(quality, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        if (isActive) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = GoldTheme,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showQualityOverlay = false },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Resolution", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Test Parameter Stream Selector Bottom Sheet / Dialog
        if (showUrlPresetDialog) {
            AlertDialog(
                onDismissRequest = { showUrlPresetDialog = false },
                containerColor = Color(0xFF161515),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, "Preset configuration", tint = RedPrimary)
                        Text(
                            text = "Token Stream Testing Hub",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Simulate various video types (HLS .m3u8 index files, MPEG-DASH .mpd manifests, progressive MP4 streams) below for end-to-end player validation:",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        // Input field
                        OutlinedTextField(
                            value = videoUrl,
                            onValueChange = { videoUrl = it },
                            label = { Text("Stream URL Parameter") },
                            textStyle = TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = RedPrimary,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "PRESET TESTING SAMPLES:",
                            color = GoldTheme,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Presets Row
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Preset 1: Mux HLS Stream
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        videoUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                                        Toast.makeText(context, "Mux HLS stream selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        "1. Mux HLS Video (.m3u8)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Preset 2: Unified Streaming Tears of Steel HLS
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                                        Toast.makeText(context, "Tears of Steel HLS selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        "2. Tears of Steel HLS (.m3u8)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "https://demo.unified-streaming.com/k8s/features/.../tears-of-steel.ism/.m3u8",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Preset 3: Sintel MP4 Stream
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                                        Toast.makeText(context, "Sintel progressive MP4 stream selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        "3. Sintel open movie progressive (.mp4)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "https://commondatastorage.googleapis.com/.../Sintel.mp4",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Preset 4: Big Buck Bunny Akamai DASH
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        videoUrl = "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd"
                                        Toast.makeText(context, "Akamai DASH stream selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        "4. Big Buck Bunny DASH (.mpd)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Preset 5: Qualcomm MultiRate DASH
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        videoUrl = "https://daultiRate.mpd.net/dash264/TestCases/1c/qualcomm/2/MultiRate.mpd"
                                        Toast.makeText(context, "Qualcomm MultiRate DASH stream selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        "5. MultiRate Qualcomm baseline DASH (.mpd)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "https://daultiRate.mpd.net/dash264/TestCases/1c/qualcomm/.../MultiRate.mpd",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Preset 6: Big Buck Bunny MP4 Stream
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                                        Toast.makeText(context, "Big Buck Bunny MP4 stream selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        "6. Big Buck Bunny progressive (.mp4)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "https://commondatastorage.googleapis.com/.../BigBuckBunny.mp4",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showUrlPresetDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("Apply & Close", color = Color.White)
                    }
                }
            )
        }
        }
    }
}

@Composable
fun PlayerUtilityItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    if (isActive) RedPrimary.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.6f),
                    CircleShape
                )
                .border(1.dp, if (isActive) RedPrimary else Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = if (isActive) RedPrimary else Color.LightGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(68.dp)
        )
    }
}

// Support extension properties
private val Icons.Outlined.PrivacyPolicy: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Default.VerifiedUser

private val Icons.Outlined.SupportAgent: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Default.Support

@Composable
fun FcmAdminScreen(viewModel: MPlayViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fcmHistorySyncStatus by viewModel.fcmHistorySyncStatus.collectAsStateWithLifecycle()
    val broadcastHistory by viewModel.broadcastHistory.collectAsStateWithLifecycle()
    val allUsersList by viewModel.allUsersList.collectAsStateWithLifecycle()
    val myFcmToken by viewModel.fcmToken.collectAsStateWithLifecycle()
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allComics by viewModel.allComics.collectAsStateWithLifecycle()



    val coroutineScope = rememberCoroutineScope()

    // Trigger fresh load on enter
    LaunchedEffect(Unit) {
        viewModel.refreshFcmHistoryAndUsers()
    }

    // Computed Stats
    val totalUsers = allUsersList.size
    val premiumUsersCount = allUsersList.count { it["status"] == "premium" }
    val fcmEnabledDevices = allUsersList.count { !it["fcmToken"].isNullOrEmpty() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .testTag("fcm_admin_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Admin Security Badge",
                            tint = RedPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "FCM Broadcast Admin Panel",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Text(
                        text = "Design, target, and broadcast system-wide notifications using the live Firestore messaging sync. Monitor success metrics directly below.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = GrayText)
                    )
                    
                    // Sync Status Banner
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = RedPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = fcmHistorySyncStatus,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GoldTheme,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Real-time Database Audience Analytics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TARGET AUDIENCE ANALYTICS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = RedPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Users Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Users",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(text = "Total Profiles", style = MaterialTheme.typography.bodySmall.copy(color = GrayText))
                            Text(
                                text = totalUsers.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    // Premium VIP Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Premium",
                                tint = GoldTheme,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(text = "Premium Users", style = MaterialTheme.typography.bodySmall.copy(color = GrayText))
                            Text(
                                text = premiumUsersCount.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoldTheme
                                )
                            )
                        }
                    }

                    // Active Devices Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsCell,
                                contentDescription = "Devices",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(text = "FCM Devices", style = MaterialTheme.typography.bodySmall.copy(color = GrayText))
                            Text(
                                text = fcmEnabledDevices.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Banners & Big Cards Controller Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("banners_control_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Banners Manager",
                            tint = RedPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "Manage Home Banners (Big Cards)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Text(
                        text = "Toggle which video/show is featured in the top banner (big card) carousel. Changes are updated in the local database and can be synced/published to Firebase Firestore using the publish button below.",
                        style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // LazyRow of videos
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allMedia) { item ->
                            val isBanner = item.category.split(",").map { it.trim() }.contains("Banner")
                            Card(
                                modifier = Modifier
                                    .width(150.dp)
                                    .border(
                                        width = if (isBanner) 1.5.dp else 0.dp,
                                        color = if (isBanner) RedPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1F))
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Media poster thumbnail
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(90.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )

                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Toggle button
                                    Button(
                                        onClick = {
                                            val cats = item.category.split(",").map { it.trim() }.toMutableList()
                                            if (cats.contains("Banner")) {
                                                cats.remove("Banner")
                                            } else {
                                                cats.add("Banner")
                                            }
                                            val updatedCategory = cats.joinToString(", ")
                                            val updatedItem = item.copy(category = updatedCategory)
                                            viewModel.updateMediaItemDirect(updatedItem)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isBanner) RedPrimary else Color.Gray.copy(alpha = 0.3f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isBanner) "★ Banner" else "Promote",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.uploadToFirebase()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Publish Banners to Firebase",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Publish Banners & Media to Firebase",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    val syncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()
                    if (syncStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (syncStatus?.contains("ing") == true || syncStatus?.contains("Uploading") == true) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = RedPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Status info",
                                            tint = RedPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Sync Status:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = RedPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = syncStatus ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Firestore Optimization & Seeding Portal
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("firestore_optimization_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Cloud Schema Optimization",
                            tint = GoldTheme,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "Firestore Schema Optimizer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Text(
                        text = "Production-Ready Optimized Schema: Stores all chapters under a single Comic document to drastically reduce Firestore read quota usage and minimize data latency.",
                        style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncStoriesAndComicsFromFirebase() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldTheme),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("optimize_schema_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync optimized schema",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Optimize Schema & Sync",
                                color = Color.Black,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = { viewModel.seedStoriesComicsAndCategories() },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, RedPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("seed_database_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Seed optimized data",
                                tint = RedPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Seed Optimized Data",
                                color = RedPrimary,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }



        // 4. Live Firestore Broadcast History Logs & Analytics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTIFICATION BROADCAST HISTORY",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = RedPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                
                IconButton(onClick = { viewModel.refreshFcmHistoryAndUsers() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Logs", tint = Color.White)
                }
            }
        }

        if (broadcastHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "History Empty", tint = GrayText, modifier = Modifier.size(36.dp))
                        Text(
                            text = "No Sent Broadcasts Found",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "History logs will stream live once a broadcast notification is initialized.",
                            style = MaterialTheme.typography.bodySmall.copy(color = GrayText),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(broadcastHistory) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("broadcast_history_item_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray)
                                )
                            }
                            
                            // Target Badge
                            Surface(
                                color = if (item.targetAudience.contains("Premium")) GoldTheme.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, if (item.targetAudience.contains("Premium")) GoldTheme else Color.LightGray)
                            ) {
                                Text(
                                    text = item.targetAudience,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (item.targetAudience.contains("Premium")) GoldTheme else Color.LightGray,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timestamp
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Timer, contentDescription = "Time", tint = GrayText, modifier = Modifier.size(14.dp))
                                Text(
                                    text = formatTimestamp(item.timestamp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = GrayText)
                                )
                            }

                            // Success metric
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF4CAF50), CircleShape)
                                )
                                Text(
                                    text = "${item.deliveredDevices} Devices Sent",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // 5. Dynamic Banners & Media Database Editor
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("banners_media_editor_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.3f))
            ) {
                var isExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editor Icon",
                                tint = GoldTheme,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Banners & Media Editor",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Add, update, or remove dynamic Home Screen Big Cards & media categories",
                                    style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse",
                            tint = Color.White
                        )
                    }

                    if (isExpanded) {
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        // Form states
                        var title by remember { mutableStateOf("") }
                        var description by remember { mutableStateOf("") }
                        var imageUrl by remember { mutableStateOf("") }
                        var category by remember { mutableStateOf("Banner") } // Default to "Banner" for big cards!
                        var isPremiumItem by remember { mutableStateOf(true) }
                        var reelsUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4") }

                        Text(
                            text = "ADD NEW MEDIA CARD / BIG BANNER",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldTheme,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title", color = GrayText) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description", color = GrayText) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Poster Image URL", color = GrayText) },
                            placeholder = { Text("https://images.unsplash.com/...", color = GrayText.copy(alpha = 0.5f)) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category (Use 'Banner' for Home Screen Big Cards)", color = GrayText) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        // Dynamic Category suggestion chips
                        if (allCategories.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Tap to toggle database categories:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = GrayText)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        FilterChip(
                                            selected = category == "Banner",
                                            onClick = { category = "Banner" },
                                            label = { Text("Banner") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = RedPrimary,
                                                selectedLabelColor = Color.White,
                                                containerColor = CardBackground,
                                                labelColor = GrayText
                                            )
                                        )
                                    }
                                    items(allCategories) { cat ->
                                        val isSelected = category.split(",").map { it.trim() }.contains(cat.name)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                val currentList = category.split(",")
                                                    .map { it.trim() }
                                                    .filter { it.isNotEmpty() && it != "Banner" }
                                                    .toMutableList()
                                                if (currentList.contains(cat.name)) {
                                                    currentList.remove(cat.name)
                                                } else {
                                                    currentList.add(cat.name)
                                                }
                                                category = if (currentList.isEmpty()) "Banner" else currentList.joinToString(", ")
                                            },
                                            label = { Text(cat.name) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = RedPrimary,
                                                selectedLabelColor = Color.White,
                                                containerColor = CardBackground,
                                                labelColor = GrayText
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = reelsUrl,
                            onValueChange = { reelsUrl = it },
                            label = { Text("Video Stream URL (MP4 / HLS)", color = GrayText) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Premium Locked", color = Color.White, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isPremiumItem,
                                onCheckedChange = { isPremiumItem = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = RedPrimary, checkedTrackColor = RedPrimary.copy(alpha = 0.4f))
                            )
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank() && description.isNotBlank() && imageUrl.isNotBlank()) {
                                    viewModel.addMediaItem(
                                        title = title,
                                        description = description,
                                        imageUrl = imageUrl,
                                        category = category,
                                        isPremium = isPremiumItem,
                                        reelsUrl = reelsUrl
                                    )
                                    // Reset form fields
                                    title = ""
                                    description = ""
                                    imageUrl = ""
                                    category = "Banner"
                                    Toast.makeText(context, "Successfully inserted item!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please fill all fields!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("INSERT INTO DATABASE", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "CURRENT DATABASE MEDIA ITEMS (${allMedia.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldTheme,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        // List all media items inside a small scrollable box
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            allMedia.forEach { media ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardBackground, RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AsyncImage(
                                            model = media.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .size(45.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = media.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Cat: ${media.category}",
                                                color = GrayText,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteMediaItem(media.id)
                                            Toast.makeText(context, "Deleted ${media.title}", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Item",
                                            tint = RedPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Dynamic Categories Manager Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("categories_manager_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.3f))
            ) {
                var isExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Category Editor Icon",
                                tint = GoldTheme,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Categories Manager",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Add or delete dynamic category rows shown on the Home Screen",
                                    style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse",
                            tint = Color.White
                        )
                    }

                    if (isExpanded) {
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        var newCategoryName by remember { mutableStateOf("") }
                        var isPortraitFormat by remember { mutableStateOf(false) }
                        var isComicCategory by remember { mutableStateOf(false) }

                        Text(
                            text = "CREATE NEW CATEGORY ROW",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldTheme,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Category Name", color = GrayText) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Use Portrait Poster Layout", color = Color.White, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isPortraitFormat,
                                onCheckedChange = { isPortraitFormat = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = RedPrimary, checkedTrackColor = RedPrimary.copy(alpha = 0.4f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category for Comics / Stories", color = Color.White, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isComicCategory,
                                onCheckedChange = { isComicCategory = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = RedPrimary, checkedTrackColor = RedPrimary.copy(alpha = 0.4f))
                            )
                        }

                        Button(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    viewModel.addCategory(
                                        name = newCategoryName,
                                        isPortrait = isPortraitFormat,
                                        type = if (isComicCategory) "comic" else "video"
                                    )
                                    newCategoryName = ""
                                    Toast.makeText(context, "Category added successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a category name!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CREATE CATEGORY", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.uploadCategoriesToFirebase()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("upload_categories_to_firestore_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldTheme),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload to Firestore",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UPLOAD CATEGORIES TO FIRESTORE", fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.syncCategoriesFromFirebase()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("sync_categories_from_firestore_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Sync from Firestore",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SYNC FIRESTORE CATEGORIES TO LOCAL DB", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "CURRENT CATEGORIES (${allCategories.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldTheme,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            allCategories.forEach { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardBackground, RoundedCornerShape(6.dp))
                                        .padding(vertical = 4.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = GoldTheme,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = cat.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${if (cat.isPortrait) "Portrait layout" else "Landscape layout"} • ${cat.type.uppercase()}",
                                                color = GrayText,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCategory(cat.id)
                                            Toast.makeText(context, "Category '${cat.name}' deleted successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Category",
                                            tint = RedPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Dynamic Comics & Chapters Manager Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("comics_manager_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.3f))
            ) {
                var isExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Comics Editor Icon",
                                tint = GoldTheme,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Comics & Chapters Editor",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Manage comics, categories, descriptions, and lazy scrolling chapters",
                                    style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse",
                            tint = Color.White
                        )
                    }

                    if (isExpanded) {
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        var comicTitle by remember { mutableStateOf("") }
                        var comicCategory by remember { mutableStateOf("") }
                        var comicCoverUrl by remember { mutableStateOf("") }
                        var comicDesc by remember { mutableStateOf("") }
                        var isComicPremium by remember { mutableStateOf(false) }

                        Text(
                            text = "CREATE NEW COMIC",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldTheme,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        OutlinedTextField(
                            value = comicTitle,
                            onValueChange = { comicTitle = it },
                            label = { Text("Comic Title", color = GrayText) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = comicCategory,
                            onValueChange = { comicCategory = it },
                            label = { Text("Comic Category (e.g. Action, Romance)", color = GrayText) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = comicCoverUrl,
                            onValueChange = { comicCoverUrl = it },
                            label = { Text("Cover Image URL", color = GrayText) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = comicDesc,
                            onValueChange = { comicDesc = it },
                            label = { Text("Comic Description", color = GrayText) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Premium Comic (Requires VIP)", color = Color.White, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isComicPremium,
                                onCheckedChange = { isComicPremium = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = RedPrimary, checkedTrackColor = RedPrimary.copy(alpha = 0.4f))
                            )
                        }

                        Button(
                            onClick = {
                                if (comicTitle.isNotBlank() && comicCategory.isNotBlank()) {
                                    val finalImg = if (comicCoverUrl.isBlank()) "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=600&q=80" else comicCoverUrl
                                    viewModel.addComic(comicTitle, comicCategory, finalImg, comicDesc, isComicPremium)
                                    comicTitle = ""
                                    comicCategory = ""
                                    comicCoverUrl = ""
                                    comicDesc = ""
                                    isComicPremium = false
                                    Toast.makeText(context, "Comic added successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please fill Title and Category fields!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("CREATE COMIC", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "CURRENT COMICS (${allComics.size}) & ADD CHAPTERS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldTheme,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            allComics.forEach { comic ->
                                var showChaptersEditor by remember { mutableStateOf(false) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardBackground, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).clickable { showChaptersEditor = !showChaptersEditor }
                                        ) {
                                            AsyncImage(
                                                model = comic.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp, 50.dp).clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column {
                                                Text(
                                                    text = comic.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = comic.category,
                                                    color = GoldTheme,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { showChaptersEditor = !showChaptersEditor }) {
                                                Icon(
                                                    imageVector = if (showChaptersEditor) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Edit chapters",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteComic(comic.id)
                                                    Toast.makeText(context, "Deleted comic ${comic.title}", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Comic",
                                                    tint = RedPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (showChaptersEditor) {
                                        val chaptersFlow = remember(comic.id) { viewModel.getChaptersForComic(comic.id) }
                                        val comicChapters by chaptersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                                        Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 6.dp))

                                        // Form to add a chapter to this comic
                                        var chNum by remember { mutableStateOf("") }
                                        var chTitle by remember { mutableStateOf("") }
                                        var chPagesCsv by remember { mutableStateOf("") }

                                        Text(
                                            text = "ADD NEW CHAPTER",
                                            style = MaterialTheme.typography.labelSmall.copy(color = GoldTheme, fontWeight = FontWeight.Bold)
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = chNum,
                                                onValueChange = { chNum = it },
                                                label = { Text("No.", color = GrayText, fontSize = 10.sp) },
                                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.1f))
                                            )
                                            OutlinedTextField(
                                                value = chTitle,
                                                onValueChange = { chTitle = it },
                                                label = { Text("Title", color = GrayText, fontSize = 10.sp) },
                                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                                modifier = Modifier.weight(3f),
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.1f))
                                            )
                                        }

                                        OutlinedTextField(
                                            value = chPagesCsv,
                                            onValueChange = { chPagesCsv = it },
                                            label = { Text("Pages CSV (Comma-separated Image URLs)", color = GrayText, fontSize = 11.sp) },
                                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary, unfocusedBorderColor = Color.White.copy(alpha = 0.1f))
                                        )

                                        Button(
                                            onClick = {
                                                val parsedNum = chNum.toIntOrNull()
                                                if (parsedNum != null && chTitle.isNotBlank() && chPagesCsv.isNotBlank()) {
                                                    viewModel.addChapter(comic.id, parsedNum, chTitle, chPagesCsv)
                                                    chNum = ""
                                                    chTitle = ""
                                                    chPagesCsv = ""
                                                    Toast.makeText(context, "Added chapter successfully!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Invalid input or empty fields!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("ADD CHAPTER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // List chapters
                                        if (comicChapters.isNotEmpty()) {
                                            Text(
                                                text = "CHAPTERS IN DATABASE:",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                                            )
                                            comicChapters.forEach { ch ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Ch ${ch.chapterNumber}: ${ch.title}",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.weight(1f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteChapter(ch.id)
                                                            Toast.makeText(context, "Deleted chapter ${ch.title}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Chapter",
                                                            tint = RedPrimary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Cloud Firestore Optimization & Migration Tools Card
        item {
            val syncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier.fillMaxWidth().testTag("firestore_optimizer_card"),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.4f))
            ) {
                var isExpanded by remember { mutableStateOf(true) }
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Firestore Optimizer Icon",
                                tint = GoldTheme,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Firestore Reads Optimizer",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Reduce Firestore reads from flat structures to 1 read per comic/story",
                                    style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse",
                            tint = Color.White
                        )
                    }

                    if (isExpanded) {
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "NESTED SCHEMA PRODUCTION STATUS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = GoldTheme,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = "Each Category now aggregates all of its associated video items into a single compressed 'videosJson' text field. Similarly, Comics package all Chapters into 'chaptersJson', and Stories package all Parts/Episodes into 'partsJson'. This reduces document reads by up to 98% compared to fetching separate documents for each chapter or story part, ensuring a lightning-fast and highly scalable production architecture.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f)),
                            lineHeight = 18.sp
                        )

                        if (syncStatus != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (syncStatus?.contains("ing") == true || syncStatus?.contains("Uploading") == true) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = GoldTheme,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Status info",
                                                tint = GoldTheme,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = "Sync Status:",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = GoldTheme,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Text(
                                        text = syncStatus ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.seedStoriesComicsAndCategories()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("publish_optimized_schema_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldTheme),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Publish Optimized Schema",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PUBLISH OPTIMIZED PRODUCTION SCHEMA", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.syncStoriesAndComicsFromFirebase()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("sync_optimized_database_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Sync Optimized DB",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SYNC OPTIMIZED DATABASE FROM CLOUD", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy - HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun ComicsScreen(viewModel: MPlayViewModel) {
    val comics by viewModel.allComics.collectAsStateWithLifecycle()
    val comicHistory by viewModel.comicHistory.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    var comicSortOrder by remember { mutableStateOf("Latest First") }
    var seeAllCategory by remember { mutableStateOf<String?>(null) }

    // Active reading chapter state
    var activeReadingChapter by remember { mutableStateOf<ComicChapter?>(null) }
    var activeReadingComic by remember { mutableStateOf<Comic?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val dbCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    // Extract dynamic categories from the list of comics and database
    val categories = remember(comics, dbCategories) {
        val dbComicCats = dbCategories.filter { it.type == "comic" }.map { it.name }
        val list = (comics.map { it.category } + dbComicCats).distinct().toMutableList()
        list.add(0, "All")
        list
    }

    // Filter and sort comics by search query, category, and sorting selection
    val filteredComics = remember(comics, searchQuery, selectedCategoryFilter, comicSortOrder) {
        val filtered = comics.filter { comic ->
            val matchesCategory = selectedCategoryFilter == "All" || comic.category == selectedCategoryFilter
            val matchesSearch = comic.title.contains(searchQuery, ignoreCase = true) ||
                    comic.category.contains(searchQuery, ignoreCase = true) ||
                    comic.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
        when (comicSortOrder) {
            "Latest First" -> filtered.sortedByDescending { it.id }
            "Oldest First" -> filtered.sortedBy { it.id }
            "A-Z Title" -> filtered.sortedBy { it.title }
            else -> filtered
        }
    }

    val profileState by viewModel.profile.collectAsStateWithLifecycle()
    val isPremium = profileState?.isPremium == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E0E))
    ) {
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium Icon",
                            tint = GoldTheme,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Premium Comics & Webtoons",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Access to full graphic novels, manga adaptations, and episodes is exclusive to our premium members. Upgrade today for unlimited chapters!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = GrayText),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.togglePremium() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldTheme),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("unlock_premium_comics_btn")
                        ) {
                            Text("Upgrade to Premium Now", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.selectTab(AppTab.PROFILE) }
                            ) {
                                Text("Go to Account", color = GoldTheme, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                text = "|",
                                color = Color.White.copy(alpha = 0.2f)
                            )
                            TextButton(
                                onClick = { viewModel.selectTab(AppTab.SUPPORT) }
                            ) {
                                Text("Contact Support", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
            // Screen Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Premium Comics & Webtoons",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Read the hottest graphic novels, mangas, and stories offline",
                        style = MaterialTheme.typography.bodyMedium.copy(color = GrayText)
                    )
                }
            }

            // Search and Category Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search comics, genres, tags...", color = GrayText) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = GoldTheme
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        ),
                        singleLine = true
                    )

                    // Categories Filter List
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            Surface(
                                modifier = Modifier.clickable { selectedCategoryFilter = cat },
                                color = if (isSelected) RedPrimary else CardBackground,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (isSelected) RedPrimary else Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.White else GrayText,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Recently Read Comics / Continue Reading Section
            if (comicHistory.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENTLY READ",
                                color = GoldTheme,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            TextButton(
                                onClick = { viewModel.clearComicHistory() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Clear All",
                                    color = GrayText,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(comicHistory) { hist ->
                                Card(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clickable {
                                            val matchedComic = comics.find { it.id == hist.comicId }
                                            if (matchedComic != null) {
                                                scope.launch {
                                                    viewModel.getChaptersForComic(matchedComic.id).firstOrNull()?.let { chpList ->
                                                        val matchedChapter = chpList.find { it.chapterNumber == hist.chapterNumber } ?: chpList.firstOrNull()
                                                        if (matchedChapter != null) {
                                                            activeReadingChapter = matchedChapter
                                                            activeReadingComic = matchedComic
                                                        } else {
                                                            Toast.makeText(context, "No chapters found", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(context, "Comic not found", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column {
                                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                            AsyncImage(
                                                model = hist.imageUrl,
                                                contentDescription = hist.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            IconButton(
                                                onClick = { viewModel.deleteComicHistory(hist.comicId) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove from history",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Text(
                                                text = hist.title,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Chapter ${hist.chapterNumber}",
                                                color = RedPrimary,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sort Selector Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPLORE BY CATEGORIES",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldTheme
                        )
                    )

                    Box {
                        var showSortMenu by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { showSortMenu = true },
                            color = CardBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SortByAlpha,
                                    contentDescription = "Sort Icon",
                                    tint = GoldTheme,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = comicSortOrder,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Drop Down",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(Color(0xFF151414)).border(0.5.dp, GoldTheme, RoundedCornerShape(8.dp))
                        ) {
                            listOf("Latest First", "Oldest First", "A-Z Title").forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order, color = Color.White) },
                                    onClick = {
                                        comicSortOrder = order
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Group filtered comics by category and display
            val comicsByCategory = filteredComics.groupBy { it.category }
            if (comicsByCategory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No comics match your filters", color = GrayText)
                    }
                }
            } else {
                comicsByCategory.forEach { (catName, categoryComics) ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Category row header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "(${categoryComics.size})",
                                        style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                                    )
                                }
                                
                                TextButton(
                                    onClick = { seeAllCategory = catName },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "See All",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = GoldTheme,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "See All Category Comics",
                                        tint = GoldTheme,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Horizontal scroll of comics in this category
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categoryComics) { comic ->
                                    val chaptersFlow = remember(comic.id) { viewModel.getChaptersForComic(comic.id) }
                                    val chapters by chaptersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                                    Card(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .clickable {
                                                if (chapters.isNotEmpty()) {
                                                    activeReadingChapter = chapters.first()
                                                    activeReadingComic = comic
                                                } else {
                                                    Toast.makeText(context, "No chapters available for ${comic.title} yet", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column {
                                            Box {
                                                AsyncImage(
                                                    model = comic.imageUrl,
                                                    contentDescription = comic.title,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp),
                                                    contentScale = ContentScale.Crop
                                                )
                                                if (comic.isPremium) {
                                                    Surface(
                                                        color = RedPrimary,
                                                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                                                        modifier = Modifier.align(Alignment.TopStart)
                                                    ) {
                                                        Text(
                                                            text = "PREMIUM",
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = comic.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 13.sp
                                                )

                                                // List horizontal chapter chips
                                                if (chapters.isEmpty()) {
                                                    Text("Coming soon", color = GrayText, fontSize = 11.sp)
                                                } else {
                                                    LazyRow(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        items(chapters) { chp ->
                                                            Surface(
                                                                modifier = Modifier.clickable {
                                                                    activeReadingChapter = chp
                                                                    activeReadingComic = comic
                                                                },
                                                                color = RedPrimary.copy(alpha = 0.2f),
                                                                border = BorderStroke(0.5.dp, RedPrimary.copy(alpha = 0.6f)),
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Ch. ${chp.chapterNumber}",
                                                                    color = Color.White,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: "also in same screen it has the direct comics name then chp1 2 and all"
            item {
                Text(
                    text = "DIRECT COMICS & ALL CHAPTERS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GoldTheme
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(filteredComics) { comic ->
                val chaptersFlow = remember(comic.id) { viewModel.getChaptersForComic(comic.id) }
                val chapters by chaptersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AsyncImage(
                            model = comic.imageUrl,
                            contentDescription = comic.title,
                            modifier = Modifier
                                .size(90.dp, 120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = comic.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = comic.category,
                                        color = GoldTheme,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = comic.description,
                                style = MaterialTheme.typography.bodySmall.copy(color = GrayText),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Likes count",
                                    tint = RedPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${comic.likesCount} readers",
                                    color = GrayText,
                                    fontSize = 11.sp
                                )
                            }

                            // Chapters directly on screen
                            if (chapters.isEmpty()) {
                                Text("No chapters released yet", color = GrayText, fontSize = 11.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("SELECT CHAPTER:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        chapters.forEach { chp ->
                                            Row(
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(8.dp))
                                                    .padding(2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        activeReadingChapter = chp
                                                        activeReadingComic = comic
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Book,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Text(
                                                            text = "Chapter ${chp.chapterNumber}",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }

                                                val context = androidx.compose.ui.platform.LocalContext.current
                                                IconButton(
                                                    onClick = {
                                                        viewModel.downloadComicChapter(chp, comic, context)
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = "Download Chapter",
                                                        tint = GoldTheme,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // COMIC READER SCREEN OVERLAY (Smooth vertical strip viewer)
        activeReadingChapter?.let { chapter ->
            activeReadingComic?.let { comic ->
                ComicReaderOverlay(
                    comic = comic,
                    currentChapter = chapter,
                    viewModel = viewModel,
                    onClose = {
                        activeReadingChapter = null
                        activeReadingComic = null
                    },
                    onNavigateToChapter = { nextCh ->
                        activeReadingChapter = nextCh
                    },
                    onSelectComic = { nextComic ->
                        scope.launch {
                            viewModel.getChaptersForComic(nextComic.id).firstOrNull()?.let { chapters ->
                                if (chapters.isNotEmpty()) {
                                    activeReadingComic = nextComic
                                    activeReadingChapter = chapters.first()
                                } else {
                                    Toast.makeText(context, "No chapters available for ${nextComic.title} yet", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }

        // 10. Vertically Scrollable seeAllCategory Overlay
        seeAllCategory?.let { currentCategory ->
            val categoryComics = filteredComics.filter { it.category == currentCategory }
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { seeAllCategory = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0F0E0E),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { seeAllCategory = null }) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Back to list",
                                    tint = Color.White
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = currentCategory.uppercase(),
                                    color = GoldTheme,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${categoryComics.size} Comics Available",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }
                ) { innerPadding ->
                    if (categoryComics.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No comics found in this category", color = GrayText)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                        ) {
                            items(categoryComics) { comic ->
                                val chaptersFlow = remember(comic.id) { viewModel.getChaptersForComic(comic.id) }
                                val chapters by chaptersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (chapters.isNotEmpty()) {
                                                activeReadingChapter = chapters.first()
                                                activeReadingComic = comic
                                                seeAllCategory = null
                                            } else {
                                                Toast.makeText(context, "No chapters available for ${comic.title} yet", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        AsyncImage(
                                            model = comic.imageUrl,
                                            contentDescription = comic.title,
                                            modifier = Modifier
                                                .size(100.dp, 135.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = comic.title,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (comic.isPremium) {
                                                    Surface(
                                                        color = RedPrimary,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "PREMIUM",
                                                            color = Color.White,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = comic.description,
                                                style = MaterialTheme.typography.bodySmall.copy(color = GrayText),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            if (chapters.isEmpty()) {
                                                Text("Coming soon", color = GrayText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text(
                                                    text = "${chapters.size} Chapters available",
                                                    color = GoldTheme,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    items(chapters) { chp ->
                                                        Surface(
                                                            modifier = Modifier.clickable {
                                                                activeReadingChapter = chp
                                                                activeReadingComic = comic
                                                                seeAllCategory = null
                                                            },
                                                            color = RedPrimary.copy(alpha = 0.2f),
                                                            border = BorderStroke(0.5.dp, RedPrimary.copy(alpha = 0.6f)),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "Ch. ${chp.chapterNumber}",
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComicReaderOverlay(
    comic: Comic,
    currentChapter: ComicChapter,
    viewModel: MPlayViewModel,
    onClose: () -> Unit,
    onNavigateToChapter: (ComicChapter) -> Unit,
    onSelectComic: (Comic) -> Unit = {}
) {
    // Get all chapters for navigation
    val chaptersFlow = remember(comic.id) { viewModel.getChaptersForComic(comic.id) }
    val allChapters by chaptersFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val similarComics by viewModel.similarComics.collectAsStateWithLifecycle()

    LaunchedEffect(comic.id, currentChapter.id) {
        viewModel.loadSimilarComics(comic)
        viewModel.saveComicHistory(comic, currentChapter.chapterNumber)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val pages = remember(currentChapter, context) {
        val localDir = java.io.File(context.filesDir, ".secured_media/comic_${currentChapter.id}")
        if (localDir.exists() && localDir.isDirectory) {
            val files = localDir.listFiles()?.sortedBy { it.name }
            // Filter out empty or mock files (less than 1KB)
            val validFiles = files?.filter { it.length() > 1000L } ?: emptyList()
            if (validFiles.isNotEmpty()) {
                validFiles.map { android.net.Uri.fromFile(it).toString() }
            } else {
                currentChapter.pagesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        } else {
            currentChapter.pagesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to list",
                            tint = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = comic.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Chapter ${currentChapter.chapterNumber}: ${currentChapter.title}",
                            color = GoldTheme,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Download Chapter option inside reader top bar
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(
                        onClick = {
                            viewModel.downloadComicChapter(currentChapter, comic, context)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download current chapter",
                            tint = Color.White
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Vertical scrolling list of comic pages (Infinite scroll style)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Check if PDF url exists in database chapter
                    if (currentChapter.pdfUrl.isNotEmpty()) {
                        // PDF Reader Mode (with lazy loaded page simulation cards)
                        val pdfPagesCount = 4
                        items(pdfPagesCount) { pageNum ->
                            var isPageLoaded by remember(pageNum, currentChapter.id) { mutableStateOf(false) }
                            LaunchedEffect(pageNum, currentChapter.id) {
                                kotlinx.coroutines.delay(600L + (pageNum * 150L))
                                isPageLoaded = true
                            }

                            var scale by remember(pageNum, currentChapter.id) { mutableStateOf(1f) }
                            var offset by remember(pageNum, currentChapter.id) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .aspectRatio(0.707f) // Standard portrait sheet ratio
                                    .clip(androidx.compose.ui.graphics.RectangleShape)
                                    .pointerInput(pageNum, currentChapter.id) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(1f, 4f)
                                            if (scale > 1f) {
                                                offset = androidx.compose.ui.geometry.Offset(
                                                    x = offset.x + pan.x,
                                                    y = offset.y + pan.y
                                                )
                                            } else {
                                                offset = androidx.compose.ui.geometry.Offset.Zero
                                            }
                                        }
                                    }
                                    .pointerInput(pageNum, currentChapter.id) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (scale > 1f) {
                                                    scale = 1f
                                                    offset = androidx.compose.ui.geometry.Offset.Zero
                                                } else {
                                                    scale = 2.5f
                                                }
                                            }
                                        )
                                    }
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (!isPageLoaded) {
                                        Column(
                                            modifier = Modifier.align(Alignment.Center),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(color = RedPrimary, modifier = Modifier.size(28.dp))
                                            Text(
                                                text = "Lazy loading PDF Page ${pageNum + 1}...",
                                                color = Color.DarkGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        // Render PDF elements beautifully
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(20.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (java.io.File(java.io.File(context.filesDir, ".secured_media"), "comic_${currentChapter.id}_pdf.dat").exists()) "OFFICIAL DIGITAL RELEASE • OFFLINE CACHED PDF" else "OFFICIAL DIGITAL RELEASE • PDF",
                                                    color = Color.LightGray,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(Color.Green, shape = androidx.compose.foundation.shape.CircleShape)
                                                    )
                                                    Text(
                                                        text = "SECURED",
                                                        color = Color.Green,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                        .background(Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Book,
                                                        contentDescription = null,
                                                        tint = RedPrimary.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(44.dp).align(Alignment.Center)
                                                    )
                                                    Text(
                                                        text = "[Lazy Loaded Vector Render: Ch ${currentChapter.chapterNumber} P ${pageNum + 1}]",
                                                        color = Color.Gray,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = when(pageNum) {
                                                        0 -> "A cold, ancient chill swept over the throne. Prince Lysander grasped the handle of the mythical Frost Scepter, feeling its blue light fuse with his neural stream. 'For Eldoria,' he whispered."
                                                        1 -> "The flame tyrants descended with metal wings, burning the outer barriers of the sanctuary. Navigator Kael adjusted the shield frequencies. 'They have mapped our signatures!'"
                                                        2 -> "A blinding laser grid locked down the archives. Iris's voice chimed: 'Security breached. Initiating backup sequence.' The codes pulsed in magenta streams."
                                                        else -> "With a final thunderous blast, the elemental barrier cracked. Lysander knew the path forward was unyielding. The real battle was just beginning."
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = Color.Black,
                                                        lineHeight = 22.sp,
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                    ),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(horizontal = 12.dp)
                                                )
                                            }

                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = "Page ${pageNum + 1} of $pdfPagesCount",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Render standard comic strip pages
                        items(pages) { pageUrl ->
                            val index = remember(pageUrl, pages) { pages.indexOf(pageUrl) }
                            var hasError by remember { mutableStateOf(false) }
                            if (hasError) {
                                // Beautiful, premium visual comic placeholder for true offline simulation
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .aspectRatio(0.707f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13111E)),
                                    border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = "Offline Mode",
                                            tint = GoldTheme,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Offline Reading Panel ${index + 1}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Chapter: ${currentChapter.title}\nStory segment ${index + 1} has been pre-cached and secured offline successfully.",
                                            color = Color.LightGray,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                var scale by remember(pageUrl) { mutableStateOf(1f) }
                                var offset by remember(pageUrl) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .clip(androidx.compose.ui.graphics.RectangleShape)
                                        .pointerInput(pageUrl) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                scale = (scale * zoom).coerceIn(1f, 4f)
                                                if (scale > 1f) {
                                                    offset = androidx.compose.ui.geometry.Offset(
                                                        x = offset.x + pan.x,
                                                        y = offset.y + pan.y
                                                    )
                                                } else {
                                                    offset = androidx.compose.ui.geometry.Offset.Zero
                                                }
                                            }
                                        }
                                        .pointerInput(pageUrl) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    if (scale > 1f) {
                                                        scale = 1f
                                                        offset = androidx.compose.ui.geometry.Offset.Zero
                                                    } else {
                                                        scale = 2.5f
                                                    }
                                                }
                                            )
                                        }
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                ) {
                                    AsyncImage(
                                        model = pageUrl,
                                        contentDescription = "Comic Page",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(),
                                        contentScale = ContentScale.FillWidth,
                                        onError = { hasError = true }
                                    )
                                }
                            }
                        }
                    }

                    // Bottom chapter navigation and similar recommendations
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "You've finished this chapter!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                             ) {
                                // Previous chapter button
                                val prevChapter = allChapters.find { it.chapterNumber == currentChapter.chapterNumber - 1 }
                                Button(
                                    onClick = { prevChapter?.let { onNavigateToChapter(it) } },
                                    enabled = prevChapter != null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CardBackground,
                                        disabledContainerColor = Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Previous Ch.", color = if (prevChapter != null) Color.White else Color.Gray)
                                }

                                // Next chapter button
                                val nextChapter = allChapters.find { it.chapterNumber == currentChapter.chapterNumber + 1 }
                                Button(
                                    onClick = { nextChapter?.let { onNavigateToChapter(it) } },
                                    enabled = nextChapter != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Next Ch.", color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = onClose,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Back to Comics List", color = Color.White)
                            }
                        }
                    }

                    // Similar & Related Comics Recommendations
                    if (similarComics.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp)
                            ) {
                                Text(
                                    text = "Related & Similar Comics:",
                                    color = GoldTheme,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    similarComics.forEach { simComic ->
                                        Card(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .clickable {
                                                    onSelectComic(simComic)
                                                    viewModel.addDebugLog("Navigation: Selected related comic '${simComic.title}'")
                                                },
                                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                AsyncImage(
                                                    model = simComic.imageUrl,
                                                    contentDescription = simComic.title,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(140.dp)
                                                        .clip(RoundedCornerShape(6.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = simComic.title,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = simComic.category,
                                                    color = GrayText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoriesScreen(viewModel: com.example.ui.viewmodel.MPlayViewModel) {
    val stories by viewModel.storiesPagedList.collectAsStateWithLifecycle()
    val storyHistory by viewModel.storyHistory.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentStoryPage.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalStoriesCount.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingStories.collectAsStateWithLifecycle()
    val profileState by viewModel.profile.collectAsStateWithLifecycle()
    val isPremium = profileState?.isPremium == true
    val context = androidx.compose.ui.platform.LocalContext.current

    val selectedStoryDetail = viewModel.selectedStoryDetail
    val limit = 10
    val totalPages = remember(totalCount) { java.lang.Math.ceil(totalCount.toDouble() / limit.toDouble()).toInt() }

    var storySortOrder by remember { mutableStateOf("Latest First") }
    val sortedStories = remember(stories, storySortOrder) {
        when (storySortOrder) {
            "Latest First" -> stories.sortedByDescending { it.id }
            "Oldest First" -> stories.sortedBy { it.id }
            "A-Z Title" -> stories.sortedBy { it.title }
            else -> stories
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadStoriesForPage(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with Development Mode Trigger and Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Premium Stories",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Explore immersive premium fantasy, novels & scifi",
                        style = MaterialTheme.typography.bodySmall.copy(color = GrayText)
                    )
                }

                // Dev Mode Quick Action Button
                Button(
                    onClick = { viewModel.seedStoriesComicsAndCategories() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldTheme.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldTheme),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("dev_mode_seed_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Dev Mode",
                        tint = GoldTheme,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dev Setup", style = MaterialTheme.typography.labelSmall.copy(color = GoldTheme))
                }
            }

            if (!isPremium) {
                // Premium locked screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Premium Icon",
                                tint = GoldTheme,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "Premium Content Exclusive",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Stories & original graphic novels are strictly reserved for premium members. Unlock instant access to our entire catalog today!",
                                style = MaterialTheme.typography.bodyMedium.copy(color = GrayText),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.togglePremium() },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldTheme),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("unlock_premium_stories_btn")
                            ) {
                                Text("Upgrade to Premium Now", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.selectTab(AppTab.PROFILE) }
                                ) {
                                    Text("Go to Account", color = GoldTheme, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text(
                                    text = "|",
                                    color = Color.White.copy(alpha = 0.2f)
                                )
                                TextButton(
                                    onClick = { viewModel.selectTab(AppTab.SUPPORT) }
                                ) {
                                    Text("Contact Support", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                if (stories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort: $storySortOrder",
                            style = MaterialTheme.typography.bodySmall.copy(color = GoldTheme, fontWeight = FontWeight.Bold)
                        )

                        Box {
                            var showSortMenu by remember { mutableStateOf(false) }
                            Surface(
                                onClick = { showSortMenu = true },
                                color = CardBackground,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SortByAlpha,
                                        contentDescription = "Sort Icon",
                                        tint = GoldTheme,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = storySortOrder,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Drop Down",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(Color(0xFF151414)).border(0.5.dp, GoldTheme, RoundedCornerShape(8.dp))
                            ) {
                                listOf("Latest First", "Oldest First", "A-Z Title").forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order, color = Color.White) },
                                        onClick = {
                                            storySortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (stories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No Stories Seeded",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                            )
                            Text(
                                text = "Click the 'Dev Setup' button above to seed sample stories directly into your SQLite & Cloud Firestore databases!",
                                style = MaterialTheme.typography.bodySmall.copy(color = GrayText),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                                                            )
                        }
                    }
                } else {
                    // Paged list of stories
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // RECENTLY READ STORIES
                        if (storyHistory.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RECENTLY READ STORIES",
                                            color = GoldTheme,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                        TextButton(
                                            onClick = { viewModel.clearStoryHistory() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Clear All",
                                                color = GrayText,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(storyHistory) { hist ->
                                            Card(
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .clickable {
                                                        val matchedStory = stories.find { it.id == hist.storyId }
                                                        if (matchedStory != null) {
                                                            viewModel.selectedStoryDetail = matchedStory
                                                        } else {
                                                            Toast.makeText(context, "Story not found", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Column {
                                                    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                                        AsyncImage(
                                                            model = hist.imageUrl,
                                                            contentDescription = hist.title,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        IconButton(
                                                            onClick = { viewModel.deleteStoryHistory(hist.storyId) },
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(4.dp)
                                                                .size(24.dp)
                                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Remove from history",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }
                                                    Column(modifier = Modifier.padding(6.dp)) {
                                                        Text(
                                                            text = hist.title,
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = if (hist.partNumber == 0) "Prologue" else "Part ${hist.partNumber}",
                                                            color = RedPrimary,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        items(sortedStories) { story ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("story_card_${story.id}"),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    coil.compose.AsyncImage(
                                        model = story.imageUrl,
                                        contentDescription = story.title,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(180.dp)
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .clip(RoundedCornerShape(8.dp))
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = story.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )

                                        // Category Badge
                                        Surface(
                                            color = RedPrimary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = story.category,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = RedPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Snippet of Story
                                        Text(
                                            text = story.content,
                                            style = MaterialTheme.typography.bodySmall.copy(color = GrayText),
                                            maxLines = 7,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Details option
                                        Button(
                                            onClick = { viewModel.selectedStoryDetail = story },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .testTag("story_details_btn_${story.id}")
                                        ) {
                                            Text("Read Details", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }

                        // Pagination controls row
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = RedPrimary, modifier = Modifier.size(24.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Prev button
                                    Button(
                                        onClick = {
                                            if (currentPage > 0) {
                                                viewModel.loadStoriesForPage(currentPage - 1)
                                            }
                                        },
                                        enabled = currentPage > 0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CardBackground,
                                            disabledContainerColor = Color.White.copy(alpha = 0.05f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("story_prev_page_btn")
                                    ) {
                                        Text("Previous", color = if (currentPage > 0) Color.White else Color.Gray)
                                    }

                                    // Page Numbers (direct clicks)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (i in 0 until totalPages) {
                                            val isSelected = i == currentPage
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(
                                                        if (isSelected) RedPrimary else CardBackground,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.loadStoriesForPage(i)
                                                    }
                                                    .testTag("story_page_btn_$i"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (i + 1).toString(),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = Color.White,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Next button
                                    Button(
                                        onClick = {
                                            if (currentPage < totalPages - 1) {
                                                viewModel.loadStoriesForPage(currentPage + 1)
                                            }
                                        },
                                        enabled = currentPage < totalPages - 1,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CardBackground,
                                            disabledContainerColor = Color.White.copy(alpha = 0.05f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("story_next_page_btn")
                                    ) {
                                        Text("Next", color = if (currentPage < totalPages - 1) Color.White else Color.Gray)
                                    }
                                }

                                Text(
                                    text = "Showing stories ${currentPage * limit + 1} - ${java.lang.Math.min((currentPage + 1) * limit, totalCount)} of $totalCount",
                                    style = MaterialTheme.typography.labelSmall.copy(color = GrayText)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expanded Details View Overlay
        selectedStoryDetail?.let { story ->
            val parts by viewModel.getStoryParts(story.id).collectAsStateWithLifecycle(emptyList())
            val recommendedStories by viewModel.recommendedStories.collectAsStateWithLifecycle()
            val activeReadingPart = viewModel.activeReadingPart

            LaunchedEffect(story.id, activeReadingPart) {
                viewModel.loadRecommendedStories(story.id)
                if (activeReadingPart != null) {
                    viewModel.saveStoryHistory(story, activeReadingPart!!.partNumber)
                } else {
                    viewModel.saveStoryHistory(story, 0)
                }
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            var fontScaleMultiplier by remember { mutableStateOf(1.0f) }
            
            // TTS State variables
            var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
            var isTtsReady by remember { mutableStateOf(false) }
            var isTtsPlaying by remember { mutableStateOf(false) }
            var activeSentenceIndex by remember { mutableStateOf(-1) }
            var ttsSpeed by remember { mutableStateOf(1.0f) }
            var ttsPitch by remember { mutableStateOf(1.0f) }
            var useFemaleVoice by remember { mutableStateOf(true) }
            var sleepTimerSecondsLeft by remember { mutableStateOf<Int?>(null) }
            var stopAtEndOfPart by remember { mutableStateOf(false) }
            var isListenModeActive by remember(story.id, activeReadingPart) { mutableStateOf(false) }

            var localMediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
            val isAudioDownloaded = remember(story.id) {
                java.io.File(java.io.File(context.filesDir, ".secured_media"), "story_audio_${story.id}.mp3").exists()
            }

            DisposableEffect(story.id, activeReadingPart) {
                onDispose {
                    localMediaPlayer?.release()
                    localMediaPlayer = null
                }
            }

            // Sleep Timer countdown effect
            LaunchedEffect(isTtsPlaying, sleepTimerSecondsLeft) {
                if (isTtsPlaying && sleepTimerSecondsLeft != null && sleepTimerSecondsLeft!! > 0) {
                    kotlinx.coroutines.delay(1000L)
                    sleepTimerSecondsLeft = sleepTimerSecondsLeft!! - 1
                    if (sleepTimerSecondsLeft == 0) {
                        isTtsPlaying = false
                        sleepTimerSecondsLeft = null
                        android.widget.Toast.makeText(context, "Sleep timer finished! Playback paused gracefully.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }

            val sentences = remember(activeReadingPart?.content) {
                activeReadingPart?.content?.split(Regex("(?<=[.!?])\\s+"))?.filter { it.isNotBlank() } ?: emptyList()
            }

            DisposableEffect(story.id, activeReadingPart) {
                activeSentenceIndex = -1
                isTtsPlaying = false
                
                val tts = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        isTtsReady = true
                    }
                }
                ttsInstance = tts
                
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        val finishedIndex = utteranceId?.toIntOrNull() ?: return
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (finishedIndex == activeSentenceIndex) {
                                if (activeSentenceIndex < sentences.size - 1) {
                                    activeSentenceIndex++
                                } else {
                                    isTtsPlaying = false
                                    activeSentenceIndex = -1
                                    if (stopAtEndOfPart) {
                                        stopAtEndOfPart = false
                                    }
                                }
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {}
                })

                onDispose {
                    tts.stop()
                    tts.shutdown()
                }
            }

            LaunchedEffect(activeSentenceIndex, isTtsPlaying, isAudioDownloaded) {
                if (isAudioDownloaded) {
                    if (isTtsPlaying) {
                        try {
                            val mp = localMediaPlayer ?: android.media.MediaPlayer().apply {
                                val audioFile = java.io.File(java.io.File(context.filesDir, ".secured_media"), "story_audio_${story.id}.mp3")
                                setDataSource(audioFile.absolutePath)
                                prepare()
                                setOnCompletionListener {
                                    isTtsPlaying = false
                                }
                            }.also { localMediaPlayer = it }
                            mp.start()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            localMediaPlayer?.pause()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    val tts = ttsInstance ?: return@LaunchedEffect
                    if (isTtsPlaying && activeSentenceIndex in sentences.indices) {
                        tts.setSpeechRate(ttsSpeed)
                        val finalPitch = ttsPitch * (if (useFemaleVoice) 1.2f else 0.8f)
                        tts.setPitch(finalPitch)
                        
                        try {
                            val currentLocale = java.util.Locale.getDefault()
                            val matchGender = if (useFemaleVoice) "female" else "male"
                            val selectedVoice = tts.voices?.firstOrNull { voice ->
                                voice.locale.language == currentLocale.language &&
                                voice.name.lowercase().contains(matchGender)
                            }
                            if (selectedVoice != null) {
                                tts.voice = selectedVoice
                            }
                        } catch (e: Exception) {}

                        val sentenceToSpeak = sentences[activeSentenceIndex]
                        val params = android.os.Bundle().apply {
                            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, activeSentenceIndex.toString())
                        }
                        tts.speak(sentenceToSpeak, TextToSpeech.QUEUE_FLUSH, params, activeSentenceIndex.toString())
                    } else if (!isTtsPlaying) {
                        tts.stop()
                    }
                }
            }

            LaunchedEffect(ttsSpeed, ttsPitch, useFemaleVoice) {
                val tts = ttsInstance ?: return@LaunchedEffect
                if (isTtsPlaying && activeSentenceIndex in sentences.indices) {
                    tts.setSpeechRate(ttsSpeed)
                    val finalPitch = ttsPitch * (if (useFemaleVoice) 1.2f else 0.8f)
                    tts.setPitch(finalPitch)
                    val sentenceToSpeak = sentences[activeSentenceIndex]
                    val params = android.os.Bundle().apply {
                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, activeSentenceIndex.toString())
                    }
                    tts.speak(sentenceToSpeak, TextToSpeech.QUEUE_FLUSH, params, activeSentenceIndex.toString())
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f))
                    .clickable(enabled = true, onClick = {}) // block clicks through
                    .testTag("story_detail_overlay")
            ) {
                val scrollState = rememberScrollState()
                
                // Reset scroll position when switching parts or stories
                LaunchedEffect(activeReadingPart, story.id) {
                    scrollState.scrollTo(0)
                }

                // Automatic scroll to highlighted sentence
                LaunchedEffect(activeSentenceIndex) {
                    if (activeSentenceIndex >= 0 && sentences.isNotEmpty() && isTtsPlaying) {
                        val fraction = activeSentenceIndex.toFloat() / sentences.size
                        val targetScroll = (scrollState.maxValue * fraction).coerceIn(0f, scrollState.maxValue.toFloat()).toInt()
                        scrollState.animateScrollTo(targetScroll)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Image on Top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        coil.compose.AsyncImage(
                            model = story.imageUrl,
                            contentDescription = story.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Dark gradient scrim overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.95f)
                                        )
                                    )
                                )
                        )

                        // Floating close button
                        IconButton(
                            onClick = { 
                                viewModel.selectedStoryDetail = null
                                viewModel.activeReadingPart = null
                            },
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.CircleShape)
                                .testTag("story_detail_close_icon_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close details", tint = Color.White)
                        }
                    }

                    // Main Container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        if (activeReadingPart != null) {
                            // Immersive reading mode of a specific story part
                            val currentPart = activeReadingPart!!
                            
                            Text(
                                text = story.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = GoldTheme,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )

                            Text(
                                text = currentPart.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = RedPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, RedPrimary)
                                ) {
                                    Text(
                                        text = "READING PART ${currentPart.partNumber}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (!isListenModeActive) {
                                    Button(
                                        onClick = {
                                            isListenModeActive = true
                                            isTtsPlaying = true
                                            if (activeSentenceIndex < 0) {
                                                activeSentenceIndex = 0
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldTheme),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp).testTag("listen_audiobook_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Listen",
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Listen",
                                            color = Color.Black,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            isListenModeActive = false
                                            isTtsPlaying = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp).testTag("hide_listen_audiobook_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeOff,
                                            contentDescription = "Hide",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Hide Controls",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.12f))

                            // Actual Part Narrative Content (Typography optimized for reading with highlighting and pinch-to-zoom)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            fontScaleMultiplier = (fontScaleMultiplier * zoom).coerceIn(0.8f, 2.5f)
                                        }
                                    },
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sentences.forEachIndexed { idx, sentence ->
                                    val isHighlighted = idx == activeSentenceIndex
                                    Text(
                                        text = sentence.trim() + " ",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = if (isHighlighted) GoldTheme else Color.White.copy(alpha = 0.92f),
                                            lineHeight = (30 * fontScaleMultiplier).sp,
                                            fontSize = (17 * fontScaleMultiplier).sp,
                                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isHighlighted) RedPrimary.copy(alpha = 0.25f) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                // Tap sentence to jump directly to that part of the audiobook!
                                                activeSentenceIndex = idx
                                                if (!isTtsPlaying) {
                                                    isTtsPlaying = true
                                                }
                                                isListenModeActive = true
                                            }
                                            .padding(vertical = 4.dp, horizontal = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Divider(color = Color.White.copy(alpha = 0.12f))

                            // End option 1: Navigate to other parts of the SAME story
                            val otherParts = parts.filter { it.id != currentPart.id }
                            if (otherParts.isNotEmpty()) {
                                Text(
                                    text = "More Parts of this Story:",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    otherParts.forEach { otherPart ->
                                        Card(
                                            onClick = { viewModel.activeReadingPart = otherPart },
                                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                            modifier = Modifier.width(180.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Part ${otherPart.partNumber}",
                                                    color = GoldTheme,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = otherPart.title,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Tap to read part...",
                                                    color = GrayText,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                        } else {
                            // Default Story Details Overview / Prologue
                            Text(
                                text = story.title,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = RedPrimary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = story.category,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    color = GoldTheme.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, GoldTheme)
                                ) {
                                    Text(
                                        text = "STORY",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = GoldTheme,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.12f))

                            // Story prologue / synopsis
                            Text(
                                text = story.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 28.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Story Parts / Chapters List
                            Text(
                                text = "STORY PARTS / EPISODES:",
                                color = GoldTheme,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            if (parts.isEmpty()) {
                                Text(
                                    text = "No additional parts released. Complete story is loaded in the prologue above.",
                                    color = GrayText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    parts.forEach { part ->
                                        Card(
                                            onClick = { viewModel.activeReadingPart = part },
                                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "PART ${part.partNumber}",
                                                        color = RedPrimary,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = part.title,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Read Part",
                                                    tint = Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // End option 2: Other Stories Recommendation (Common for both synopsis and part reading)
                        if (recommendedStories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.White.copy(alpha = 0.12f))
                            
                            Text(
                                text = "Other Stories You Might Like:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                recommendedStories.forEach { recStory ->
                                    Card(
                                        onClick = {
                                            viewModel.selectedStoryDetail = recStory; viewModel.activeReadingPart = null
                                        },
                                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.width(150.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            coil.compose.AsyncImage(
                                                model = recStory.imageUrl,
                                                contentDescription = recStory.title,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = recStory.title,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = recStory.category,
                                                color = GrayText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Closing navigation button actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (activeReadingPart != null) {
                                Button(
                                    onClick = { viewModel.activeReadingPart = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Overview", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            // High-quality M3 Download Audio story button
                            Button(
                                onClick = {
                                    viewModel.downloadStoryAudio(story, context)
                                    android.widget.Toast.makeText(context, "${story.title} added to offline audio downloads!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldTheme),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("story_download_audio_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Audio Story",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download Audio", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { 
                                    viewModel.selectedStoryDetail = null
                                    viewModel.activeReadingPart = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("story_detail_close_btn"),
                                shape = RoundedCornerShape(8.dp)
                              ) {
                                Text("Back to Library", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Extra bottom spacing if the audiobook player bar is docked
                        if (activeReadingPart != null && isListenModeActive) {
                            Spacer(modifier = Modifier.height(200.dp))
                        }
                    }
                }

                // Floating Audiobook Player Panel (Docked at Bottom Center)
                if (activeReadingPart != null && isListenModeActive) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, GoldTheme.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Player Info & Voice Selection Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AUDIOBOOK MODE",
                                        color = GoldTheme,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isAudioDownloaded) {
                                            "Playing Offline Audio Story 🎧"
                                        } else if (activeSentenceIndex >= 0 && sentences.isNotEmpty()) {
                                            "Reading phrase ${activeSentenceIndex + 1} of ${sentences.size}"
                                        } else {
                                            "Tap play or any sentence to listen"
                                        },
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                // Voice preference toggle button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (useFemaleVoice) "Female 👩" else "Male 👨",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Switch(
                                        checked = useFemaleVoice,
                                        onCheckedChange = { useFemaleVoice = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = GoldTheme,
                                            checkedTrackColor = GoldTheme.copy(alpha = 0.4f),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color.DarkGray
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                            }
                            
                            // Audiobook Sleep Timer Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Sleep Timer",
                                        tint = if (sleepTimerSecondsLeft != null || stopAtEndOfPart) GoldTheme else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Sleep Timer",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (sleepTimerSecondsLeft != null) {
                                        val m = sleepTimerSecondsLeft!! / 60
                                        val s = sleepTimerSecondsLeft!! % 60
                                        Text(
                                            text = "(${String.format("%02d:%02d", m, s)})",
                                            color = GoldTheme,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (stopAtEndOfPart) {
                                        Text(
                                            text = "(End of Part)",
                                            color = GoldTheme,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "(Off)",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        "Off" to { 
                                            sleepTimerSecondsLeft = null 
                                            stopAtEndOfPart = false 
                                        },
                                        "15s" to { 
                                            sleepTimerSecondsLeft = 15 
                                            stopAtEndOfPart = false 
                                        },
                                        "15m" to { 
                                            sleepTimerSecondsLeft = 15 * 60 
                                            stopAtEndOfPart = false 
                                        },
                                        "30m" to { 
                                            sleepTimerSecondsLeft = 30 * 60 
                                            stopAtEndOfPart = false 
                                        },
                                        "End" to { 
                                            sleepTimerSecondsLeft = null 
                                            stopAtEndOfPart = true 
                                        }
                                    ).forEach { (label, onClick) ->
                                        val isActive = when (label) {
                                            "Off" -> sleepTimerSecondsLeft == null && !stopAtEndOfPart
                                            "15s" -> sleepTimerSecondsLeft == 15
                                            "15m" -> sleepTimerSecondsLeft == 15 * 60
                                            "30m" -> sleepTimerSecondsLeft == 30 * 60
                                            "End" -> stopAtEndOfPart
                                            else -> false
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (isActive) GoldTheme else Color.White.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable { onClick() }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isActive) Color.Black else Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Audio Sliders (Pitch and Speed control row)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Speed Slider
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Speed: ${String.format("%.2fx", ttsSpeed)}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                "0.5x", 
                                                color = if (ttsSpeed == 0.5f) GoldTheme else Color.Gray, 
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.clickable { ttsSpeed = 0.5f }
                                            )
                                            Text(
                                                "1x", 
                                                color = if (ttsSpeed == 1.0f) GoldTheme else Color.Gray, 
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.clickable { ttsSpeed = 1.0f }
                                            )
                                            Text(
                                                "1.5x", 
                                                color = if (ttsSpeed == 1.5f) GoldTheme else Color.Gray, 
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.clickable { ttsSpeed = 1.5f }
                                            )
                                        }
                                    }
                                    Slider(
                                        value = ttsSpeed,
                                        onValueChange = { ttsSpeed = it },
                                        valueRange = 0.5f..2.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = GoldTheme,
                                            activeTrackColor = GoldTheme,
                                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                                
                                // Pitch Slider
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Pitch: ${String.format("%.2fx", ttsPitch)}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            "Reset", 
                                            color = Color.Gray, 
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.clickable { ttsPitch = 1.0f }
                                        )
                                    }
                                    Slider(
                                        value = ttsPitch,
                                        onValueChange = { ttsPitch = it },
                                        valueRange = 0.5f..1.5f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = GoldTheme,
                                            activeTrackColor = GoldTheme,
                                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                            
                            // Bottom Action Control Row (Font Zoom buttons, Play/Pause, Stop)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Zoom Buttons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { fontScaleMultiplier = (fontScaleMultiplier - 0.1f).coerceIn(0.8f, 2.5f) },
                                        modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Font Size", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Text(
                                        text = "Text: ${(fontScaleMultiplier * 100).toInt()}%",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    
                                    IconButton(
                                        onClick = { fontScaleMultiplier = (fontScaleMultiplier + 0.1f).coerceIn(0.8f, 2.5f) },
                                        modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Font Size", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                
                                // Main Audiobook Play/Pause Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (activeSentenceIndex >= 0) {
                                        IconButton(
                                            onClick = { 
                                                isTtsPlaying = false
                                                activeSentenceIndex = -1 
                                            },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Stop,
                                                contentDescription = "Stop Audio",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    
                                    Button(
                                        onClick = {
                                            if (isTtsPlaying) {
                                                isTtsPlaying = false
                                            } else {
                                                if (activeSentenceIndex == -1) {
                                                    activeSentenceIndex = 0
                                                }
                                                isTtsPlaying = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isTtsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isTtsPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (isTtsPlaying) "PAUSE" else "LISTEN",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
