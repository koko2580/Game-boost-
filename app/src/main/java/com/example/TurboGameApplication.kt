package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.ads.AdMobManager
import com.example.ads.isRobolectricEnvironment

class TurboGameApplication :
    Application(),
    DefaultLifecycleObserver,
    Application.ActivityLifecycleCallbacks {

    lateinit var adMobManager: AdMobManager
        private set

    private var currentMainActivity: Activity? = null
    private var wentToBackground: Boolean = false

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
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentMainActivity === activity) {
            currentMainActivity = null
        }
    }
}
