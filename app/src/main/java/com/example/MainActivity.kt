package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MPlayViewModel
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MPlayViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure aggressive caching for Coil images to reduce bandwidth and support seamless offline viewing
        try {
            val coilImageLoader = ImageLoader.Builder(this)
                .diskCache {
                    DiskCache.Builder()
                        .directory(this.cacheDir.resolve("image_cache"))
                        .maxSizeBytes(250L * 1024L * 1024L) // 250 MB disk cache
                        .build()
                }
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25) // Use up to 25% of available RAM
                        .build()
                }
                .crossfade(true)
                .build()
            coil.Coil.setImageLoader(coilImageLoader)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize Database and Repository
        val database = AppDatabase.getInstance(this)
        val repository = AppRepository(database.appDao, this)

        // Custom ViewModel Factory to support Repository Constructor Injection
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MPlayViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MPlayViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        viewModel = ViewModelProvider(this, viewModelFactory)[MPlayViewModel::class.java]

        // Handle possible Firebase Cloud Messaging (FCM) push notification cold launch
        handleNotificationIntent(intent)

        // Create notification channels on startup
        com.example.data.network.NotificationHelper.createNotificationChannels(this)

        // Trigger Play Store Install Referrer Client on App Launch (Cold Start only)
        if (savedInstanceState == null) {
            setupInstallReferrer(viewModel)
        }

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                // Main screen component container
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (::viewModel.isInitialized) {
            viewModel.setInPipMode(isInPictureInPictureMode)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (::viewModel.isInitialized && viewModel.selectedMedia.value != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    try {
                        @Suppress("DEPRECATION")
                        enterPictureInPictureMode()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                try {
                    @Suppress("DEPRECATION")
                    enterPictureInPictureMode()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        
        // Wait for class initialization, delayed safe trigger check if viewModel isn't populated instantly
        if (!::viewModel.isInitialized) {
            // Intent will be recaptured inside onCreate after viewModel becomes active
            return
        }

        val action = intent.getStringExtra("notification_action")
        val url = intent.getStringExtra("notification_url")
        val historyId = intent.getIntExtra("notification_history_id", -1)

        if (historyId != -1) {
            viewModel.updateHistoryStatus(historyId, "Opened")
        }

        if (!action.isNullOrEmpty() || !url.isNullOrEmpty()) {
            viewModel.addDebugLog("Intercepted Click Action: $action | URL: $url")
            
            when (action) {
                "in_app_url" -> {
                    if (!url.isNullOrEmpty()) {
                        viewModel.addDebugLog("In-app WebView requested for: $url")
                        // Show web text context or feedback summary
                        viewModel.setFeedbackMessage("WebView Loaded content from: $url")
                        viewModel.selectTab(com.example.ui.viewmodel.AppTab.SUPPORT)
                    }
                }
                "external_url" -> {
                    if (!url.isNullOrEmpty()) {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(browserIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            viewModel.addDebugLog("Failed to open browser URL: $e")
                        }
                    }
                }
                "youtube_url" -> {
                    val ytUrl = url ?: "https://www.youtube.com"
                    try {
                        val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl))
                        startActivity(ytIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        viewModel.addDebugLog("Failed to open YouTube: $e")
                    }
                }
                "playstore_url" -> {
                    val psUrl = url ?: "market://details?id=$packageName"
                    try {
                        val psIntent = Intent(Intent.ACTION_VIEW, Uri.parse(psUrl))
                        startActivity(psIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        viewModel.addDebugLog("Failed to open Play Store: $e")
                    }
                }
                "deep_link" -> {
                    if (!url.isNullOrEmpty()) {
                        try {
                            val uri = Uri.parse(url)
                            val path = uri.path ?: ""
                            val tab = uri.getQueryParameter("tab") ?: ""
                            val mediaId = uri.getQueryParameter("mediaId")?.toIntOrNull()
                            
                            viewModel.addDebugLog("Parsed Deep Link: Tab: $tab | Media: $mediaId")
                            
                            if (tab.equals("explore", ignoreCase = true)) {
                                viewModel.selectTab(com.example.ui.viewmodel.AppTab.EXPLORE)
                            } else if (tab.equals("reels", ignoreCase = true)) {
                                viewModel.selectTab(com.example.ui.viewmodel.AppTab.REELS)
                            } else if (tab.equals("profile", ignoreCase = true)) {
                                viewModel.selectTab(com.example.ui.viewmodel.AppTab.PROFILE)
                            } else if (tab.equals("fcm", ignoreCase = true) || tab.equals("notifications", ignoreCase = true)) {
                                viewModel.selectTab(com.example.ui.viewmodel.AppTab.PROFILE)
                            } else if (tab.equals("support", ignoreCase = true)) {
                                viewModel.selectTab(com.example.ui.viewmodel.AppTab.SUPPORT)
                            }
                            
                            if (mediaId != null) {
                                viewModel.selectMediaById(mediaId)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            viewModel.addDebugLog("Failed to parse deep link: $e")
                        }
                    }
                }
            }
        }
    }

    private fun setupInstallReferrer(viewModel: MPlayViewModel) {
        try {
            val referrerClient = com.android.installreferrer.api.InstallReferrerClient.newBuilder(this).build()
            referrerClient.startConnection(object : com.android.installreferrer.api.InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.OK -> {
                            try {
                                val response = referrerClient.installReferrer
                                val referrerUrl = response.installReferrer
                                viewModel.updateProfilePremiumFromReferrer(referrerUrl)
                                referrerClient.endConnection()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                viewModel.updateProfilePremiumFromReferrer(null)
                            }
                        }
                        else -> {
                            // On developer/uninstalled environments, fallback
                            viewModel.updateProfilePremiumFromReferrer(null)
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    // Fail gracefully
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.updateProfilePremiumFromReferrer(null)
        }
    }
}
