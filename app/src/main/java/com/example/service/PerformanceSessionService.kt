package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PerformanceSessionService : Service() {

    companion object {
        const val CHANNEL_ID = "turbo_x_super_perf_channel"
        const val NOTIFICATION_ID = 4202
        const val ACTION_START_SESSION = "com.example.action.START_SUPER_PERF_SESSION"
        const val ACTION_STOP_SESSION = "com.example.action.STOP_SUPER_PERF_SESSION"
        const val EXTRA_GAME_NAME = "extra_game_name"
        const val EXTRA_GAME_PACKAGE = "extra_game_package"
        const val EXTRA_ACQUIRE_WAKELOCK = "extra_acquire_wakelock"
        const val ROUTE_SUPER_PERF = "super_performance"
        private const val WAKELOCK_TAG = "TurboX:SuperPerfSession"
        private const val MAX_WAKELOCK_MS = 30 * 60 * 1000L

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        private val _wakeLockHeldFlow = MutableStateFlow(false)
        val wakeLockHeldFlow: StateFlow<Boolean> = _wakeLockHeldFlow.asStateFlow()

        @Volatile
        var activeTargetPackage: String? = null
            private set

        @Volatile
        var onExternalStopRequested: (() -> Unit)? = null

        fun createStartIntent(
            context: Context,
            gameName: String,
            gamePackage: String,
            acquireWakeLock: Boolean = true
        ): Intent {
            return Intent(context, PerformanceSessionService::class.java).apply {
                action = ACTION_START_SESSION
                putExtra(EXTRA_GAME_NAME, gameName)
                putExtra(EXTRA_GAME_PACKAGE, gamePackage)
                putExtra(EXTRA_ACQUIRE_WAKELOCK, acquireWakeLock)
            }
        }

        fun resetStateForCleanShutdown() {
            _isRunningFlow.value = false
            _wakeLockHeldFlow.value = false
            activeTargetPackage = null
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SESSION -> {
                releaseWakeLockSafely()
                resetStateForCleanShutdown()
                onExternalStopRequested?.invoke()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_SESSION, null -> {
                val gameName = intent?.getStringExtra(EXTRA_GAME_NAME)?.takeIf { it.isNotBlank() }
                    ?: "Selected Game"
                val gamePackage = intent?.getStringExtra(EXTRA_GAME_PACKAGE)?.takeIf { it.isNotBlank() }
                val shouldHoldWakeLock = intent?.getBooleanExtra(EXTRA_ACQUIRE_WAKELOCK, true) ?: true

                activeTargetPackage = gamePackage
                _isRunningFlow.value = true

                val notification = buildNotification(gameName)
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                }

                if (shouldHoldWakeLock) {
                    acquireWakeLockSafely()
                } else {
                    releaseWakeLockSafely()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        releaseWakeLockSafely()
        resetStateForCleanShutdown()
        onExternalStopRequested?.invoke()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseWakeLockSafely()
        resetStateForCleanShutdown()
        super.onDestroy()
    }

    private fun acquireWakeLockSafely() {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            val existing = wakeLock
            if (existing != null && existing.isHeld) {
                _wakeLockHeldFlow.value = true
                return
            }
            val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
                setReferenceCounted(false)
                acquire(MAX_WAKELOCK_MS)
            }
            wakeLock = lock
            _wakeLockHeldFlow.value = lock.isHeld
        }.onFailure {
            _wakeLockHeldFlow.value = false
        }
    }

    private fun releaseWakeLockSafely() {
        runCatching {
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                }
            }
        }
        wakeLock = null
        _wakeLockHeldFlow.value = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Turbo X Super High Performance",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active selected-game extreme performance session & thermal guard"
                setShowBadge(false)
            }
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(gameName: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            30,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(GameOverlayService.EXTRA_START_ROUTE, ROUTE_SUPER_PERF)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            31,
            Intent(this, PerformanceSessionService::class.java).apply {
                action = ACTION_STOP_SESSION
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Turbo X • Super High Performance")
            .setContentText("Active for $gameName • Tap to inspect or stop")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "Stop & Restore", stopIntent)
            .build()
    }
}
