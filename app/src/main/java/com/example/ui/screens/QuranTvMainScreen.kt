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
import androidx.compose.foundation.shape.CircleShape
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

import android.app.Activity
import android.view.KeyEvent
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.ImeAction
import com.example.ui.components.AmbientScreensaver
import com.example.viewmodel.ViewMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun QuranTvMainScreen(
    viewModel: QuranPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playPauseFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

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

    // Inactivity Ambient Screensaver Monitor (5 Minutes during playback)
    LaunchedEffect(uiState.isPlaying) {
        while (isActive) {
            delay(10000L) // Check every 10 seconds
            val now = System.currentTimeMillis()
            if (uiState.isPlaying && (now - lastInteractionTime >= 300_000L) && !uiState.isScreensaverActive) {
                viewModel.setScreensaverActive(true)
            }
        }
    }

    // Filter tracks based on ViewMode & Search Query
    val filteredTracks = remember(uiState.tracks, uiState.favorites, uiState.currentViewMode, uiState.searchQuery) {
        var list = uiState.tracks
        if (uiState.currentViewMode == ViewMode.FAVORITES) {
            list = list.filter { uiState.favorites.contains(it.filePath) }
        }
        if (uiState.searchQuery.isNotBlank()) {
            val q = uiState.searchQuery.trim()
            list = list.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.surahNameArabic.contains(q, ignoreCase = true) ||
                it.fileName.contains(q, ignoreCase = true)
            }
        }
        list
    }

    // RTL Layout Direction for Quran TV interface
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppleBaseBackground)
                .onKeyEvent {
                    lastInteractionTime = System.currentTimeMillis()
                    if (uiState.isScreensaverActive) {
                        viewModel.setScreensaverActive(false)
                        true
                    } else {
                        false
                    }
                }
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
                    // 1. RIGHT SIDE: Floating Apple Capsules, Header & Tracklist
                    // =========================================================================
                    Box(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                    ) {
                        // Tracklist body
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
                        } else if (filteredTracks.isEmpty()) {
                            // Empty state
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
                                        text = if (uiState.currentViewMode == ViewMode.FAVORITES) "لا توجد تلاوات في المفضلة"
                                               else if (uiState.searchQuery.isNotBlank()) "لا توجد نتائج مطابقة للبحث"
                                               else stringResource(R.string.no_audio_files),
                                        color = AppleTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    if (uiState.currentViewMode == ViewMode.FAVORITES) {
                                        FocusableCapsuleButton(
                                            onClick = { viewModel.setViewMode(ViewMode.ALL) },
                                            icon = Icons.Default.Close,
                                            label = "العودة للتلاوات العامة",
                                            isPrimary = true,
                                            testTag = "btn_empty_fav_back"
                                        )
                                    } else {
                                        FocusableCapsuleButton(
                                            onClick = { viewModel.loadAudioFiles() },
                                            icon = Icons.Default.Refresh,
                                            label = stringResource(R.string.scan_storage),
                                            isDiskStyle = true,
                                            testTag = "btn_empty_refresh"
                                        )
                                    }
                                }
                            }
                        } else {
                            // Scrollable list passing underneath floating header
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(top = 62.dp, bottom = 28.dp)
                            ) {
                                itemsIndexed(
                                    items = filteredTracks,
                                    key = { _, track -> track.id }
                                ) { index, track ->
                                    TrackCapsuleItem(
                                        track = track,
                                        isSelected = uiState.currentTrack?.id == track.id,
                                        isPlaying = uiState.isPlaying && uiState.currentTrack?.id == track.id,
                                        index = index,
                                        isFavorite = uiState.favorites.contains(track.filePath),
                                        onClick = { viewModel.selectAndPlayTrack(track) },
                                        onToggleFavorite = { viewModel.toggleFavorite(track) }
                                    )
                                }
                            }
                        }

                        // Top Gradient Blur
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

                        // Bottom Gradient Vignette
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

                        // Floating Header System
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = uiState.isSearchActive,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                // Full-width Capsule Search Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(AppleTertiaryBackground)
                                        .border(BorderStroke(1.5.dp, Color.White), RoundedCornerShape(24.dp))
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "البحث",
                                                tint = AppleTextPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            BasicTextField(
                                                value = uiState.searchQuery,
                                                onValueChange = { viewModel.setSearchQuery(it) },
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    color = AppleTextPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    textDirection = TextDirection.ContentOrRtl
                                                ),
                                                cursorBrush = SolidColor(AppleTextPrimary),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                keyboardActions = KeyboardActions(onSearch = { }),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .focusRequester(searchFocusRequester),
                                                decorationBox = { innerTextField ->
                                                    if (uiState.searchQuery.isEmpty()) {
                                                        Text(
                                                            text = "ابحث عن سورة أو قارئ...",
                                                            color = AppleTextSecondary,
                                                            fontSize = 14.sp
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                        }

                                        FocusableCapsuleButton(
                                            onClick = { viewModel.toggleSearchActive(false) },
                                            icon = Icons.Default.Close,
                                            contentDescription = "إغلاق البحث",
                                            buttonSize = 32.dp,
                                            iconSize = 18.dp,
                                            shape = CircleShape,
                                            testTag = "btn_close_search"
                                        )
                                    }
                                }

                                LaunchedEffect(Unit) {
                                    delay(100L)
                                    searchFocusRequester.requestFocus()
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = !uiState.isSearchActive,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Title Capsule (التلاوات or المفضلة - Pure Title Only)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(AppleTertiaryBackground)
                                            .border(BorderStroke(1.dp, AppleSubtleBorder), RoundedCornerShape(24.dp))
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.currentViewMode == ViewMode.FAVORITES) "المفضلة" else "التلاوات",
                                            color = AppleTextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Standalone Opposite Floating Capsule (Search & Favorites Icons in ViewMode.ALL, or Circular Exit Capsule in ViewMode.FAVORITES)
                                    if (uiState.currentViewMode == ViewMode.ALL) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(AppleTertiaryBackground)
                                                .border(BorderStroke(1.dp, AppleSubtleBorder), RoundedCornerShape(24.dp))
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // 1. Search Icon Button (Swapped to first position)
                                                FocusableCapsuleButton(
                                                    onClick = { viewModel.toggleSearchActive(true) },
                                                    icon = Icons.Default.Search,
                                                    contentDescription = "البحث",
                                                    buttonSize = 32.dp,
                                                    iconSize = 18.dp,
                                                    shape = CircleShape,
                                                    testTag = "btn_header_search"
                                                )

                                                // 2. Favorites Heart Icon Button (Swapped to second position)
                                                FocusableCapsuleButton(
                                                    onClick = { viewModel.setViewMode(ViewMode.FAVORITES) },
                                                    icon = if (uiState.favorites.isNotEmpty()) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "عرض المفضلة",
                                                    buttonSize = 32.dp,
                                                    iconSize = 18.dp,
                                                    shape = CircleShape,
                                                    testTag = "btn_header_fav"
                                                )
                                            }
                                        }
                                    } else {
                                        // Circular Floating Exit Capsule Button (Icon-only, no text label)
                                        FocusableCapsuleButton(
                                            onClick = { viewModel.setViewMode(ViewMode.ALL) },
                                            icon = Icons.Default.Close,
                                            contentDescription = "العودة للتلاوات العامة",
                                            isPrimary = true,
                                            buttonSize = 38.dp,
                                            iconSize = 20.dp,
                                            shape = CircleShape,
                                            testTag = "btn_exit_fav"
                                        )
                                    }
                                }
                            }
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
                        onSetSleepTimer = { mins ->
                            viewModel.setSleepTimer(mins) {
                                (context as? Activity)?.finish()
                            }
                        },
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

                // Error Message Bar
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

            // Full Overlay Ambient Screensaver
            AnimatedVisibility(
                visible = uiState.isScreensaverActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AmbientScreensaver(
                    trackTitle = uiState.currentTrack?.title ?: "",
                    progress = uiState.progress,
                    onDismiss = { viewModel.setScreensaverActive(false) }
                )
            }
        }
    }
}
