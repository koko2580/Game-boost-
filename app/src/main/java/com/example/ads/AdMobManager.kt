package com.example.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.webkit.WebView
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.TurboGameApplication
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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URL
import java.util.Date
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

object AdUnitIds {
    const val BANNER = "ca-app-pub-5984576938417142/2994020841"
    const val INTERSTITIAL = "ca-app-pub-5984576938417142/3117455560"
    const val REWARDED = "ca-app-pub-5984576938417142/5620184189"
    const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val APP_OPEN = "ca-app-pub-5984576938417142/2587838493"

    fun effectiveRewarded(isDebug: Boolean = BuildConfig.DEBUG): String =
        if (isDebug) TEST_REWARDED else REWARDED
}

enum class AdEndpointProbeResult {
    HEALTHY,
    SSL_BLOCKED,
    UNREACHABLE
}

object AdLoadPolicy {
    private val THROTTLE_BACKOFF_MS = longArrayOf(60_000L, 90_000L, 120_000L)
    private val STANDARD_BACKOFF_MS = longArrayOf(15_000L, 30_000L, 60_000L, 120_000L)
    const val SSL_CIRCUIT_BREAKER_MS = 300_000L

    fun isSslOrWebViewError(errorMessage: String?): Boolean {
        val lower = errorMessage?.lowercase().orEmpty()
        return lower.contains("ssl") ||
            lower.contains("handshake") ||
            lower.contains("cert") ||
            lower.contains("trust anchor") ||
            lower.contains("javascriptengine") ||
            lower.contains("webview") ||
            lower.contains("err_cert")
    }

    fun isThrottleError(errorCode: Int, errorMessage: String?): Boolean {
        val lower = errorMessage?.lowercase().orEmpty()
        return errorCode == AdRequest.ERROR_CODE_INVALID_REQUEST ||
            lower.contains("too many") ||
            lower.contains("wait a few seconds") ||
            lower.contains("recently failed")
    }

    fun computeBackoffMs(
        errorCode: Int,
        errorMessage: String?,
        consecutiveFailures: Int
    ): Long {
        if (isSslOrWebViewError(errorMessage)) {
            return SSL_CIRCUIT_BREAKER_MS
        }
        val index = (consecutiveFailures - 1).coerceAtLeast(0)
        return if (isThrottleError(errorCode, errorMessage)) {
            THROTTLE_BACKOFF_MS[index.coerceAtMost(THROTTLE_BACKOFF_MS.lastIndex)]
        } else {
            STANDARD_BACKOFF_MS[index.coerceAtMost(STANDARD_BACKOFF_MS.lastIndex)]
        }
    }

    fun sanitizeUserErrorMessage(errorCode: Int, errorMessage: String?): String {
        return when {
            isSslOrWebViewError(errorMessage) ->
                "Ad server TLS blocked by network or DNS. Switch network or disable ad blocker."
            isThrottleError(errorCode, errorMessage) ->
                "Ad server is warming up. Auto-retrying shortly..."
            errorCode == AdRequest.ERROR_CODE_NO_FILL ->
                "Ad inventory is busy right now. Retrying automatically..."
            errorCode == AdRequest.ERROR_CODE_NETWORK_ERROR ->
                "Connection unstable while loading ad. Retrying shortly..."
            else ->
                "Ad is temporarily unavailable. Please wait a moment."
        }
    }
}

class AdMobManager(private val appContext: Context) {

