package com.canhamp.util

import android.os.Handler
import android.os.Looper
import android.view.Window
import java.util.concurrent.atomic.AtomicInteger
import androidx.metrics.performance.JankStats

private const val TAG = "Jank"
const val JANK_REPORT_MS = 30_000L

/** Counts frames and janky frames since the last [report]; no per-frame data is kept. */
class JankCounter {
    private val frames = AtomicInteger()
    private val janky = AtomicInteger()

    /** Called from JankStats' own thread. */
    fun onFrame(isJank: Boolean) {
        frames.incrementAndGet()
        if (isJank) janky.incrementAndGet()
    }

    /** The summary line for the window since the last call, and a reset. Counts only (B75). */
    fun report(): String {
        return "frames=${frames.getAndSet(0)} janky=${janky.getAndSet(0)}"
    }
}

/**
 * Debug-only frame timing: logs the jank frame count every [JANK_REPORT_MS] while started.
 * Frames arrive on JankStats' thread, reports go out from the main thread.
 */
class JankMonitor(window: Window, private val logger: Logger) {
    private val counter = JankCounter()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            logger.d(TAG, counter.report())
            handler.postDelayed(this, JANK_REPORT_MS)
        }
    }
    private val stats = JankStats.createAndTrack(window, JankStats.OnFrameListener { frame ->
        counter.onFrame(frame.isJank)
    })

    fun start() {
        stats.isTrackingEnabled = true
        handler.postDelayed(tick, JANK_REPORT_MS)
    }

    fun stop() {
        stats.isTrackingEnabled = false
        handler.removeCallbacks(tick)
    }
}
