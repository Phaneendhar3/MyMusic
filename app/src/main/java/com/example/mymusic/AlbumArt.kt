package com.example.mymusic

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Remembers pictures we've already loaded, so scrolling stays smooth
object ArtCache {
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8L).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val missing = java.util.Collections.synchronizedSet(HashSet<String>())

    fun peek(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
    fun isMissing(key: String): Boolean = missing.contains(key)
    fun markMissing(key: String) {
        missing.add(key)
    }
}

// Asks Android for a song's album art (returns null if the song has none)
fun loadArt(context: Context, songId: Long, sizePx: Int): Bitmap? {
    val key = "$songId-$sizePx"
    ArtCache.peek(key)?.let { return it }
    if (ArtCache.isMissing(key)) return null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songId
            )
            val bitmap = context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
            ArtCache.put(key, bitmap)
            return bitmap
        } catch (e: Exception) {
            // no artwork for this song
        }
    }
    ArtCache.markMissing(key)
    return null
}

@Composable
fun AlbumArt(
    songId: Long,
    sizePx: Int,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp
) {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(songId, sizePx) { mutableStateOf(ArtCache.peek("$songId-$sizePx")) }

    LaunchedEffect(songId, sizePx) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) { loadArt(context, songId, sizePx) }
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(corner))) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ArtPlaceholder(songId)
        }
    }
}

// A colored "vinyl record" shown when a song has no album art
@Composable
fun ArtPlaceholder(songId: Long) {
    val palette = listOf(
        Color(0xFF2E2A4F) to Color(0xFF6C5CE7),
        Color(0xFF4A2C2A) to Color(0xFFE17055),
        Color(0xFF1F3B4D) to Color(0xFF00B8A9),
        Color(0xFF3D2C4E) to Color(0xFFE056A0),
        Color(0xFF3A3A1F) to Color(0xFFFFB84D)
    )
    val (c1, c2) = palette[(songId % palette.size).toInt()]

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(listOf(c1, c2)))
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension
        drawCircle(Color.White.copy(alpha = 0.10f), radius = r * 0.42f, center = center,
            style = Stroke(width = r * 0.02f))
        drawCircle(Color.White.copy(alpha = 0.12f), radius = r * 0.30f, center = center,
            style = Stroke(width = r * 0.02f))
        drawCircle(Color.White.copy(alpha = 0.20f), radius = r * 0.08f, center = center)
    }
}