    companion object {
        private const val TAG = "AdMobManager"
        private const val APP_OPEN_COOLDOWN_MS = 600_000L // 10 minutes between App Open ads
        private const val INTERSTITIAL_COOLDOWN_MS = 240_000L // 4 minutes between Interstitials
        private const val FOUR_HOURS_MS = 4L * 3600L * 1000L
        private const val MAX_AUTO_RETRIES = 3
        private const val TLS_PROBE_TIMEOUT_MS = 2500
        private const val UNREACHABLE_COOLDOWN_MS = 30_000L
        private val AD_PROBE_URLS = listOf(
            "https://googleads.g.doubleclick.net/favicon.ico",
            "https://pagead2.googlesyndication.com/favicon.ico"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isInitializing = false

    @Volatile
    private var tlsBlockedUntilElapsedMs = 0L

    private val _isSdkInitialized = MutableStateFlow(false)
    val isSdkInitialized: StateFlow<Boolean> = _isSdkInitialized.asStateFlow()

    private val _adHostHealthy = MutableStateFlow(false)
    val adHostHealthy: StateFlow<Boolean> = _adHostHealthy.asStateFlow()

    private var pendingAppOpenLoad = false
    private var pendingInterstitialLoad = false
    private var pendingRewardedLoad = false

    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoadTimeMs: Long = 0L
    private var isLoadingAppOpen = false
    private var appOpenFailures = 0
    private var appOpenNextAllowedElapsedMs = 0L
    private var appOpenRetryRunnable: Runnable? = null
    private var lastAppOpenShownMs: Long = 0L
    private var suppressAppOpenUntilMs: Long = 0L

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var interstitialFailures = 0
    private var interstitialNextAllowedElapsedMs = 0L
    private var interstitialRetryRunnable: Runnable? = null
    private var lastInterstitialShownMs: Long = 0L

    private var rewardedAd: RewardedAd? = null
    private var rewardedFailures = 0
    private var rewardedNextAllowedElapsedMs = 0L
    private var rewardedRetryRunnable: Runnable? = null
    private var rewardedCountdownRunnable: Runnable? = null

    private var bannerFailures = 0
    private var bannerNextAllowedElapsedMs = 0L

    private val _rewardedAdReady = MutableStateFlow(false)
    val rewardedAdReady: StateFlow<Boolean> = _rewardedAdReady.asStateFlow()

    private val _rewardedAdLoading = MutableStateFlow(false)
    val rewardedAdLoading: StateFlow<Boolean> = _rewardedAdLoading.asStateFlow()

    private val _rewardedAdError = MutableStateFlow<String?>(null)
    val rewardedAdError: StateFlow<String?> = _rewardedAdError.asStateFlow()

    private val _rewardedCooldownSeconds = MutableStateFlow(0)
    val rewardedCooldownSeconds: StateFlow<Int> = _rewardedCooldownSeconds.asStateFlow()

    @Volatile
    var isShowingFullScreenAd: Boolean = false
        private set

    fun initialize() {
        if (isInitialized || isInitializing || isRobolectricEnvironment()) return
        val now = SystemClock.elapsedRealtime()
        if (now < tlsBlockedUntilElapsedMs) return
        if (!isNetworkAvailable()) return

        isInitializing = true
        Thread {
            try {
                if (!hasUsableWebViewAndPlayServices()) {
                    _adHostHealthy.value = false
                    tlsBlockedUntilElapsedMs = SystemClock.elapsedRealtime() + AdLoadPolicy.SSL_CIRCUIT_BREAKER_MS
                    mainHandler.post {
                        _rewardedAdLoading.value = false
                        _rewardedAdError.value = "Ad services unavailable on this device."
                    }
                    return@Thread
                }

                when (probeAdEndpointsTls()) {
                    AdEndpointProbeResult.HEALTHY -> {
                        _adHostHealthy.value = true
                        tlsBlockedUntilElapsedMs = 0L
                    }
                    AdEndpointProbeResult.SSL_BLOCKED -> {
                        _adHostHealthy.value = false
                        tlsBlockedUntilElapsedMs = SystemClock.elapsedRealtime() + AdLoadPolicy.SSL_CIRCUIT_BREAKER_MS
                        mainHandler.post {
                            _rewardedAdLoading.value = false
                            _rewardedAdError.value =
                                "Ad server TLS blocked by network or DNS. Switch network or disable ad blocker."
                        }
                        if (BuildConfig.DEBUG) {
                            Log.w(TAG, "Ad host TLS preflight failed; skipping WebView/MobileAds initialization")
                        }
                        return@Thread
                    }
                    AdEndpointProbeResult.UNREACHABLE -> {
                        _adHostHealthy.value = false
                        tlsBlockedUntilElapsedMs = SystemClock.elapsedRealtime() + UNREACHABLE_COOLDOWN_MS
                        mainHandler.post {
                            _rewardedAdLoading.value = false
                            _rewardedAdError.value = "Ad servers unreachable right now. Retrying when connected."
                        }
                        return@Thread
                    }
                }

                MobileAds.initialize(appContext) {
                    isInitialized = true
                    mainHandler.post {
                        _isSdkInitialized.value = true
                        val rewardedImmediate = pendingRewardedLoad
                        pendingRewardedLoad = false
                        if (rewardedImmediate) {
                            loadRewardedAd()
                        } else {
                            mainHandler.postDelayed({ loadRewardedAd() }, 1_200L)
                        }

                        pendingInterstitialLoad = false
                        mainHandler.postDelayed({ loadInterstitialAd() }, 2_800L)

                        pendingAppOpenLoad = false
                        mainHandler.postDelayed({ loadAppOpenAd() }, 4_400L)
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "MobileAds init failed", t)
                mainHandler.post { _rewardedAdLoading.value = false }
            } finally {
                isInitializing = false
            }
        }.start()
    }

    fun onNetworkConnected() {
        if (isRobolectricEnvironment()) return
        tlsBlockedUntilElapsedMs = 0L
        bannerFailures = 0
        bannerNextAllowedElapsedMs = 0L
        if (!isInitialized) {
            initialize()
        } else {
            mainHandler.post {
                if (!_adHostHealthy.value) {
                    Thread {
                        if (probeAdEndpointsTls() == AdEndpointProbeResult.HEALTHY) {
                            _adHostHealthy.value = true
                            mainHandler.post {
                                loadRewardedAd()
                                mainHandler.postDelayed({ loadInterstitialAd() }, 1_500L)
                            }
                        }
                    }.start()
                } else {
                    loadRewardedAd()
                    mainHandler.postDelayed({ loadInterstitialAd() }, 1_500L)
                }
            }
        }
    }

    private fun hasUsableWebViewAndPlayServices(): Boolean {
        val hasWebView = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WebView.getCurrentWebViewPackage() != null
            } else {
                true
            }
        }.getOrDefault(false)
        if (!hasWebView) return false

        val gmsStatus = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext)
        }.getOrDefault(ConnectionResult.SUCCESS)

