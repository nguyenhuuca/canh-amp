package com.canhamp.util

import android.Manifest
import android.os.Build

/** The runtime permission that grants read access to audio files on this Android version. */
fun audioPermission(sdk: Int = Build.VERSION.SDK_INT): String =
    if (sdk >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
