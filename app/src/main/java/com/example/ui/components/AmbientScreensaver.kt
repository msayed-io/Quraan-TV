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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Radical Apple-Style Ambient OLED Screensaver.
 * Displays an extra-large, ultra-bold platinum digital clock at screen center.
 * Features a smoothly blinking colon and 100% English numerals in 12-hour format.
 * Lightweight & optimized for 1GB RAM Android 9 TV.
 * Dismisses instantly on any D-Pad button press.
 */
@Composable
fun AmbientScreensaver(
    trackTitle: String = "",
    progress: Float = 0f,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Time state tracking (updated every second)
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = Date()
            delay(1000L)
        }
    }

    val hoursFormat = remember { SimpleDateFormat("h", Locale.ENGLISH) }
    val minutesFormat = remember { SimpleDateFormat("mm", Locale.ENGLISH) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.ENGLISH) }

    val hoursText = hoursFormat.format(currentTime)
    val minutesText = minutesFormat.format(currentTime)
    val amPmText = amPmFormat.format(currentTime)

    // Smooth infinite blinking transition for the clock colon
    val infiniteTransition = rememberInfiniteTransition(label = "colon_blink_transition")
    val colonAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colon_alpha"
    )

    val platinumWhite = Color(0xFFFAFAFA)

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
        // Center-aligned Apple Watch / iOS Lockscreen Digital Clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Hours
            Text(
                text = hoursText,
                color = platinumWhite,
                fontSize = 110.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )

            // Smoothly blinking colon :
            Text(
                text = ":",
                color = platinumWhite,
                fontSize = 105.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                modifier = Modifier.alpha(colonAlpha)
            )

            // Minutes
            Text(
                text = minutesText,
                color = platinumWhite,
                fontSize = 110.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            // AM / PM indicator
            Text(
                text = amPmText,
                color = platinumWhite.copy(alpha = 0.65f),
                fontSize = 32.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