        return gmsStatus != ConnectionResult.SERVICE_MISSING &&
            gmsStatus != ConnectionResult.SERVICE_INVALID
    }

    private fun probeAdEndpointsTls(): AdEndpointProbeResult {
        var sawSslError = false
        for (urlString in AD_PROBE_URLS) {
            var conn: HttpsURLConnection? = null
            try {
                conn = (URL(urlString).openConnection() as HttpsURLConnection).apply {
                    connectTimeout = TLS_PROBE_TIMEOUT_MS
                    readTimeout = TLS_PROBE_TIMEOUT_MS
                    instanceFollowRedirects = false
                    useCaches = false
                    requestMethod = "HEAD"
                }
                conn.connect()
                conn.responseCode
                return AdEndpointProbeResult.HEALTHY
            } catch (_: SSLPeerUnverifiedException) {
                sawSslError = true
            } catch (_: SSLHandshakeException) {
                sawSslError = true
            } catch (_: Throwable) {
                // Continue probing secondary host
            } finally {
                runCatching { conn?.disconnect() }
            }
        }
        return if (sawSslError) {
            AdEndpointProbeResult.SSL_BLOCKED
        } else {
            AdEndpointProbeResult.UNREACHABLE
        }
    }

    private fun markSslOrWebViewCircuitBreaker(errorMessage: String?) {
        _adHostHealthy.value = false
        tlsBlockedUntilElapsedMs = SystemClock.elapsedRealtime() + AdLoadPolicy.SSL_CIRCUIT_BREAKER_MS
        bannerNextAllowedElapsedMs = tlsBlockedUntilElapsedMs
        rewardedNextAllowedElapsedMs = tlsBlockedUntilElapsedMs
        interstitialNextAllowedElapsedMs = tlsBlockedUntilElapsedMs
        appOpenNextAllowedElapsedMs = tlsBlockedUntilElapsedMs
        _rewardedAdError.value = AdLoadPolicy.sanitizeUserErrorMessage(
            AdRequest.ERROR_CODE_NETWORK_ERROR,
            errorMessage
        )
    }

    fun suppressAppOpenTemporarily(durationMs: Long = 90_000L) {
        suppressAppOpenUntilMs = System.currentTimeMillis() + durationMs
    }

    fun isNetworkAvailable(): Boolean {
        if (isRobolectricEnvironment()) return true
        return runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return@runCatching true
            val network = cm.activeNetwork ?: return@runCatching false
            val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.getOrDefault(true)
    }

    // region Banner Throttle Helpers
    fun canAttemptBannerLoad(): Boolean {
        if (isRobolectricEnvironment()) return false
        if (!_isSdkInitialized.value || !_adHostHealthy.value) return false
        if (!isNetworkAvailable()) return false
        val now = SystemClock.elapsedRealtime()
        return now >= tlsBlockedUntilElapsedMs && now >= bannerNextAllowedElapsedMs
    }

    fun recordBannerSuccess() {
        bannerFailures = 0
        bannerNextAllowedElapsedMs = 0L
    }

    fun recordBannerFailure(error: LoadAdError) {
        bannerFailures++
        if (AdLoadPolicy.isSslOrWebViewError(error.message)) {
            markSslOrWebViewCircuitBreaker(error.message)
        }
        val backoff = AdLoadPolicy.computeBackoffMs(error.code, error.message, bannerFailures)
        bannerNextAllowedElapsedMs = SystemClock.elapsedRealtime() + backoff
        if (BuildConfig.DEBUG) {
            Log.w("AdMobBannerBar", "Banner paused ${backoff / 1000}s after error code ${error.code}")
        }
    }
    // endregion

    // region App Open Ad
    private fun isAppOpenAvailable(): Boolean =
        appOpenAd != null && (Date().time - appOpenLoadTimeMs) < FOUR_HOURS_MS

    fun loadAppOpenAd() {
        if (isRobolectricEnvironment() || isLoadingAppOpen || isAppOpenAvailable()) return
        if (!_isSdkInitialized.value) {
            pendingAppOpenLoad = true
            initialize()
            return
        }
        if (!_adHostHealthy.value || !isNetworkAvailable()) return
        val now = SystemClock.elapsedRealtime()
        if (now < tlsBlockedUntilElapsedMs || now < appOpenNextAllowedElapsedMs) return

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
                    appOpenFailures = 0
                    appOpenNextAllowedElapsedMs = 0L
                    appOpenRetryRunnable?.let { mainHandler.removeCallbacks(it) }
                    appOpenRetryRunnable = null
                    if (BuildConfig.DEBUG) Log.d(TAG, "AppOpenAd loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAppOpen = false
                    appOpenFailures++
                    val isSslError = AdLoadPolicy.isSslOrWebViewError(error.message)
                    if (isSslError) {
                        markSslOrWebViewCircuitBreaker(error.message)
                    }
                    val backoff = AdLoadPolicy.computeBackoffMs(error.code, error.message, appOpenFailures)
                    appOpenNextAllowedElapsedMs = SystemClock.elapsedRealtime() + backoff
                    if (BuildConfig.DEBUG) Log.w(TAG, "AppOpenAd deferred ${backoff / 1000}s (code ${error.code})")
                    if (!isSslError && appOpenFailures <= MAX_AUTO_RETRIES) {
                        appOpenRetryRunnable?.let { mainHandler.removeCallbacks(it) }
                        val runnable = Runnable { loadAppOpenAd() }
                        appOpenRetryRunnable = runnable
                        mainHandler.postDelayed(runnable, backoff)
                    }
                }
            }
        )
    }

    fun showAppOpenAdIfAvailable(activity: Activity, onComplete: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        if (isShowingFullScreenAd ||
            activity.isFinishing ||
            activity.isDestroyed ||
            now < suppressAppOpenUntilMs ||
            now - lastAppOpenShownMs < APP_OPEN_COOLDOWN_MS
        ) {
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
        if (!_isSdkInitialized.value) {
            pendingInterstitialLoad = true
            initialize()
            return
        }
        if (!_adHostHealthy.value || !isNetworkAvailable()) return
        val now = SystemClock.elapsedRealtime()
        if (now < tlsBlockedUntilElapsedMs || now < interstitialNextAllowedElapsedMs) return

        isLoadingInterstitial = true
        InterstitialAd.load(
            appContext,
            AdUnitIds.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    interstitialFailures = 0
                    interstitialNextAllowedElapsedMs = 0L
                    interstitialRetryRunnable?.let { mainHandler.removeCallbacks(it) }
                    interstitialRetryRunnable = null
                    if (BuildConfig.DEBUG) Log.d(TAG, "InterstitialAd loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    interstitialFailures++
                    val isSslError = AdLoadPolicy.isSslOrWebViewError(error.message)
                    if (isSslError) {
                        markSslOrWebViewCircuitBreaker(error.message)
                    }
                    val backoff = AdLoadPolicy.computeBackoffMs(error.code, error.message, interstitialFailures)
                    interstitialNextAllowedElapsedMs = SystemClock.elapsedRealtime() + backoff
                    if (BuildConfig.DEBUG) Log.w(TAG, "InterstitialAd deferred ${backoff / 1000}s (code ${error.code})")
                    if (!isSslError && interstitialFailures <= MAX_AUTO_RETRIES) {
                        interstitialRetryRunnable?.let { mainHandler.removeCallbacks(it) }
                        val runnable = Runnable { loadInterstitialAd() }
                        interstitialRetryRunnable = runnable
                        mainHandler.postDelayed(runnable, backoff)
                    }
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
    private fun startRewardedCooldownTicker() {
        rewardedCountdownRunnable?.let { mainHandler.removeCallbacks(it) }
        val ticker = object : Runnable {
            override fun run() {
                val remMs = rewardedNextAllowedElapsedMs - SystemClock.elapsedRealtime()
                if (remMs > 0L) {
                    _rewardedCooldownSeconds.value = ((remMs + 999L) / 1000L).toInt()
                    mainHandler.postDelayed(this, 1000L)
                } else {
                    _rewardedCooldownSeconds.value = 0
                    rewardedCountdownRunnable = null
                }
            }
        }
        rewardedCountdownRunnable = ticker
        ticker.run()
    }

    private fun clearRewardedTimers() {
        rewardedRetryRunnable?.let { mainHandler.removeCallbacks(it) }
        rewardedRetryRunnable = null
        rewardedCountdownRunnable?.let { mainHandler.removeCallbacks(it) }
        rewardedCountdownRunnable = null
        _rewardedCooldownSeconds.value = 0
    }

    fun loadRewardedAd(onLoadedCallback: (() -> Unit)? = null) {
        if (isRobolectricEnvironment()) return
        if (rewardedAd != null) {
            _rewardedAdReady.value = true
            _rewardedAdLoading.value = false
            _rewardedAdError.value = null
            clearRewardedTimers()
            onLoadedCallback?.invoke()
            return
        }
        if (_rewardedAdLoading.value) return

        val now = SystemClock.elapsedRealtime()
        if (now < tlsBlockedUntilElapsedMs) {
            _rewardedAdLoading.value = false
            if (_rewardedAdError.value.isNullOrBlank()) {
                _rewardedAdError.value =
                    "Ad server TLS blocked by network or DNS. Switch network or disable ad blocker."
            }
            return
        }

        if (!_isSdkInitialized.value) {
            pendingRewardedLoad = true
            _rewardedAdLoading.value = true
            _rewardedAdError.value = null
            initialize()
            return
        }

        if (!isNetworkAvailable()) {
            _rewardedAdLoading.value = false
            _rewardedAdError.value = "Connect to Wi-Fi or mobile data to load ad."
            return
        }

        if (!_adHostHealthy.value) {
            _rewardedAdLoading.value = false
            _rewardedAdError.value = "Ad servers unreachable right now. Retrying when connected."
            return
        }

        if (now < rewardedNextAllowedElapsedMs) {
            val remainingSec = ((rewardedNextAllowedElapsedMs - now + 999L) / 1000L).toInt()
            _rewardedCooldownSeconds.value = remainingSec
            _rewardedAdLoading.value = false
            if (_rewardedAdError.value.isNullOrBlank()) {
                _rewardedAdError.value = "Ad server is warming up. Auto-retrying shortly..."
            }
            startRewardedCooldownTicker()
            return
        }

        _rewardedAdLoading.value = true
        _rewardedAdError.value = null

        RewardedAd.load(
            appContext,
            AdUnitIds.effectiveRewarded(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedFailures = 0
                    rewardedNextAllowedElapsedMs = 0L
                    clearRewardedTimers()
                    _rewardedAdReady.value = true
                    _rewardedAdLoading.value = false
                    _rewardedAdError.value = null
                    if (BuildConfig.DEBUG) Log.d(TAG, "RewardedAd loaded")
                    onLoadedCallback?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    rewardedFailures++
                    val isSslError = AdLoadPolicy.isSslOrWebViewError(error.message)
                    if (isSslError) {
                        markSslOrWebViewCircuitBreaker(error.message)
                    }
                    val backoffMs = AdLoadPolicy.computeBackoffMs(error.code, error.message, rewardedFailures)
                    rewardedNextAllowedElapsedMs = SystemClock.elapsedRealtime() + backoffMs
                    _rewardedAdReady.value = false
                    _rewardedAdLoading.value = false
                    _rewardedAdError.value = AdLoadPolicy.sanitizeUserErrorMessage(error.code, error.message)
                    startRewardedCooldownTicker()
                    if (BuildConfig.DEBUG) Log.w(TAG, "RewardedAd deferred ${backoffMs / 1000}s (code ${error.code})")

                    if (!isSslError && rewardedFailures <= MAX_AUTO_RETRIES) {
                        rewardedRetryRunnable?.let { mainHandler.removeCallbacks(it) }
                        val runnable = Runnable { loadRewardedAd() }
                        rewardedRetryRunnable = runnable
                        mainHandler.postDelayed(runnable, backoffMs + 250L)
                    }
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
                _rewardedAdError.value = "Unable to display ad right now. Retrying shortly..."
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
        BannerDockPlaceholder(modifier)
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adMobManager = remember(context) {
        (context.applicationContext as? TurboGameApplication)?.adMobManager
    }
    val sdkReady by (adMobManager?.isSdkInitialized ?: remember { MutableStateFlow(false) })
        .collectAsState()
    val hostHealthy by (adMobManager?.adHostHealthy ?: remember { MutableStateFlow(false) })
        .collectAsState()

    if (!sdkReady || !hostHealthy) {
        BannerDockPlaceholder(modifier)
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .testTag("admob_banner_container")
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightSurface.copy(alpha = 0.94f))
            .border(1.dp, CyberBorder.copy(alpha = 0.65f), RoundedCornerShape(12.dp)),
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
                    override fun onAdLoaded() {
                        adMobManager?.recordBannerSuccess()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adMobManager?.recordBannerFailure(error)
                    }
                }
            }
        }

        var hasRequestedLoad by remember(adView) { mutableStateOf(false) }

        LaunchedEffect(sdkReady, hostHealthy, adView) {
            if (sdkReady && hostHealthy && !hasRequestedLoad) {
                val allowed = adMobManager?.canAttemptBannerLoad() ?: false
                if (allowed) {
                    hasRequestedLoad = true
                    adView.loadAd(AdRequest.Builder().build())
                }
            }
        }

        DisposableEffect(lifecycleOwner, adView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> runCatching { adView.resume() }
                    Lifecycle.Event.ON_PAUSE -> runCatching { adView.pause() }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                runCatching { adView.destroy() }
            }
        }

        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BannerDockPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag("admob_banner_container")
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightSurface.copy(alpha = 0.92f))
            .border(1.dp, CyberBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("Sponsored Banner", style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
