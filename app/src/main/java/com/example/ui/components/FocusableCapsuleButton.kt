package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleBaseBackground
import com.example.ui.theme.AppleFocusBorder
import com.example.ui.theme.AppleQuaternaryBackground
import com.example.ui.theme.AppleSecondaryBackground
import com.example.ui.theme.AppleSeparator
import com.example.ui.theme.AppleSubtleBorder
import com.example.ui.theme.AppleTertiaryBackground
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary

// Apple Fluid Easing Curve: cubic-bezier(0.25, 1, 0.5, 1)
val AppleFluidEasing = CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f)

/**
 * Apple HIG Fluid Interactive Button
 * Features:
 * 1. The Elastic Pinch (scale to 0.95 on press, elastic bounce back)
 * 2. Ambient Focus Glow Ring (for TV remote focus feedback)
 * 3. Specular Hairline Borders
 */
@Composable
fun FocusableCapsuleButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    label: String? = null,
    contentDescription: String? = null,
    iconSize: Dp = 20.dp,
    buttonSize: Dp = 46.dp,
    shape: Shape = CircleShape,
    isPrimary: Boolean = false,
    isActiveToggle: Boolean = false,
    isDiskStyle: Boolean = false,
    focusRequester: FocusRequester? = null,
    testTag: String = "capsule_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. Elastic Pinch / Physical Spring Scale Animation
    val targetScale = when {
        isPressed -> 0.94f // Elastic Pinch compression on touch/press
        isFocused -> if (isPrimary) 1.10f else 1.08f // TV Remote focus elevation
        else -> 1.0f
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "elastic_scale"
    )

    // Animated Background Color
    val targetBgColor = when {
        isFocused && isPrimary -> Color(0xFFFFFFFF)
        isFocused -> AppleQuaternaryBackground
        isPrimary -> AppleQuaternaryBackground
        isDiskStyle -> AppleTertiaryBackground
        isActiveToggle -> AppleTertiaryBackground
        else -> Color.Transparent
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 200, easing = AppleFluidEasing),
        label = "bg_color"
    )

    // Content Color
    val contentColor = when {
        isFocused && isPrimary -> AppleBaseBackground
        isFocused -> AppleTextPrimary
        isPrimary -> AppleTextPrimary
        isDiskStyle -> AppleTextPrimary
        isActiveToggle -> AppleTextPrimary
        else -> AppleTextSecondary
    }

    // 3. Specular Hairline & Glowing White Focus Ring (TV Optimizations)
    val border = when {
        isFocused -> BorderStroke(2.5.dp, Color.White)
        isPrimary -> BorderStroke(1.dp, AppleSeparator)
        isDiskStyle -> BorderStroke(1.dp, AppleSubtleBorder)
        isActiveToggle -> BorderStroke(1.dp, AppleSeparator)
        else -> BorderStroke(0.8.dp, Color.Transparent)
    }

    val shadowElevation = when {
        isFocused -> 8.dp
        isPrimary -> 4.dp
        else -> 0.dp
    }

    var mod = modifier
        .testTag(testTag)
        .scale(scale)
        .then(
            if (label == null) {
                Modifier.size(buttonSize)
            } else {
                Modifier.defaultMinSize(minWidth = 48.dp, minHeight = buttonSize)
            }
        )
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

    if (focusRequester != null) {
        mod = mod.focusRequester(focusRequester)
    }

    Box(
        modifier = mod.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        if (label == null) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = if (isFocused || isPrimary) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
