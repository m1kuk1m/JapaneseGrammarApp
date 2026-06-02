package com.example.japanesegrammarapp.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

object ScreenshotHelper {
    fun captureActivity(activity: Activity, callback: (Bitmap?) -> Unit) {
        val view = activity.window.decorView.rootView
        if (view.width == 0 || view.height == 0) {
            callback(null)
            return
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        callback(bitmap)
    }
}