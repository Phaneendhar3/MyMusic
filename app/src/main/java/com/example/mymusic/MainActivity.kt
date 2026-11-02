package com.example.mymusic

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
                    MusicScreen()
                }
            }
        }
    }
}

// Turns our list of songs into items the player understands
fun songsToMediaItems(songs: List<Song>): List<MediaItem> {
    return songs.map { song ->
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            song.id
        )
        MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(uri)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder().setMediaUri(uri).build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(uri)   // used for lock screen and notification art
                    .build()
            )
            .build()
    }
}

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val originalListState = rememberLazyListState()
    val shuffledListState = rememberLazyListState()

    // ---------- Permissions ----------
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var shuffledSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }
    var queueMode by remember { mutableStateOf(loadQueueMode(context)) }
    var showPlayer by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val (loaded, savedShuffle) = withContext(Dispatchers.IO) {
                val l = loadSongs(context)
                Pair(l, loadSavedShuffle(context, l))
            }
            allSongs = loaded
            shuffledSongs = savedShuffle

            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            launcher.launch(permission)
        }
    }

    // ---------- Connect to the background player ----------
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var currentSongId by remember { mutableStateOf<Long?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            { controller = future.get() },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            MediaController.releaseFuture(future)
            controller = null
        }
    }

    // Listen for changes coming from the player
    DisposableEffect(controller) {
        val c = controller
        if (c == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (!playing) {
                        // Paused: remember where we stopped
                        val id = c.currentMediaItem?.mediaId?.toLongOrNull()
                        if (id != null) savePlaybackState(context, id, c.currentPosition)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val id = mediaItem?.mediaId?.toLongOrNull()
                    currentSongId = id
                    position = c.currentPosition
                    if (id != null) savePlaybackState(context, id, c.currentPosition)
                }
            }
            c.addListener(listener)
            onDispose { c.removeListener(listener) }
        }
    }

    // When songs and player are both ready: catch up, or restore where you left off
    LaunchedEffect(allSongs, controller) {
        val c = controller ?: return@LaunchedEffect
        if (allSongs.isEmpty()) return@LaunchedEffect

        if (c.mediaItemCount > 0) {
            // The player is still alive from before: just catch up with it
            currentSongId = c.currentMediaItem?.mediaId?.toLongOrNull()
            isPlaying = c.isPlaying
            position = c.currentPosition
            selectedTab = if (queueMode == MODE_SHUFFLED) 1 else 0
        } else {
            // Fresh start: restore the last list, song and position (paused)
            var mode = loadQueueMode(context)
            if (mode == MODE_SHUFFLED && shuffledSongs.isEmpty()) mode = MODE_ORIGINAL

            val queue = if (mode == MODE_SHUFFLED) shuffledSongs else allSongs
            val lastId = loadLastSongId(context)
            val foundIndex = queue.indexOfFirst { it.id == lastId }
            val startIndex = if (foundIndex >= 0) foundIndex else 0
            val startPosition = if (foundIndex >= 0) loadLastPosition(context) else 0L

            c.setMediaItems(songsToMediaItems(queue), startIndex, startPosition)
            c.prepare()

            queueMode = mode
            currentSongId = queue[startIndex].id
            position = startPosition
            selectedTab = if (mode == MODE_SHUFFLED) 1 else 0
        }
    }

    // While music is playing, update the slider and save our place every 2 seconds
    LaunchedEffect(isPlaying, controller) {
        var ticks = 0
        while (isPlaying) {
            val c = controller ?: break
            position = c.currentPosition
            ticks++
            if (ticks % 4 == 0) {
                val id = c.currentMediaItem?.mediaId?.toLongOrNull()
                if (id != null) savePlaybackState(context, id, c.currentPosition)
            }
            delay(500)
        }
    }

    // Plays a song from a list (original or shuffled)
    val playFrom: (List<Song>, String, Int) -> Unit = { list, mode, index ->
        val c = controller
        if (c != null) {
            if (queueMode != mode) {
                c.setMediaItems(songsToMediaItems(list), index, 0L)
                c.prepare()
                queueMode = mode
                saveQueueMode(context, mode)
            } else {
                c.seekTo(index, 0L)
            }
            c.play()
        }
    }

    val songsById = remember(allSongs) { allSongs.associateBy { it.id } }
    val currentSong = currentSongId?.let { songsById[it] }

    // For the "now playing" jump button
    val activeList = if (selectedTab == 0) allSongs else shuffledSongs
    val activeListState = if (selectedTab == 0) originalListState else shuffledListState
    val currentIndexInTab = activeList.indexOfFirst { it.id == currentSongId }

    // Back gesture closes the full-screen player first
    BackHandler(enabled = showPlayer) { showPlayer = false }

    // ---------- Screen ----------
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Ink)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ప్రియరాగాలు",
                    color = TextMain,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (hasPermission) {
                    Text(
                        text = "${allSongs.size} tracks",
                        color = TextDim,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Tabs
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SegmentedTabs(
                    selected = selectedTab,
                    labels = listOf("Songs", "Shuffled"),
                    onSelect = { selectedTab = it }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // The list area, with the buttons floating on top
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (!hasPermission) {
                    Text(
                        text = "Permission needed to read your music",
                        color = TextDim,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (selectedTab == 0) {
                    SongList(allSongs, currentSongId, originalListState) { index ->
                        playFrom(allSongs, MODE_ORIGINAL, index)
                    }
                } else if (shuffledSongs.isEmpty()) {
                    Text(
                        text = "No shuffled order yet.\nPress Shuffle to create one.",
                        color = TextDim,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    SongList(shuffledSongs, currentSongId, shuffledListState) { index ->
                        playFrom(shuffledSongs, MODE_SHUFFLED, index)
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 4.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // "Now playing" jump button
                    if (currentIndexInTab >= 0) {
                        IconBtn(
                            type = IconType.Locate,
                            size = 52.dp,
                            iconSize = 26.dp,
                            background = PanelHigh,
                            tint = Saffron,
                            onClick = {
                                scope.launch {
                                    activeListState.animateScrollToItem(
                                        index = currentIndexInTab,
                                        scrollOffset = -250
                                    )
                                }
                            }
                        )
                    }

                    // Shuffle button (same logic as before)
                    ExtendedFloatingActionButton(
                        onClick = {
                            val c = controller
                            if (c != null && allSongs.isNotEmpty()) {
                                val shuffled = allSongs.shuffled()   // new random order
                                saveOrder(context, shuffled)         // remember it
                                shuffledSongs = shuffled
                                c.setMediaItems(songsToMediaItems(shuffled), 0, 0L)
                                c.prepare()
                                c.play()                             // start from the first song
                                queueMode = MODE_SHUFFLED
                                saveQueueMode(context, MODE_SHUFFLED)
                                currentSongId = shuffled[0].id
                                position = 0L
                                selectedTab = 1
                                scope.launch { shuffledListState.scrollToItem(0) }
                            }
                        },
                        containerColor = Saffron,
                        contentColor = Ink,
                        shape = RoundedCornerShape(22.dp)
                    ) {
//                        AppIcon(IconType.Sparkle, Modifier.width(22.dp).height(22.dp), Ink)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Shuffle", fontWeight = FontWeight.Bold)
                        Text("Shuffle", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Mini player
            if (currentSong != null) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    MiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        position = position,
                        onOpen = { showPlayer = true },
                        onPlayPause = {
                            if (isPlaying) controller?.pause() else controller?.play()
                        },
                        onNext = { controller?.seekToNextMediaItem() }
                    )
                }
            }
        }

        // Full-screen player slides up over everything
        AnimatedVisibility(
            visible = showPlayer && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            if (currentSong != null) {
                FullPlayer(
                    song = currentSong,
                    isPlaying = isPlaying,
                    position = position,
                    onPositionChange = { position = it },
                    onSeekFinished = { controller?.seekTo(position) },
                    onClose = { showPlayer = false },
                    onPrevious = { controller?.seekToPreviousMediaItem() },
                    onPlayPause = {
                        if (isPlaying) controller?.pause() else controller?.play()
                    },
                    onNext = { controller?.seekToNextMediaItem() }
                )
            }
        }
    }
}