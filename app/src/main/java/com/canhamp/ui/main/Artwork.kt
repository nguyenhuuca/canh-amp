package com.canhamp.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Largest power-of-two sample size that keeps both sides at or above [targetPx]. */
fun artworkSampleSize(width: Int, height: Int, targetPx: Int): Int {
    if (width <= 0 || height <= 0 || targetPx <= 0) return 1
    var sample = 1
    while (width / (sample * 2) >= targetPx && height / (sample * 2) >= targetPx) sample *= 2
    return sample
}

/** Decodes embedded cover bytes near [targetPx] (B32). Returns null when the bytes are not an image. Call off the main thread. */
fun decodeArtwork(data: ByteArray, targetPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts = BitmapFactory.Options().apply {
        inSampleSize = artworkSampleSize(bounds.outWidth, bounds.outHeight, targetPx)
    }
    return BitmapFactory.decodeByteArray(data, 0, data.size, opts)
}
