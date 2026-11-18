package com.example.data

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenshotHelper {

    fun captureActivityWindow(activity: Activity, onComplete: (Boolean, String) -> Unit) {
        val window = activity.window
        val decorView = window.decorView
        val width = decorView.width
        val height = decorView.height
        if (width <= 0 || height <= 0) {
            onComplete(false, "Window not ready for capture")
            return
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(
                window,
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        val saved = saveBitmapToGallery(activity.applicationContext, bitmap)
                        bitmap.recycle()
                        onComplete(saved != null, saved ?: "Failed to save screenshot")
                    } else {
                        bitmap.recycle()
                        onComplete(false, "Screenshot capture error ($result)")
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } else {
            val canvas = Canvas(bitmap)
            decorView.draw(canvas)
            val saved = saveBitmapToGallery(activity.applicationContext, bitmap)
            bitmap.recycle()
            onComplete(saved != null, saved ?: "Failed to save screenshot")
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): String? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "TurboGameX_$timestamp.png"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TurboGameX")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            runCatching {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "Pictures/TurboGameX/$filename"
            }.getOrNull()
        } else {
            runCatching {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "TurboGameX")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                file.name
            }.getOrNull()
        }
    }
}
