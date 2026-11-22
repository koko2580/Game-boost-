package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.ads.AdMobManager
import com.example.ads.isRobolectricEnvironment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TurboGameApplication :
    Application(),
    DefaultLifecycleObserver,
    Application.ActivityLifecycleCallbacks {

    companion object {
        private val _isForegroundAppFlow = MutableStateFlow(false)
        val isForegroundAppFlow = _isForegroundAppFlow.asStateFlow()
    }

    lateinit var adMobManager: AdMobManager
        private set

    private var currentMainActivity: Activity? = null
    private var wentToBackground: Boolean = false
    private var activeResumedCount: Int = 0

    override fun onCreate() {
        super<Application>.onCreate()
        adMobManager = AdMobManager(this)
        if (!isRobolectricEnvironment()) {
            registerActivityLifecycleCallbacks(this)
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            adMobManager.initialize()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStart(owner)
        if (!wentToBackground) return
        wentToBackground = false
        val activity = currentMainActivity ?: return
        adMobManager.showAppOpenAdIfAvailable(activity)
    }

    override fun onStop(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStop(owner)
        wentToBackground = true
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is MainActivity) {
            currentMainActivity = activity
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity is MainActivity && !adMobManager.isShowingFullScreenAd) {
            currentMainActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is MainActivity && !adMobManager.isShowingFullScreenAd) {
            currentMainActivity = activity
        }
        activeResumedCount += 1
        _isForegroundAppFlow.value = true
    }

    override fun onActivityPaused(activity: Activity) {
        activeResumedCount = (activeResumedCount - 1).coerceAtLeast(0)
        if (activeResumedCount == 0) {
            _isForegroundAppFlow.value = false
        }
    }

    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentMainActivity === activity) {
            currentMainActivity = null
        }
    }
}
