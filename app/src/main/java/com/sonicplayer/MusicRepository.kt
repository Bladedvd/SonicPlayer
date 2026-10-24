package com.sonicplayer

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

class MusicRepository(private val context: Context) {
    fun loadTracks(): List<MusicTrack> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val tracks = mutableListOf<MusicTrack>()
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        context.contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val musicCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)
            while (cursor.moveToNext()) {
                if (cursor.getInt(musicCol) != 1) continue
                val id = cursor.getLong(idCol)
                tracks += MusicTrack(
                    id = id,
                    title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Faixa sem título",
                    artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "Artista desconhecido",
                    album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Álbum desconhecido",
                    uri = ContentUris.withAppendedId(collection, id),
                    durationMs = cursor.getLong(durationCol)
                )
            }
        }
        return tracks
    }
}
