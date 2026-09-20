package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AudioTrack
import com.example.data.RepeatMode
import com.example.ui.theme.AppleProgressBg
import com.example.ui.theme.AppleProgressFill
import com.example.ui.theme.AppleQuaternaryBackground
import com.example.ui.theme.AppleSecondaryBackground
import com.example.ui.theme.AppleSeparator
import com.example.ui.theme.AppleSubtleBorder
import com.example.ui.theme.AppleTertiaryBackground
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.AppleTextTertiary
import com.example.viewmodel.QuranPlayerUiState

@Composable
fun PlayerControlPanel(
    state: QuranPlayerUiState,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSeekToFraction: (Float) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onRefreshFiles: () -> Unit,
    playPauseFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .background(AppleSecondaryBackground)
            .border(BorderStroke(1.dp, AppleSubtleBorder), RoundedCornerShape(26.dp))
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // =========================================================================
        // 1. TOP SECTION: Spacious Apple Notes Style Presentation Card
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AppleTertiaryBackground)
                            .border(BorderStroke(1.dp, AppleSubtleBorder), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = stringResource(R.string.quran_player_title),
                            tint = AppleTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.quran_player_title),
                        color = AppleTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status Badge & Quick Refresh
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (state.isPlaying) AppleQuaternaryBackground else AppleTertiaryBackground)
                            .border(
                                BorderStroke(0.8.dp, AppleSeparator),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                state.isBuffering -> "جار التحميل..."
                                state.isPlaying -> stringResource(R.string.now_playing)
                                track != null -> "متوقف مؤقتاً"
                                else -> "في الانتظار"
                            },
                            color = if (state.isPlaying) AppleTextPrimary else AppleTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Compact Refresh button in header
                    FocusableCapsuleButton(
                        onClick = onRefreshFiles,
                        icon = Icons.Default.Refresh,
                        contentDescription = "فحص التنزيلات",
                        buttonSize = 34.dp,
                        iconSize = 16.dp,
                        shape = CircleShape,
                        testTag = "btn_refresh_header"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Surah Display Hero Card (Apple Tertiary Background #2C2C2E)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppleTertiaryBackground)
                    .border(BorderStroke(1.dp, AppleSubtleBorder), RoundedCornerShape(22.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Decorative Quran Surah Emblem
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AppleQuaternaryBackground)
                            .border(BorderStroke(1.dp, AppleSeparator), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = AppleTextPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = AppleTextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Surah Name (Pure #F5F5F5 Apple Heading)
                    Text(
                        text = track?.surahNameArabic ?: stringResource(R.string.no_track_selected),
                        color = AppleTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Reciter Subtitle (Apple System Gray #8E8E93)
                    Text(
                        text = track?.reciterOrSubtitle ?: "اختر تلاوة من القائمة لبدء الاستماع",
                        color = AppleTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                    )

                    if (track != null && track.fileName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = track.fileName,
                            color = AppleTextTertiary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // =========================================================================
        // 2. BOTTOM SECTION: Minimalist Apple HIG Media Controls
        // =========================================================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // A) Progress Bar with Time Labels (#8E8E93 / #636366)
            val animatedProgress by animateFloatAsState(
                targetValue = state.progress,
                label = "player_progress"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Elapsed Duration (Left)
                Text(
                    text = AudioTrack.formatDuration(state.currentPositionMs),
                    color = AppleTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.Start
                )

                // Seekable Linear Progress Line with circular thumb
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .height(20.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeekToFraction(fraction)
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Inactive Background Track (#2C2C2E)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AppleProgressBg)
                    )

                    // Active Progress Fill (#F5F5F5)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AppleProgressFill)
                    )

                    // Progress Thumb Knob
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(BorderStroke(1.dp, AppleSeparator), CircleShape)
                        )
                    }
                }

                // Total Duration (Right)
                Text(
                    text = AudioTrack.formatDuration(state.durationMs),
                    color = AppleTextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.End
                )
            }

            // B) Single Balanced Horizontal Control Row
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Repeat Mode Toggle (Far Left)
                    val repeatIcon = when (state.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    FocusableCapsuleButton(
                        onClick = onCycleRepeat,
                        icon = repeatIcon,
                        contentDescription = when (state.repeatMode) {
                            RepeatMode.ALL -> stringResource(R.string.repeat_all)
                            RepeatMode.ONE -> stringResource(R.string.repeat_one)
                            RepeatMode.OFF -> stringResource(R.string.repeat_off)
                        },
                        isActiveToggle = state.repeatMode != RepeatMode.OFF,
                        buttonSize = 40.dp,
                        iconSize = 20.dp,
                        shape = CircleShape,
                        testTag = "btn_repeat"
                    )

                    // 2. Previous Track Disk Button
                    FocusableCapsuleButton(
                        onClick = onPlayPrevious,
                        icon = Icons.Default.SkipPrevious,
                        contentDescription = stringResource(R.string.previous),
                        isDiskStyle = true,
                        buttonSize = 48.dp,
                        iconSize = 24.dp,
                        shape = CircleShape,
                        testTag = "btn_prev"
                    )

                    // 3. Central Primary Play / Pause Disk (Apple Quaternary Elevated Surface #3A3A3C)
                    FocusableCapsuleButton(
                        onClick = onTogglePlayPause,
                        icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        isPrimary = true,
                        buttonSize = 64.dp,
                        iconSize = 34.dp,
                        shape = CircleShape,
                        focusRequester = playPauseFocusRequester,
                        testTag = "btn_play_pause"
                    )

                    // 4. Next Track Disk Button
                    FocusableCapsuleButton(
                        onClick = onPlayNext,
                        icon = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.next),
                        isDiskStyle = true,
                        buttonSize = 48.dp,
                        iconSize = 24.dp,
                        shape = CircleShape,
                        testTag = "btn_next"
                    )

                    // 5. Shuffle Toggle (Far Right)
                    FocusableCapsuleButton(
                        onClick = onToggleShuffle,
                        icon = Icons.Default.Shuffle,
                        contentDescription = "الوضع العشوائي",
                        isActiveToggle = state.isShuffle,
                        buttonSize = 40.dp,
                        iconSize = 20.dp,
                        shape = CircleShape,
                        testTag = "btn_shuffle"
                    )
                }
            }
        }
    }
}
