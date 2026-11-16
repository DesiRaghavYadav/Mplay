package com.example.data.ads

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    // Master Feature Flag: Set to false to disable all ads immediately across the entire app.
    // To re-enable ads in the next version, simply set ADS_ENABLED = true.
    const val ADS_ENABLED = false

    // Real Production AdMob Ad Unit IDs
    var bannerAdId = ""
    var interstitialAdId = "ca-app-pub-4168400468940239/2161678726"
    var rewardedAdId = "ca-app-pub-4168400468940239/6180814786"
    var rewardedInterstitialAdId = "ca-app-pub-4168400468940239/4787842062"
    var appOpenAdId = "ca-app-pub-4168400468940239/9575108199"
    var nativeAdId = ""

    // Frequency Capping Intervals for Interstitial Ads:
    // First 5 ads: 3 minutes interval (180,000 ms)
    // After 5 ads shown: 1 minute interval (60,000 ms)
    private const val INITIAL_INTERSTITIAL_CAPPING_MS = 3 * 60 * 1000L
    private const val AFTER_FIVE_INTERSTITIAL_CAPPING_MS = 1 * 60 * 1000L
    private const val CAPPING_INTERVAL_MS = 3 * 60 * 1000L

    // Impression counters for first 5 ads rule
    private var interstitialCount = 0
    private var rewardedCount = 0

    private var lastInterstitialShowTime = 0L
    private var lastRewardedShowTime = 0L
    private var lastRewardedInterstitialShowTime = 0L
    private var lastAppOpenShowTime = 0L

    // Safety flags to prevent App Open Ads right after closing an Interstitial/Rewarded ad
    private var isShowingAd = false
    private var lastAdDismissedTime = 0L

    // Preloaded Ad Instances
    private var preloadedInterstitialAd: InterstitialAd? = null
    private var preloadedRewardedAd: RewardedAd? = null
    private var preloadedRewardedInterstitialAd: RewardedInterstitialAd? = null
    private var preloadedAppOpenAd: AppOpenAd? = null

    private var isInterstitialLoading = false
    private var isRewardedLoading = false
    private var isRewardedInterstitialLoading = false
    private var isAppOpenLoading = false

    private var isInitialized = false

    fun initialize(context: Context) {
        if (!ADS_ENABLED) {
            Log.d(TAG, "Ads are disabled (ADS_ENABLED = false). Skipping MobileAds initialization.")
            return
        }
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "AdMob SDK Initialized successfully: $initializationStatus")
                isInitialized = true
                preloadAllAds(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds: ${e.message}")
        }
    }

    private val preloadedNativeAdsPool = java.util.concurrent.ConcurrentLinkedQueue<NativeAd>()
    private const val MAX_PRELOADED_NATIVE_ADS = 2
    private val nativeAdCache = java.util.concurrent.ConcurrentHashMap<String, NativeAd>()
    private var lastNativeAdRequestTime = 0L
    private const val MIN_NATIVE_AD_INTERVAL_MS = 5000L
    @Volatile private var isPreloadingNative = false

    fun canRequestNativeAd(): Boolean = false

    fun getCachedNativeAd(key: String): NativeAd? = null

    fun putCachedNativeAd(key: String, ad: NativeAd) {
        // Disabled: Native ads removed
    }

    fun clearNativeAdCache() {
        nativeAdCache.values.forEach {
            try {
                it.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying native ad: ${e.message}")
            }
        }
        nativeAdCache.clear()
        preloadedNativeAdsPool.forEach {
            try {
                it.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying preloaded native ad: ${e.message}")
            }
        }
        preloadedNativeAdsPool.clear()
    }

    private fun preloadAllAds(context: Context) {
        if (!ADS_ENABLED) return
        preloadInterstitial(context)
        preloadRewarded(context)
        preloadRewardedInterstitial(context)
        preloadAppOpen(context)
    }

    fun preloadNativeAd(context: Context) {
        // Disabled: Native ads completely removed
    }

    fun consumePreloadedNativeAd(): NativeAd? = null

    // ------------------------------------------------------------------------
    // PRELOADING LOGIC
    // ------------------------------------------------------------------------

    fun preloadInterstitial(context: Context) {
        if (!ADS_ENABLED) return
        if (preloadedInterstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            interstitialAdId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    preloadedInterstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial Ad Preloaded & Ready")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    preloadedInterstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Interstitial Ad Failed to Preload: ${error.message}")
                }
            }
        )
    }

    fun preloadRewarded(context: Context) {
        if (!ADS_ENABLED) return
        if (preloadedRewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            rewardedAdId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    preloadedRewardedAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "Rewarded Video Ad Preloaded & Ready")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    preloadedRewardedAd = null
                    isRewardedLoading = false
                    Log.w(TAG, "Rewarded Video Ad Failed to Preload: ${error.message}")
                }
            }
        )
    }

    fun preloadRewardedInterstitial(context: Context) {
        if (!ADS_ENABLED) return
        if (preloadedRewardedInterstitialAd != null || isRewardedInterstitialLoading) return
        isRewardedInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            context,
            rewardedInterstitialAdId,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    preloadedRewardedInterstitialAd = ad
                    isRewardedInterstitialLoading = false
                    Log.d(TAG, "Rewarded Interstitial Ad Preloaded & Ready")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    preloadedRewardedInterstitialAd = null
                    isRewardedInterstitialLoading = false
                    Log.w(TAG, "Rewarded Interstitial Ad Failed to Preload: ${error.message}")
                }
            }
        )
    }

    fun preloadAppOpen(context: Context) {
        if (!ADS_ENABLED) return
        if (preloadedAppOpenAd != null || isAppOpenLoading) return
        isAppOpenLoading = true
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            appOpenAdId,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    preloadedAppOpenAd = ad
                    isAppOpenLoading = false
                    Log.d(TAG, "App Open Ad Preloaded & Ready")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    preloadedAppOpenAd = null
                    isAppOpenLoading = false
                    Log.w(TAG, "App Open Ad Failed to Preload: ${error.message}")
                }
            }
        )
    }

    // ------------------------------------------------------------------------
    // DISPLAY & FREQUENCY CAPPING METHODS
    // ------------------------------------------------------------------------

    /**
     * Attempts to show an Interstitial Ad.
     * First 5 ads: enforces 3 minutes (180s) frequency cap between ads.
     * After 5 ads shown: reduces frequency cap interval to 1 minute (60s) between ads.
     */
    fun tryShowInterstitial(
        activity: Activity,
        ignoreFrequencyCap: Boolean = false,
        onComplete: () -> Unit = {}
    ) {
        if (!ADS_ENABLED) {
            onComplete()
            return
        }
        val now = System.currentTimeMillis()
        val currentCapInterval = if (interstitialCount < 5) INITIAL_INTERSTITIAL_CAPPING_MS else AFTER_FIVE_INTERSTITIAL_CAPPING_MS
        val timeSinceLast = now - lastInterstitialShowTime
        val isCapped = timeSinceLast < currentCapInterval

        if (!ignoreFrequencyCap && isCapped) {
            val remainingSec = (currentCapInterval - timeSinceLast) / 1000
            val capText = if (interstitialCount < 5) "3 min cap" else "1 min cap"
            Log.d(TAG, "Interstitial capped ($interstitialCount shown, $remainingSec s remaining under $capText). Skipping.")
            onComplete()
            return
        }

        val ad = preloadedInterstitialAd
        if (ad == null) {
            Log.d(TAG, "Interstitial ad not ready. Skipping.")
            preloadInterstitial(activity)
            onComplete()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "Interstitial showed successfully.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed.")
                preloadedInterstitialAd = null
                isShowingAd = false
                lastAdDismissedTime = System.currentTimeMillis()
                lastInterstitialShowTime = System.currentTimeMillis()
                interstitialCount++
                preloadInterstitial(activity) // Preload next immediately
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                preloadedInterstitialAd = null
                isShowingAd = false
                lastAdDismissedTime = System.currentTimeMillis()
                preloadInterstitial(activity)
                onComplete()
            }
        }

        preloadedInterstitialAd = null
        isShowingAd = true
        ad.show(activity)
    }

    private var adLoadingDialog: Dialog? = null

    fun showAdLoadingDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread {
            try {
                dismissAdLoadingDialog()
                val density = activity.resources.displayMetrics.density

                val container = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (20 * density).toInt())
                    gravity = Gravity.CENTER_VERTICAL
                    val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        setColor(Color.parseColor("#1C1C26"))
                        cornerRadius = 16f * density
                        setStroke((1 * density).toInt(), Color.parseColor("#2A2A38"))
                    }
                    background = bgDrawable

                    val progressBar = ProgressBar(activity).apply {
                        isIndeterminate = true
                        indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E5A93C"))
                    }

                    val textLayout = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding((16 * density).toInt(), 0, 0, 0)

                        val titleView = TextView(activity).apply {
                            text = "Loading Ad to Unlock Reward"
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            setTypeface(null, Typeface.BOLD)
                            setTextColor(Color.WHITE)
                        }

                        val subtitleView = TextView(activity).apply {
                            text = "Please wait a moment..."
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                            setTextColor(Color.parseColor("#A0A0B0"))
                        }

                        addView(titleView)
                        addView(subtitleView)
                    }

                    addView(progressBar, LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt()))
                    addView(textLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                }

                adLoadingDialog = Dialog(activity).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(container)
                    setCancelable(false)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing ad loading dialog: ${e.message}")
            }
        }
    }

    fun dismissAdLoadingDialog() {
        try {
            adLoadingDialog?.let {
                if (it.isShowing) {
                    it.dismiss()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing ad loading dialog: ${e.message}")
        } finally {
            adLoadingDialog = null
        }
    }

    private var showRewardedInterstitialNext = false

    /**
     * Attempts to show a Rewarded Video Ad or Rewarded Interstitial alternately (1 Video, 1 Interstitial Rewarded).
     * Pops up a loading dialog if preloaded ad is not ready, then shows the ad or grants reward.
     */
    fun tryShowRewardedOrRewardedInterstitial(
        activity: Activity,
        onUserEarnedReward: () -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        if (!ADS_ENABLED) {
            onUserEarnedReward()
            onComplete()
            return
        }
        val now = System.currentTimeMillis()
        val isCapped = (rewardedCount >= 5) && (now - lastRewardedShowTime < CAPPING_INTERVAL_MS)

        if (isCapped) {
            Log.d(TAG, "Rewarded ad capped ($rewardedCount shown, < 3 mins since last). Skipping.")
            dismissAdLoadingDialog()
            onComplete()
            return
        }

        // 1. Check preloaded ads first
        if (preloadedRewardedInterstitialAd != null) {
            dismissAdLoadingDialog()
            showRewardedInterstitialNext = false
            showRewardedInterstitialInternal(activity, onUserEarnedReward, onComplete)
            return
        }

        if (preloadedRewardedAd != null) {
            dismissAdLoadingDialog()
            showRewardedInterstitialNext = true
            showRewardedVideoInternal(activity, onUserEarnedReward, onComplete)
            return
        }

        // 2. Not pre-cached: Show loading dialog popup to unlock reward
        showAdLoadingDialog(activity)

        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var isHandled = false

        val timeoutRunnable = Runnable {
            if (!isHandled) {
                isHandled = true
                Log.w(TAG, "On-demand ad loading timed out after 8s.")
                dismissAdLoadingDialog()
                onUserEarnedReward()
                onComplete()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 8000L)

        val wantInterstitialFirst = showRewardedInterstitialNext
        val adRequest = AdRequest.Builder().build()

        if (wantInterstitialFirst) {
            Log.d(TAG, "Loading Rewarded Interstitial Ad on demand with popup...")
            RewardedInterstitialAd.load(
                activity,
                rewardedInterstitialAdId,
                adRequest,
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        if (isHandled) return
                        isHandled = true
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        dismissAdLoadingDialog()
                        preloadedRewardedInterstitialAd = ad
                        showRewardedInterstitialNext = false
                        showRewardedInterstitialInternal(activity, onUserEarnedReward, onComplete)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "On-demand Rewarded Interstitial failed: ${error.message}. Trying Rewarded Video...")
                        RewardedAd.load(
                            activity,
                            rewardedAdId,
                            adRequest,
                            object : RewardedAdLoadCallback() {
                                override fun onAdLoaded(ad: RewardedAd) {
                                    if (isHandled) return
                                    isHandled = true
                                    timeoutHandler.removeCallbacks(timeoutRunnable)
                                    dismissAdLoadingDialog()
                                    preloadedRewardedAd = ad
                                    showRewardedInterstitialNext = true
                                    showRewardedVideoInternal(activity, onUserEarnedReward, onComplete)
                                }

                                override fun onAdFailedToLoad(err: LoadAdError) {
                                    if (isHandled) return
                                    isHandled = true
                                    timeoutHandler.removeCallbacks(timeoutRunnable)
                                    Log.w(TAG, "On-demand Rewarded Video also failed: ${err.message}")
                                    dismissAdLoadingDialog()
                                    onUserEarnedReward()
                                    onComplete()
                                }
                            }
                        )
                    }
                }
            )
        } else {
            Log.d(TAG, "Loading Rewarded Video Ad on demand with popup...")
            RewardedAd.load(
                activity,
                rewardedAdId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (isHandled) return
                        isHandled = true
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        dismissAdLoadingDialog()
                        preloadedRewardedAd = ad
                        showRewardedInterstitialNext = true
                        showRewardedVideoInternal(activity, onUserEarnedReward, onComplete)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "On-demand Rewarded Video failed: ${error.message}. Trying Rewarded Interstitial...")
                        RewardedInterstitialAd.load(
                            activity,
                            rewardedInterstitialAdId,
                            adRequest,
                            object : RewardedInterstitialAdLoadCallback() {
                                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                                    if (isHandled) return
                                    isHandled = true
                                    timeoutHandler.removeCallbacks(timeoutRunnable)
                                    dismissAdLoadingDialog()
                                    preloadedRewardedInterstitialAd = ad
                                    showRewardedInterstitialNext = false
                                    showRewardedInterstitialInternal(activity, onUserEarnedReward, onComplete)
                                }

                                override fun onAdFailedToLoad(err: LoadAdError) {
                                    if (isHandled) return
                                    isHandled = true
                                    timeoutHandler.removeCallbacks(timeoutRunnable)
                                    Log.w(TAG, "On-demand Rewarded Interstitial also failed: ${err.message}")
                                    dismissAdLoadingDialog()
                                    onUserEarnedReward()
                                    onComplete()
                                }
                            }
                        )
                    }
                }
            )
        }
    }

    private fun showRewardedVideoInternal(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onComplete: () -> Unit
    ) {
        dismissAdLoadingDialog()
        val ad = preloadedRewardedAd
        var rewardGranted = false

        ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "Rewarded Video showed successfully.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded Video dismissed.")
                preloadedRewardedAd = null
                isShowingAd = false
                lastAdDismissedTime = System.currentTimeMillis()
                lastRewardedShowTime = System.currentTimeMillis()
                rewardedCount++
                preloadRewarded(activity)
                if (rewardGranted) {
                    onUserEarnedReward()
                } else {
                    android.widget.Toast.makeText(activity, "Unlocking content...", android.widget.Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded Video failed to show: ${error.message}")
                preloadedRewardedAd = null
                isShowingAd = false
                lastAdDismissedTime = System.currentTimeMillis()
                preloadRewarded(activity)
                onUserEarnedReward()
                onComplete()
            }
        }

        preloadedRewardedAd = null
        isShowingAd = true
        ad?.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.type} (${rewardItem.amount})")
            rewardGranted = true
        }
    }

    private fun showRewardedInterstitialInternal(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onComplete: () -> Unit
    ) {
        dismissAdLoadingDialog()
        val ad = preloadedRewardedInterstitialAd
        var rewardGranted = false

        ad?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "Rewarded Interstitial showed successfully.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded Interstitial dismissed.")
                preloadedRewardedInterstitialAd = null
                isShowingAd = false
                lastAdDismissedTime = System.currentTimeMillis()
                lastRewardedShowTime = System.currentTimeMillis()
                rewardedCount++
                preloadRewardedInterstitial(activity)
                if (rewardGranted) {
                    onUserEarnedReward()
                } else {
                    android.widget.Toast.makeText(activity, "Unlocking content...", android.widget.Toast.LENGTH_SHORT).show()
                }
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded Interstitial failed to show: ${error.message}")
                preloadedRewardedInterstitialAd = null
                isShowingAd = false
                lastAdDismissedTime = System.currentTimeMillis()
                preloadRewardedInterstitial(activity)
                onUserEarnedReward()
                onComplete()
            }
        }

        preloadedRewardedInterstitialAd = null
        isShowingAd = true
        ad?.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.type} (${rewardItem.amount})")
            rewardGranted = true
        }
    }

    /**
     * Attempts to show an App Open Ad when the app opens or returns to foreground.
     * Will NOT show if an ad is currently open or was dismissed within the last 10 seconds.
     */
    fun tryShowAppOpenAd(activity: Activity) {
        if (!ADS_ENABLED) return
        val now = System.currentTimeMillis()

        if (isShowingAd) {
            Log.d(TAG, "An ad is currently showing. Skipping App Open Ad.")
            return
        }

        if (now - lastAdDismissedTime < 10000L) {
            Log.d(TAG, "An ad was recently closed (< 10 seconds ago). Skipping App Open Ad.")
            return
        }

        if (now - lastAppOpenShowTime < CAPPING_INTERVAL_MS) {
            Log.d(TAG, "App Open Ad capped. Skipping.")
            return
        }

        val ad = preloadedAppOpenAd
        if (ad == null) {
            preloadAppOpen(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "App Open Ad showed successfully.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open Ad dismissed.")
                preloadedAppOpenAd = null
                isShowingAd = false
                lastAdDismissedTime = System.currentTimeMillis()
                lastAppOpenShowTime = System.currentTimeMillis()
                preloadAppOpen(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "App Open Ad failed to show: ${error.message}")
                preloadedAppOpenAd = null
                isShowingAd = false
                lastAdDismissedTime = System.currentTimeMillis()
                preloadAppOpen(activity)
            }
        }

        preloadedAppOpenAd = null
        isShowingAd = true
        ad.show(activity)
    }
}

/**
 * Composable Banner Ad component - completely removed.
 */
@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier,
    adSize: AdSize? = null
) {
    // Banner ads completely removed
}

/**
 * Native Ad Skeleton Loader - completely removed.
 */
@Composable
fun NativeAdSkeletonLoader(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    // Native ads completely removed
}

/**
 * Native Ad Component - completely removed.
 */
@Composable
fun AdMobNativeAdView(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    adKey: String? = null
) {
    // Native ads completely removed
}
