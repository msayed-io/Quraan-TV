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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.PopupProperties

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
    onShowQrDialog: () -> Unit = {},
    playPauseFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack
    var showSleepTimerOptions by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

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

                    // Status Badge, Sleep Timer Clock Icon & More Menu Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sleep Timer Clock Button with Floating Dropdown Menu
                        Box {
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

                            // Apple Floating Dropdown for Sleep Timer (Zero Layout Shift & Human Scale)
                            DropdownMenu(
                                expanded = showSleepTimerOptions,
                                onDismissRequest = { showSleepTimerOptions = false },
                                shape = RoundedCornerShape(14.dp),
                                containerColor = Color(0xFF1C1C1E),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                                tonalElevation = 0.dp,
                                shadowElevation = 16.dp,
                                modifier = Modifier
                                    .width(200.dp)
                                    .padding(2.dp),
                                properties = PopupProperties(focusable = true)
                            ) {
                                listOf(
                                    Pair(15, "15 دقيقة"),
                                    Pair(30, "30 دقيقة"),
                                    Pair(45, "45 دقيقة"),
                                    Pair(60, "ساعة كاملة")
                                ).forEach { (mins, labelStr) ->
                                    val isCurrent = state.sleepTimerMinutes == mins
                                    AppleDropdownMenuItem(
                                        label = labelStr,
                                        icon = if (isCurrent) Icons.Default.Check else Icons.Default.Timer,
                                        isActive = isCurrent,
                                        onClick = {
                                            showSleepTimerOptions = false
                                            onSetSleepTimer?.invoke(mins)
                                        }
                                    )
                                    AppleMenuDivider()
                                }

                                AppleDropdownMenuItem(
                                    label = "إيقاف المؤقت",
                                    icon = Icons.Default.Close,
                                    isActive = state.sleepTimerMinutes == null,
                                    onClick = {
                                        showSleepTimerOptions = false
                                        onSetSleepTimer?.invoke(null)
                                    }
                                )
                            }
                        }

                        // 3-dots More Options Button with Floating Dropdown Menu
                        Box {
                            FocusableCapsuleButton(
                                onClick = { showMoreMenu = !showMoreMenu },
                                icon = Icons.Default.MoreVert,
                                contentDescription = "خيارات إضافية",
                                isActiveToggle = showMoreMenu,
                                buttonSize = 34.dp,
                                iconSize = 16.dp,
                                shape = CircleShape,
                                testTag = "btn_player_more"
                            )

                            // Apple Floating Dropdown Menu (Absolute Overlay, Smooth 14dp Corners, 220dp Width)
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                shape = RoundedCornerShape(14.dp),
                                containerColor = Color(0xFF1C1C1E),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                                tonalElevation = 0.dp,
                                shadowElevation = 16.dp,
                                modifier = Modifier
                                    .width(220.dp)
                                    .padding(2.dp),
                                properties = PopupProperties(focusable = true)
                            ) {
                                // 1. Remote Control (QR)
                                AppleDropdownMenuItem(
                                    label = "التحكم عن بعد",
                                    icon = Icons.Default.PhoneAndroid,
                                    onClick = {
                                        showMoreMenu = false
                                        onShowQrDialog()
                                    }
                                )

                                AppleMenuDivider()

                                // 2. Night Audio Mode
                                AppleDropdownMenuItem(
                                    label = "الوضع الليلي",
                                    icon = Icons.Default.Bedtime,
                                    isActive = state.isNightMode,
                                    onClick = {
                                        showMoreMenu = false
                                        onToggleNightMode()
                                    }
                                )

                                AppleMenuDivider()

                                // 3. Refresh Files
                                AppleDropdownMenuItem(
                                    label = "تحديث الملفات",
                                    icon = Icons.Default.Refresh,
                                    onClick = {
                                        showMoreMenu = false
                                        onRefreshFiles()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Surah Display Hero Card (Apple Full-Bleed Artwork Card #2C2C2E)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppleTertiaryBackground)
                    .border(BorderStroke(1.dp, AppleSubtleBorder), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                val artwork = artworkBitmap

                // 1. Full-Bleed Artwork Cover Image / Ambient Backdrop
                if (artwork != null) {
                    Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = "غلاف المقطع",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback Ambient Islamic Geometric / Gradient Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF2C2C2E),
                                        Color(0xFF1C1C1E)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                // 2. Buffering Indicator Overlay (if active)
                if (state.isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = AppleTextPrimary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // 3. Top-End Night Mode Badge (if active)
                if (state.isNightMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = Color(0xFF0A84FF),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الوضع الليلي",
                                color = AppleTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 4. Compact Apple Scrim Gradient with Title and Reciter (Subtle Vignette)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp)
                        .padding(top = 8.dp, bottom = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = track?.title ?: stringResource(R.string.no_track_selected),
                            color = AppleTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                        )

                        val reciterDisplay = if (!track?.reciterName.isNullOrBlank()) {
                            track?.reciterName
                        } else if (!track?.surahNameArabic.isNullOrBlank()) {
                            track?.surahNameArabic
                        } else null

                        if (!reciterDisplay.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = reciterDisplay,
                                color = AppleTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                            )
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

/**
 * Apple-Standard Floating Dropdown Menu Item with magnetic alignment,
 * 44dp touch/D-pad target, single-line text with ellipsis, and distinct 24dp icon slot.
 */
@Composable
private fun AppleDropdownMenuItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) Color.White.copy(alpha = 0.16f)
                else Color.Transparent
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (isActive) Color(0xFF0A84FF) else AppleTextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isActive || isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
        )

        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color(0xFF0A84FF) else if (isFocused) AppleTextPrimary else AppleTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AppleMenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

