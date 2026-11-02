package com.example.mymusic

import android.content.Context

const val MODE_ORIGINAL = "original"
const val MODE_SHUFFLED = "shuffled"

private const val PREFS_NAME = "music_prefs"
private const val KEY_ORDER = "saved_order"
private const val KEY_MODE = "queue_mode"
private const val KEY_LAST_SONG = "last_song_id"
private const val KEY_LAST_POSITION = "last_position"

private fun prefs(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

// Saves the shuffled order
fun saveOrder(context: Context, songs: List<Song>) {
    val text = songs.joinToString(",") { it.id.toString() }
    prefs(context).edit().putString(KEY_ORDER, text).apply()
}

// Loads the saved shuffled order (empty list if you never pressed Shuffle)
fun loadSavedShuffle(context: Context, allSongs: List<Song>): List<Song> {
    val text = prefs(context).getString(KEY_ORDER, null)
    if (text.isNullOrEmpty()) return emptyList()

    val savedIds = text.split(",").mapNotNull { it.toLongOrNull() }
    val songsById = allSongs.associateBy { it.id }

    // Songs in the saved order (songs you deleted are skipped)
    val ordered = savedIds.mapNotNull { songsById[it] }

    // Songs added since then go at the end
    val savedSet = savedIds.toSet()
    val newSongs = allSongs.filter { it.id !in savedSet }

    return ordered + newSongs
}

// Remembers which list the player is using: original or shuffled
fun saveQueueMode(context: Context, mode: String) {
    prefs(context).edit().putString(KEY_MODE, mode).apply()
}

fun loadQueueMode(context: Context): String =
    prefs(context).getString(KEY_MODE, MODE_ORIGINAL) ?: MODE_ORIGINAL

// Remembers the current song and the position inside it
fun savePlaybackState(context: Context, songId: Long, positionMs: Long) {
    prefs(context).edit()
        .putLong(KEY_LAST_SONG, songId)
        .putLong(KEY_LAST_POSITION, positionMs)
        .apply()
}

fun loadLastSongId(context: Context): Long = prefs(context).getLong(KEY_LAST_SONG, -1L)

fun loadLastPosition(context: Context): Long = prefs(context).getLong(KEY_LAST_POSITION, 0L)