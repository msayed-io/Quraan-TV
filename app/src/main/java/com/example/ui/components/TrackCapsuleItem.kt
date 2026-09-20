package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AudioTrack
import com.example.ui.theme.AppleBaseBackground
import com.example.ui.theme.AppleFocusBorder
import com.example.ui.theme.AppleQuaternaryBackground
import com.example.ui.theme.AppleSeparator
import com.example.ui.theme.AppleSubtleBorder
import com.example.ui.theme.AppleTertiaryBackground
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.AppleTextTertiary

/**
 * Apple Fluid Interactive Track Capsule Item
 * Features:
 * 1. The Elastic Pinch: Scale down to 0.96 on press, bounce back on release
 * 2. Focus Glow & Specular Hairline: Elevated 2dp white focus outline on TV Remote focus
 * 3. Shared Element State Transition: Morphing audio equalizer & badge colors
 */
@Composable
fun TrackCapsuleItem(
    track: AudioTrack,
    isSelected: Boolean,
    isPlaying: Boolean,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. Elastic Pinch / Physical Spring Scale Animation for TV
    val targetScale = when {
        isPressed -> 0.96f // Elastic Pinch when user presses
        isFocused -> 1.04f // High-visibility TV Remote elevation
        else -> 1.0f
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "track_elastic_scale"
    )

    val shape = RoundedCornerShape(18.dp)

    // Animated Apple Elevation Background
    val targetBgColor = when {
        isFocused -> AppleQuaternaryBackground
        isSelected -> AppleQuaternaryBackground
        else -> AppleTertiaryBackground
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 200, easing = AppleFluidEasing),
        label = "track_bg_color"
    )

    // Specular Hairline & Glowing White Focus Ring (TV Optimizations)
    val border = when {
        isFocused -> BorderStroke(2.5.dp, Color.White)
        isSelected -> BorderStroke(1.dp, AppleSeparator)
        else -> BorderStroke(0.8.dp, AppleSubtleBorder)
    }

    val shadowElevation = if (isFocused) 8.dp else 0.dp

    Box(
        modifier = modifier
            .testTag("track_item_${track.id}")
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(shape)
            .background(backgroundColor)
            .border(border, shape)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_BUTTON_A -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 4. Smooth Morphing Badge (Index / Play / Equalizer Wave)
            val badgeBgColor by animateColorAsState(
                targetValue = if (isSelected) AppleTextPrimary else AppleQuaternaryBackground,
                animationSpec = tween(durationMillis = 250, easing = AppleFluidEasing),
                label = "badge_bg"
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeBgColor),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = Triple(isSelected, isPlaying, index),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180, easing = AppleFluidEasing)) togetherWith
                                fadeOut(animationSpec = tween(150, easing = AppleFluidEasing))
                    },
                    label = "badge_content"
                ) { (selected, playing, idx) ->
                    if (selected && playing) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Playing",
                            tint = AppleBaseBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    } else if (selected) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Selected",
                            tint = AppleBaseBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "${idx + 1}",
                            color = if (isFocused) AppleTextPrimary else AppleTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Surah Title & Reciter Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.surahNameArabic,
                    color = if (isSelected || isFocused) AppleTextPrimary else AppleTextPrimary.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                )

                Spacer(modifier = Modifier.width(2.dp))

                Text(
                    text = track.reciterOrSubtitle,
                    color = if (isFocused) AppleTextSecondary else AppleTextTertiary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
                )
            }

            // Duration badge
            if (track.durationMs > 0) {
                Text(
                    text = formatDuration(track.durationMs),
                    color = if (isFocused) AppleTextSecondary else AppleTextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
