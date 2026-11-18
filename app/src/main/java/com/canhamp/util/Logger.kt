package com.canhamp.util

import android.util.Log
import com.canhamp.BuildConfig

/**
 * Structured logging seam.
 *
 * Callers log only counts, ids, durations and error class names. Never log titles, artists, file
 * names, paths or URIs: they are personal data (B75).
 */
interface Logger {
    fun d(tag: String, msg: String, t: Throwable? = null)
    fun i(tag: String, msg: String, t: Throwable? = null)
    fun w(tag: String, msg: String, t: Throwable? = null)
    fun e(tag: String, msg: String, t: Throwable? = null)
}

/** Logs through [Log]; debug and info lines are dropped in release builds. */
class AndroidLogger : Logger {
    override fun d(tag: String, msg: String, t: Throwable?) {
        if (BuildConfig.DEBUG) Log.d(tag, msg, t)
    }

    override fun i(tag: String, msg: String, t: Throwable?) {
        if (BuildConfig.DEBUG) Log.i(tag, msg, t)
    }

    override fun w(tag: String, msg: String, t: Throwable?) {
        Log.w(tag, msg, t)
    }

    override fun e(tag: String, msg: String, t: Throwable?) {
        Log.e(tag, msg, t)
    }
}

/** Discards everything; for tests. */
object NoopLogger : Logger {
    override fun d(tag: String, msg: String, t: Throwable?) = Unit
    override fun i(tag: String, msg: String, t: Throwable?) = Unit
    override fun w(tag: String, msg: String, t: Throwable?) = Unit
    override fun e(tag: String, msg: String, t: Throwable?) = Unit
}
