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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            Toast.makeText(this, "Screenshot permission cancelled", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(mgr.createScreenCaptureIntent())
    }
}

class ScreenshotProjectionService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "turbo_screenshot_channel"
        private const val NOTIF_ID = 7002
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, notification)
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
            stopSelf()
            return START_NOT_STICKY
        }

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = mgr.getMediaProjection(resultCode, resultData)
        if (projection == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        mediaProjection = projection

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels.coerceAtLeast(720)
        val height = metrics.heightPixels.coerceAtLeast(720)
        val density = metrics.densityDpi

        val handler = Handler(Looper.getMainLooper())
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                cleanup()
            }
        }, handler)

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        var captured = false

        reader.setOnImageAvailableListener({ r ->
            if (captured) return@setOnImageAvailableListener
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            captured = true
            runCatching {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val fullBitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                fullBitmap.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(fullBitmap, 0, 0, width, height)
                fullBitmap.recycle()
                val savedPath = ScreenshotHelper.saveBitmapToGallery(applicationContext, cropped)
                cropped.recycle()
                handler.post {
                    val msg = if (savedPath != null) "Saved: $savedPath" else "Failed to save screenshot"
                    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
            image.close()
            cleanup()
            stopSelf()
        }, handler)

        // Small delay so the permission dialog finishes fading out before capturing
        handler.postDelayed({
            runCatching {
                virtualDisplay = projection.createVirtualDisplay(
                    "TurboGameXCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface,
                    null,
                    handler
                )
            }.onFailure {
                cleanup()
                stopSelf()
            }
        }, 280L)

        return START_NOT_STICKY
    }

    private fun cleanup() {
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { mediaProjection?.stop() }
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
        super.onDestroy()
        cleanup()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
