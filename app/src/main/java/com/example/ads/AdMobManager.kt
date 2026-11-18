package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.TextMuted
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

object AdUnitIds {
    const val BANNER = "ca-app-pub-5984576938417142/2994020841"
    const val INTERSTITIAL = "ca-app-pub-5984576938417142/3117455560"
    const val REWARDED = "ca-app-pub-5984576938417142/5620184189"
    const val APP_OPEN = "ca-app-pub-5984576938417142/2587838493"
}

class AdMobManager(private val appContext: Context) {

    companion object {
        private const val TAG = "AdMobManager"
        private const val APP_OPEN_COOLDOWN_MS = 30_000L
        private const val INTERSTITIAL_COOLDOWN_MS = 45_000L
        private const val FOUR_HOURS_MS = 4L * 3600L * 1000L
    }

    private var isInitialized = false
    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoadTimeMs: Long = 0L
    private var isLoadingAppOpen = false
    private var lastAppOpenShownMs: Long = 0L
    private var hasShownColdStartAppOpen = false
    private val coldStartTimestampMs = System.currentTimeMillis()

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var lastInterstitialShownMs: Long = 0L

    private var rewardedAd: RewardedAd? = null

    private val _rewardedAdReady = MutableStateFlow(false)
    val rewardedAdReady: StateFlow<Boolean> = _rewardedAdReady.asStateFlow()

    private val _rewardedAdLoading = MutableStateFlow(false)
    val rewardedAdLoading: StateFlow<Boolean> = _rewardedAdLoading.asStateFlow()

    private val _rewardedAdError = MutableStateFlow<String?>(null)
    val rewardedAdError: StateFlow<String?> = _rewardedAdError.asStateFlow()

    @Volatile
    var isShowingFullScreenAd: Boolean = false
        private set

    fun initialize(currentActivityProvider: () -> Activity? = { null }) {
        if (isInitialized || isRobolectricEnvironment()) return
        isInitialized = true
        runCatching {
            MobileAds.initialize(appContext) {
                loadAppOpenAd(currentActivityProvider)
                loadInterstitialAd()
                loadRewardedAd()
            }
        }.onFailure { e ->
            Log.e(TAG, "MobileAds init failed", e)
        }
    }

    // region App Open Ad
    private fun isAppOpenAvailable(): Boolean =
        appOpenAd != null && (Date().time - appOpenLoadTimeMs) < FOUR_HOURS_MS

    fun loadAppOpenAd(currentActivityProvider: () -> Activity? = { null }) {
        if (isRobolectricEnvironment() || isLoadingAppOpen || isAppOpenAvailable()) return
        isLoadingAppOpen = true
        AppOpenAd.load(
            appContext,
            AdUnitIds.APP_OPEN,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    appOpenLoadTimeMs = Date().time
                    isLoadingAppOpen = false
                    Log.d(TAG, "AppOpenAd loaded")
                    // Show once on cold start if loaded quickly while MainActivity is active
                    if (!hasShownColdStartAppOpen &&
                        System.currentTimeMillis() - coldStartTimestampMs <= 6000L
                    ) {
                        currentActivityProvider()?.let { act ->
                            showAppOpenAdIfAvailable(act)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAppOpen = false
                    Log.w(TAG, "AppOpenAd failed: ${error.code} - ${error.message}")
                }
            }
        )
    }

    fun showAppOpenAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        if (isShowingFullScreenAd || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastAppOpenShownMs < APP_OPEN_COOLDOWN_MS) {
            onComplete()
            return
        }
        val ad = appOpenAd
        if (ad == null || !isAppOpenAvailable()) {
            appOpenAd = null
            loadAppOpenAd()
            onComplete()
            return
        }

