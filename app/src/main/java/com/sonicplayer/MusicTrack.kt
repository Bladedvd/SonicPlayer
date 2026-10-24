package com.sonicplayer

import android.net.Uri

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: Uri,
    val durationMs: Long
)
