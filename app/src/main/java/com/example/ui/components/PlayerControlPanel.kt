package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.example.data.ArtworkExtractor
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Timer

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
    onToggleNightMode: () -> Unit = {},
    onSetSleepTimer: ((Int?) -> Unit)? = null,
    playPauseFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack
    var showSleepTimerOptions by remember { mutableStateOf(false) }

    // Asynchronous embedded artwork & video thumbnail extraction
    var artworkBitmap by remember(track?.filePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(track?.filePath) {
        val path = track?.filePath
        if (!path.isNullOrBlank()) {
            artworkBitmap = ArtworkExtractor.getArtwork(path)
        } else {
            artworkBitmap = null
        }
    }

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
            Column(modifier = Modifier.fillMaxWidth()) {
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

                    // Status Badge, Sleep Timer Clock Icon & Quick Refresh
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Night Audio Mode Button (Equalizer Vocal Booster)
                        FocusableCapsuleButton(
                            onClick = onToggleNightMode,
                            icon = Icons.Default.Bedtime,
                            contentDescription = if (state.isNightMode) "إيقاف وضع الاستماع الليلي" else "تفعيل وضع الاستماع الليلي (تحسين صوت القارئ)",
                            isActiveToggle = state.isNightMode,
                            buttonSize = 34.dp,
                            iconSize = 16.dp,
                            shape = CircleShape,
                            testTag = "btn_night_mode"
                        )

                        // Sleep Timer Clock Button
                        val hasTimer = state.sleepTimerRemainingSeconds > 0
                        val timerLabel = if (hasTimer) {
                            val m = state.sleepTimerRemainingSeconds / 60
                            val s = state.sleepTimerRemainingSeconds % 60
                            String.format("%02d:%02d", m, s)
                        } else null

                        FocusableCapsuleButton(
                            onClick = { showSleepTimerOptions = !showSleepTimerOptions },
                            icon = Icons.Default.Timer,
                            label = timerLabel,
                            contentDescription = "مؤقت النوم",
                            isActiveToggle = hasTimer,
                            buttonSize = 34.dp,
                            iconSize = 16.dp,
                            shape = CircleShape,
                            testTag = "btn_sleep_timer"
                        )

                        // Compact Refresh button in header
                        FocusableCapsuleButton(
                            onClick = onRefreshFiles,
                            icon = Icons.Default.Refresh,
                            contentDescription = "فحص التنزيلات",
                            buttonSize = 34.dp,
                            iconSize = 16.dp,
                            shape = CircleShape,
                            testTag = "btn_player_refresh"
                        )
                    }
                }

                // Sleep Timer Dropdown Options Row
                AnimatedVisibility(visible = showSleepTimerOptions) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleTertiaryBackground)
                            .border(BorderStroke(1.dp, AppleSeparator), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Pair(15, "15د"),
                                Pair(30, "30د"),
                                Pair(45, "45د"),
                                Pair(60, "1س")
                            ).forEach { (mins, labelStr) ->
                                FocusableCapsuleButton(
                                    onClick = {
                                        onSetSleepTimer?.invoke(mins)
                                        showSleepTimerOptions = false
                                    },
                                    label = labelStr,
                                    isActiveToggle = state.sleepTimerMinutes == mins,
                                    buttonSize = 32.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    testTag = "btn_timer_$mins"
                                )
                            }

                            FocusableCapsuleButton(
                                onClick = {
                                    onSetSleepTimer?.invoke(null)
                                    showSleepTimerOptions = false
                                },
                                label = "إيقاف",
                                isPrimary = state.sleepTimerMinutes == null,
                                buttonSize = 32.dp,
                                shape = RoundedCornerShape(12.dp),
                                testTag = "btn_timer_off"
                            )
                        }
                    }
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
                    // Embedded Album Art / Frame Box (Square card with downsampled artwork or fallback icon)
                    val artwork = artworkBitmap
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppleQuaternaryBackground)
                            .border(BorderStroke(1.dp, AppleSeparator), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = AppleTextPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else if (artwork != null) {
                            Image(
                                bitmap = artwork.asImageBitmap(),
                                contentDescription = "غلاف المقطع",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = AppleTextPrimary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real File Name (Pure #F5F5F5 Apple Heading)
                    Text(
                        text = track?.title ?: stringResource(R.string.no_track_selected),
                        color = AppleTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle (Format / Size / Folder)
                    Text(
                        text = track?.reciterOrSubtitle ?: "اختر ملفاً من القائمة لبدء الاستماع",
                        color = AppleTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                    )

                    if (state.isNightMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppleQuaternaryBackground)
                                .border(BorderStroke(1.dp, AppleSeparator), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = AppleTextPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "نمط ليلي محسن",
                                    color = AppleTextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
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
