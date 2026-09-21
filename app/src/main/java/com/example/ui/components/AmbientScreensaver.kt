package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleProgressBg
import com.example.ui.theme.AppleProgressFill
import com.example.ui.theme.AppleTextSecondary
import kotlin.math.roundToInt

/**
 * Ambient OLED Screensaver to prevent pixel burn-in on Android TV.
 * Fades out main UI and floats minimal surah title & thin progress bar across dark screen.
 * Dismisses instantly on any D-Pad button press.
 */
@Composable
fun AmbientScreensaver(
    trackTitle: String,
    progress: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Slow drifting offset animation across screen
    val infiniteTransition = rememberInfiniteTransition(label = "screensaver_anim")
    val offsetXPercent by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset_x"
    )

    val offsetYPercent by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating minimal text & thin progress bar
        Column(
            modifier = Modifier
                .offset { IntOffset(offsetXPercent.roundToInt(), offsetYPercent.roundToInt()) }
                .width(220.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = trackTitle.ifBlank { "القرآن الكريم" },
                color = AppleTextSecondary.copy(alpha = 0.55f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.ContentOrRtl)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Very thin progress bar (1.5dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(AppleProgressBg.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(1.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(AppleProgressFill.copy(alpha = 0.6f))
                )
            }
        }
    }
}
