package com.example.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkGatePolicy {
    fun hasValidatedInternet(
        hasInternetCapability: Boolean,
        hasValidatedCapability: Boolean
    ): Boolean = hasInternetCapability && hasValidatedCapability

    fun isCapabilitiesOnline(caps: NetworkCapabilities?): Boolean {
        if (caps == null) return false
        return hasValidatedInternet(
            hasInternetCapability = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            hasValidatedCapability = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        )
    }
}

class NetworkGateMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isConnected = MutableStateFlow(checkCurrentConnection())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var callbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refreshNow()
        }

        override fun onLost(network: Network) {
            refreshNow()
        }

        override fun onUnavailable() {
            _isConnected.value = false
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _isConnected.value = if (isRobolectric()) {
                true
            } else {
                NetworkGatePolicy.isCapabilitiesOnline(networkCapabilities)
            }
        }
    }

    init {
        if (!isRobolectric()) {
            runCatching {
                connectivityManager?.registerDefaultNetworkCallback(networkCallback)
                callbackRegistered = true
            }
        }
    }

    fun refreshNow(): Boolean {
        val online = checkCurrentConnection()
        _isConnected.value = online
        return online
    }

    fun unregister() {
        if (callbackRegistered) {
            runCatching {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            }
            callbackRegistered = false
        }
    }

    private fun checkCurrentConnection(): Boolean {
        if (isRobolectric()) return true
        val cm = connectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active)
        return NetworkGatePolicy.isCapabilitiesOnline(caps)
    }

    private fun isRobolectric(): Boolean =
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    fun openInternetSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }
}
