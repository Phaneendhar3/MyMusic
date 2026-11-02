package com.example.mymusic

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executors

// Tells the notification and lock screen how to get each song's album art
@OptIn(UnstableApi::class)
class ArtBitmapLoader(private val context: Context) : BitmapLoader {

    private val executor = Executors.newSingleThreadExecutor()

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        executor.execute {
            try {
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) future.set(bitmap)
                else future.setException(IllegalStateException("Could not decode image"))
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        executor.execute {
            try {
                val songId = ContentUris.parseId(uri)
                val bitmap = loadArt(context, songId, 512)
                if (bitmap != null) future.set(bitmap)
                else future.setException(IllegalStateException("No artwork"))
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }
}

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)  // pauses for calls, etc.
            .setHandleAudioBecomingNoisy(true)          // pauses if headphones are unplugged
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setBitmapLoader(ArtBitmapLoader(this))
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    // Called when you swipe the app away from Recents
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // Keep playing if music is playing; otherwise shut down
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}