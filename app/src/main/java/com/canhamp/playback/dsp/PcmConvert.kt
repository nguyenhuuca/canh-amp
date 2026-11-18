package com.canhamp.playback.dsp

import java.nio.ByteBuffer

/** 16-bit PCM <-> float [-1, 1) helpers; byte order is whatever the buffer is set to. */
object PcmConvert {
    private const val SCALE = 32768f

    fun toFloat(sample: Short): Float = sample / SCALE

    fun toShort(x: Float): Short = Math.round(x * SCALE).coerceIn(-32768, 32767).toShort()

    /** Reads the next sample from [buf], advancing its position by 2. */
    fun readFloat(buf: ByteBuffer): Float = toFloat(buf.short)

    /** Writes [x] (clamped) to [buf], advancing its position by 2. */
    fun writeFloat(buf: ByteBuffer, x: Float) {
        buf.putShort(toShort(x))
    }
}
