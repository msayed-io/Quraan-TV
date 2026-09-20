package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleSecondaryBackground
import com.example.ui.theme.AppleTextPrimary

/**
 * Apple HIG Floating Capsule with Specular Hairline Border & Ambient Shadow
 * Inspired by Apple Notes & Liquid Glass elevation guidelines.
 */
@Composable
fun AppleFloatingCapsule(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = AppleTextPrimary,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontSize: Int = 13
) {
    val capsuleShape = RoundedCornerShape(9999.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = capsuleShape,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(capsuleShape)
            .background(AppleSecondaryBackground) // #1C1C1E
            .border(
                border = BorderStroke(0.6.dp, Color(0x1FFFFFFF)), // Specular Hairline (border-white/8)
                shape = capsuleShape
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            letterSpacing = 0.2.sp
        )
    }
}
