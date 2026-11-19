package com.canhamp.playback

/** What the exported media session accepts from other apps (system and Bluetooth controllers included). */
internal object SessionPolicy {
    private val allowedSchemes = setOf("content", "file")

    /** Only local audio is playable: remote and malformed uris are refused. */
    fun isAllowedMediaUri(uri: String?): Boolean {
        val value = uri?.trim().orEmpty()
        val colon = value.indexOf(':')
        if (colon <= 0) return false
        return value.substring(0, colon).lowercase() in allowedSchemes
    }

    /** Own app and system-trusted controllers (media buttons, Bluetooth, notification) may connect; others may not. */
    fun isAllowedController(trusted: Boolean, controllerPackage: String, ownPackage: String): Boolean =
        trusted || controllerPackage == ownPackage
}
