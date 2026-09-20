package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.components.AppleFloatingCapsule
import com.example.ui.components.FocusableCapsuleButton
import com.example.ui.components.PlayerControlPanel
import com.example.ui.components.TrackCapsuleItem
import com.example.ui.theme.AppleBaseBackground
import com.example.ui.theme.AppleSeparator
import com.example.ui.theme.AppleSubtleBorder
import com.example.ui.theme.AppleTertiaryBackground
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.viewmodel.QuranPlayerViewModel

@Composable
fun QuranTvMainScreen(
    viewModel: QuranPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playPauseFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Permission launcher for Storage
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permissionToCheck
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.onPermissionResult(isGranted)

        if (!isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    // RTL Layout Direction for Quran TV interface
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppleBaseBackground)
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main TV 2-Panel Content Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // =========================================================================
                    // 1. RIGHT SIDE: Floating Apple Capsules & Magnetic Dissolve Tracklist
                    // =========================================================================
                    Box(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                    ) {
                        // Tracklist body (Scrolls smoothly underneath the floating top capsules)
                        if (uiState.isLoadingFiles) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = AppleTextPrimary,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "جار فحص ملفات القرآن...",
                                        color = AppleTextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else if (uiState.tracks.isEmpty()) {
                            // Empty state: exact required text
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 64.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(AppleTertiaryBackground)
                                    .border(BorderStroke(1.dp, AppleSubtleBorder), RoundedCornerShape(20.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = AppleTextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = stringResource(R.string.no_audio_files),
                                        color = AppleTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    FocusableCapsuleButton(
                                        onClick = { viewModel.loadAudioFiles() },
                                        icon = Icons.Default.Refresh,
                                        label = stringResource(R.string.scan_storage),
                                        isDiskStyle = true,
                                        testTag = "btn_empty_refresh"
                                    )
                                }
                            }
                        } else {
                            // Scrollable list passing underneath the floating capsules
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 58.dp, bottom = 28.dp)
                            ) {
                                itemsIndexed(
                                    items = uiState.tracks,
                                    key = { _, track -> track.id }
                                ) { index, track ->
                                    TrackCapsuleItem(
                                        track = track,
                                        isSelected = uiState.currentTrack?.id == track.id,
                                        isPlaying = uiState.isPlaying && uiState.currentTrack?.id == track.id,
                                        index = index,
                                        onClick = { viewModel.selectAndPlayTrack(track) }
                                    )
                                }
                            }
                        }

                        // A) Apple Top Magnetic Blur & Vignette Backdrop (Gradual soft fade from solid black to transparent)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(74.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            AppleBaseBackground,
                                            AppleBaseBackground.copy(alpha = 0.90f),
                                            AppleBaseBackground.copy(alpha = 0.40f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // B) Apple Bottom Magnetic Vignette Backdrop (Gentle dissolve as items reach bottom)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            AppleBaseBackground.copy(alpha = 0.65f),
                                            AppleBaseBackground
                                        )
                                    )
                                )
                        )

                        // C) Floating Pinned Header Capsules (Pure Apple HIG Floating System)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Right Floating Capsule: Title only (No icons, no bulky descriptions)
                            AppleFloatingCapsule(
                                text = "التلاوات",
                                fontSize = 14,
                                fontWeight = FontWeight.Bold
                            )

                            // Left Floating Capsule: Count badge only
                            AppleFloatingCapsule(
                                text = "${uiState.tracks.size} تلاوة",
                                fontSize = 12,
                                fontWeight = FontWeight.Medium,
                                textColor = AppleTextSecondary
                            )
                        }
                    }

                    // =========================================================================
                    // 2. LEFT SIDE: Apple Media Control Player Panel
                    // =========================================================================
                    PlayerControlPanel(
                        state = uiState,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onPlayNext = { viewModel.playNext() },
                        onPlayPrevious = { viewModel.playPrevious() },
                        onSeekToFraction = { frac -> viewModel.seekToFraction(frac) },
                        onCycleRepeat = { viewModel.cycleRepeatMode() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onRefreshFiles = { viewModel.loadAudioFiles() },
                        playPauseFocusRequester = playPauseFocusRequester,
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
                    )
                }

                // Storage Permission Notice
                if (!uiState.hasStoragePermission) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppleTertiaryBackground)
                            .border(BorderStroke(1.dp, AppleSeparator), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AppleTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.storage_permission_required),
                                    color = AppleTextPrimary,
                                    fontSize = 13.sp
                                )
                            }
                            FocusableCapsuleButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
                                    } else {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                                    }
                                },
                                label = stringResource(R.string.grant_permission),
                                isPrimary = true,
                                testTag = "btn_grant_permission"
                            )
                        }
                    }
                }

                // Error Message Bar (Apple HIG Style)
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppleTertiaryBackground)
                            .border(BorderStroke(1.dp, AppleSeparator), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = AppleTextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                            )
                            IconButton(
                                onClick = { viewModel.clearErrorMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = AppleTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
