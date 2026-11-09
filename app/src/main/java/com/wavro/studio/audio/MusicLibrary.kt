package com.wavro.studio.audio

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: Uri
)

class MusicLibrary(private val context: Context) {
    suspend fun scanDeviceForMusic(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Unknown"
                val artist = it.getString(artistCol) ?: "Unknown"
                val duration = it.getLong(durationCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                tracks.add(Track(id, title, artist, duration, contentUri))
            }
        }
        tracks
    }
}