package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.ScreenshotHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureActivity : ComponentActivity() {

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ScreenshotProjectionService::class.java).apply {
                putExtra(ScreenshotProjectionService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenshotProjectionService.EXTRA_RESULT_DATA, data)
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }.onFailure {
                Toast.makeText(this, "Unable to start capture service", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Screenshot permission cancelled", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (mgr == null) {
            Toast.makeText(this, "Screen capture unavailable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        runCatching {
            captureLauncher.launch(mgr.createScreenCaptureIntent())
        }.onFailure {
            Toast.makeText(this, "Unable to request screen capture", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

class ScreenshotProjectionService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "turbo_screenshot_channel"
        private const val NOTIF_ID = 7002
        private const val CAPTURE_TIMEOUT_MS = 8_000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var timeoutRunnable: Runnable? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = buildNotification()
        val foregroundStarted = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        }.isSuccess
        if (!foregroundStarted) {
            stopAndClearForeground()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopAndClearForeground()
            return START_NOT_STICKY
        }

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        val projection = mgr?.getMediaProjection(resultCode, resultData)
        if (projection == null) {
            stopAndClearForeground()
            return START_NOT_STICKY
        }
        mediaProjection = projection

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.densityDpi.coerceAtLeast(160)
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            width = bounds.width().coerceAtLeast(720)
            height = bounds.height().coerceAtLeast(720)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels.coerceAtLeast(720)
            height = metrics.heightPixels.coerceAtLeast(720)
        }

        val captured = AtomicBoolean(false)
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                if (captured.compareAndSet(false, true)) {
                    stopAndClearForeground()
                } else {
                    cleanup()
                }
            }
        }
        projectionCallback = callback
        projection.registerCallback(callback, mainHandler)

        val timeout = Runnable {
            if (captured.compareAndSet(false, true)) {
                Toast.makeText(applicationContext, "Screenshot timed out", Toast.LENGTH_SHORT).show()
                stopAndClearForeground()
            }
        }
        timeoutRunnable = timeout
        mainHandler.postDelayed(timeout, CAPTURE_TIMEOUT_MS)

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        reader.setOnImageAvailableListener({ r ->
            if (!captured.compareAndSet(false, true)) return@setOnImageAvailableListener
            val image = r.acquireLatestImage()
            if (image == null) {
                captured.set(false)
                return@setOnImageAvailableListener
            }
            timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
            timeoutRunnable = null

            val croppedBitmap = runCatching {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = (rowStride - pixelStride * width).coerceAtLeast(0)
                val fullBitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                try {
                    fullBitmap.copyPixelsFromBuffer(buffer)
                    Bitmap.createBitmap(fullBitmap, 0, 0, width, height)
                } finally {
                    if (!fullBitmap.isRecycled) fullBitmap.recycle()
                }
            }.getOrNull()
            runCatching { image.close() }
            cleanup()

            serviceScope.launch {
                val savedPath = try {
                    croppedBitmap?.let { ScreenshotHelper.saveBitmapToGallery(applicationContext, it) }
                } finally {
                    if (croppedBitmap != null && !croppedBitmap.isRecycled) {
                        croppedBitmap.recycle()
                    }
                }
                withContext(Dispatchers.Main) {
                    val msg = if (savedPath != null) "Saved: $savedPath" else "Failed to save screenshot"
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                    stopAndClearForeground()
                }
            }
        }, mainHandler)

        // Small delay so the permission dialog finishes fading out before capturing
        mainHandler.postDelayed({
            if (captured.get()) return@postDelayed
            runCatching {
                virtualDisplay = projection.createVirtualDisplay(
                    "TurboGameXCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface,
                    null,
                    mainHandler
                )
            }.onFailure {
                if (captured.compareAndSet(false, true)) {
                    stopAndClearForeground()
                }
            }
        }, 280L)

        return START_NOT_STICKY
    }

    private fun stopAndClearForeground() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        cleanup()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun cleanup() {
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.setOnImageAvailableListener(null, null) }
        runCatching { imageReader?.close() }
        projectionCallback?.let { cb -> runCatching { mediaProjection?.unregisterCallback(cb) } }
        runCatching { mediaProjection?.stop() }
        projectionCallback = null
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, "Game Screenshot", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Capturing Game Screenshot")
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
        cleanup()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
