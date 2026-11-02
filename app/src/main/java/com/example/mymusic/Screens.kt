package com.example.mymusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// 215000 milliseconds -> "3:35"
fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

// The two pill-shaped tabs at the top
@Composable
fun SegmentedTabs(selected: Int, labels: List<String>, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Saffron else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Ink else TextDim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// One song in the list
@Composable
fun SongRow(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrent) Panel else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(
            songId = song.id,
            sizePx = 160,
            modifier = Modifier.size(52.dp),
            corner = 14.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) Saffron else TextMain,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = TextDim,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent) {
            Spacer(modifier = Modifier.width(8.dp))
            AppIcon(IconType.Sparkle, Modifier.size(20.dp), Saffron)
        }
    }
}

@Composable
fun SongList(
    songs: List<Song>,
    currentSongId: Long?,
    listState: LazyListState,
    onSongClick: (Int) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(bottom = 150.dp)
    ) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                isCurrent = song.id == currentSongId,
                onClick = { onSongClick(index) }
            )
        }
    }
}

// The small bar at the bottom of the screen
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val fraction = if (song.durationMs > 0L) {
        (position.toFloat() / song.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(PanelHigh)
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                songId = song.id,
                sizePx = 160,
                modifier = Modifier.size(46.dp),
                corner = 12.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextMain,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = TextDim,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconBtn(
                type = if (isPlaying) IconType.Pause else IconType.Play,
                size = 44.dp,
                iconSize = 22.dp,
                background = Saffron,
                tint = Ink,
                onClick = onPlayPause
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconBtn(
                type = IconType.Next,
                size = 44.dp,
                iconSize = 22.dp,
                onClick = onNext
            )
        }
        // thin progress line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(Saffron)
            )
        }
    }
}

// The full-screen player
@Composable
fun FullPlayer(
    song: Song,
    isPlaying: Boolean,
    position: Long,
    onPositionChange: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
//    val maxValue = maxOf(song.durationMs, 1L).toFloat()
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Brush.verticalGradient(listOf(Color(0xFF2B2440), Ink, Ink)))
            // stops taps from reaching the list underneath
    val maxValue = maxOf(song.durationMs, 1L).toFloat()

    // ---- Drag down to close ----
    val offsetY = remember { Animatable(0f) }
    val dragScope = rememberCoroutineScope()
    val closeDistance = with(LocalDensity.current) { 140.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY.value > closeDistance) {
                            onClose()                                   // dragged far enough: close
                        } else {
                            dragScope.launch { offsetY.animateTo(0f) }  // not far enough: spring back
                        }
                    },
                    onDragCancel = {
                        dragScope.launch { offsetY.animateTo(0f) }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragScope.launch {
                            // only allow dragging downwards
                            offsetY.snapTo((offsetY.value + dragAmount).coerceAtLeast(0f))
                        }
                    }
                )
            }
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .background(Brush.verticalGradient(listOf(Color(0xFF2B2440), Ink, Ink)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBtn(type = IconType.Down, size = 48.dp, iconSize = 24.dp, onClick = onClose)
            Text(
                text = "NOW PLAYING",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = TextDim,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        AlbumArt(
            songId = song.id,
            sizePx = 720,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            corner = 32.dp
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = song.title,
                color = TextMain,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                color = TextDim,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = position.toFloat().coerceIn(0f, maxValue),
                onValueChange = { onPositionChange(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..maxValue,
                colors = SliderDefaults.colors(
                    thumbColor = Saffron,
                    activeTrackColor = Saffron,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(position), color = TextDim, style = MaterialTheme.typography.bodySmall)
                Text(formatTime(song.durationMs), color = TextDim, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBtn(type = IconType.Previous, size = 64.dp, iconSize = 32.dp, onClick = onPrevious)
            IconBtn(
                type = if (isPlaying) IconType.Pause else IconType.Play,
                size = 84.dp,
                iconSize = 40.dp,
                background = Saffron,
                tint = Ink,
                onClick = onPlayPause
            )
            IconBtn(type = IconType.Next, size = 64.dp, iconSize = 32.dp, onClick = onNext)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}