        hasShownColdStartAppOpen = true
        isShowingFullScreenAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingFullScreenAd = false
                lastAppOpenShownMs = System.currentTimeMillis()
                loadAppOpenAd()
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingFullScreenAd = false
                loadAppOpenAd()
                onComplete()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingFullScreenAd = true
            }
        }
        ad.show(activity)
    }
    // endregion

    // region Interstitial Ad
    fun loadInterstitialAd() {
        if (isRobolectricEnvironment() || isLoadingInterstitial || interstitialAd != null) return
        isLoadingInterstitial = true
        InterstitialAd.load(
            appContext,
            AdUnitIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "InterstitialAd loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.w(TAG, "InterstitialAd failed: ${error.code} - ${error.message}")
                }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity, onComplete: () -> Unit = {}) {
        if (isShowingFullScreenAd || activity.isFinishing || activity.isDestroyed) {
            onComplete()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownMs < INTERSTITIAL_COOLDOWN_MS) {
            onComplete()
            return
        }
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitialAd()
            onComplete()
            return
        }

        isShowingFullScreenAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isShowingFullScreenAd = false
                lastInterstitialShownMs = System.currentTimeMillis()
                lastAppOpenShownMs = System.currentTimeMillis()
                loadInterstitialAd()
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                isShowingFullScreenAd = false
                loadInterstitialAd()
                onComplete()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingFullScreenAd = true
            }
        }
        ad.show(activity)
    }

    fun delayNextInterstitial() {
        lastInterstitialShownMs = System.currentTimeMillis()
    }
    // endregion

    // region Rewarded Ad
    fun loadRewardedAd(onLoadedCallback: (() -> Unit)? = null) {
        if (isRobolectricEnvironment()) return
        if (rewardedAd != null) {
            _rewardedAdReady.value = true
            _rewardedAdLoading.value = false
            _rewardedAdError.value = null
            onLoadedCallback?.invoke()
            return
        }
        if (_rewardedAdLoading.value) return

        _rewardedAdLoading.value = true
        _rewardedAdError.value = null

        RewardedAd.load(
            appContext,
            AdUnitIds.REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _rewardedAdReady.value = true
                    _rewardedAdLoading.value = false
                    _rewardedAdError.value = null
                    Log.d(TAG, "RewardedAd loaded")
                    onLoadedCallback?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _rewardedAdReady.value = false
                    _rewardedAdLoading.value = false
                    _rewardedAdError.value = "${error.message} (code ${error.code})"
                    Log.w(TAG, "RewardedAd failed: ${error.code} - ${error.message}")
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onClosedWithoutReward: () -> Unit,
        onNotReady: () -> Unit
    ) {
        if (isShowingFullScreenAd || activity.isFinishing || activity.isDestroyed) {
            onNotReady()
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            _rewardedAdReady.value = false
            loadRewardedAd()
            onNotReady()
            return
        }

        var earnedReward = false
        isShowingFullScreenAd = true

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _rewardedAdReady.value = false
                isShowingFullScreenAd = false
                lastAppOpenShownMs = System.currentTimeMillis()
                lastInterstitialShownMs = System.currentTimeMillis()
                loadRewardedAd()
                if (earnedReward) {
                    onRewardEarned()
                } else {
                    onClosedWithoutReward()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _rewardedAdReady.value = false
                isShowingFullScreenAd = false
                _rewardedAdError.value = adError.message
                loadRewardedAd()
                onClosedWithoutReward()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingFullScreenAd = true
            }
        }

        ad.show(activity) {
            earnedReward = true
        }
    }
    // endregion
}

fun isRobolectricEnvironment(): Boolean =
    Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

@Composable
fun AdMobBannerBar(
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current || isRobolectricEnvironment()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MidnightSurface)
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("AdMob Banner", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        return
    }

    val context = LocalContext.current

    BoxWithConstraints(
        modifier = modifier
            .testTag("admob_banner_container")
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val widthDp = maxWidth.value.toInt().coerceIn(320, 728)
        val adSize = remember(widthDp, context) {
            runCatching {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            }.getOrDefault(AdSize.BANNER)
        }

        val adView = remember(adSize) {
            AdView(context).apply {
                adUnitId = AdUnitIds.BANNER
                setAdSize(adSize)
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("AdMobBannerBar", "Banner failed: ${error.code} - ${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }

        DisposableEffect(adView) {
            onDispose {
                runCatching { adView.destroy() }
            }
        }

        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
