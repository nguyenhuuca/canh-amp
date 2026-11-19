package com.canhamp.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPolicyTest {
    @Test
    fun localSchemesAreAllowed() {
        assertTrue(SessionPolicy.isAllowedMediaUri("content://media/external/audio/media/1"))
        assertTrue(SessionPolicy.isAllowedMediaUri("file:///storage/emulated/0/a.mp3"))
        assertTrue(SessionPolicy.isAllowedMediaUri("CONTENT://x/1"))
    }

    @Test
    fun remoteAndMalformedUrisAreRejected() {
        assertFalse(SessionPolicy.isAllowedMediaUri("http://example.com/a.mp3"))
        assertFalse(SessionPolicy.isAllowedMediaUri("https://example.com/a.mp3"))
        assertFalse(SessionPolicy.isAllowedMediaUri("rtsp://example.com/a"))
        assertFalse(SessionPolicy.isAllowedMediaUri(null))
        assertFalse(SessionPolicy.isAllowedMediaUri(""))
        assertFalse(SessionPolicy.isAllowedMediaUri("/storage/a.mp3"))
        assertFalse(SessionPolicy.isAllowedMediaUri("://x"))
    }

    @Test
    fun onlyTrustedOrOwnControllersConnect() {
        assertTrue(SessionPolicy.isAllowedController(true, "com.android.systemui", "com.canhamp"))
        assertTrue(SessionPolicy.isAllowedController(false, "com.canhamp", "com.canhamp"))
        assertFalse(SessionPolicy.isAllowedController(false, "com.evil.app", "com.canhamp"))
    }
}
