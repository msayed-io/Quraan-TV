package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Apple HIG Pure Dark Theme (System Backgrounds & Typography)
// =========================================================================

// 1. Base & Surfaces (Apple System Gray Elevation Philosophy)
val AppleBaseBackground = Color(0xFF000000)       // Pure Black (#000000) - Base OLED
val AppleSecondaryBackground = Color(0xFF1C1C1E)  // Secondary Background (#1C1C1E) - Cards, Panels
val AppleTertiaryBackground = Color(0xFF2C2C2E)   // Tertiary Background (#2C2C2E) - Floating Elements, Capsules
val AppleQuaternaryBackground = Color(0xFF3A3A3C) // Quaternary (#3A3A3C) - Elevated Disks, Active State

// 2. Apple System Borders & Separators
val AppleSeparator = Color(0xFF38383A)            // System Separator (#38383A)
val AppleSubtleBorder = Color(0xFF2C2C2E)         // Subtle Border (#2C2C2E)
val AppleFocusBorder = Color(0xFFFFFFFF)          // Crisp Focus Ring (#FFFFFF)
val AppleFocusGlow = Color(0xFF3A3A3C)            // Focused Surface Highlight

// 3. Apple System Typography (Comfortable AAA Contrast)
val AppleTextPrimary = Color(0xFFF5F5F5)          // Primary Text (#F5F5F5 / #FFFFFF)
val AppleTextSecondary = Color(0xFF8E8E93)        // System Gray (#8E8E93) - Subtitles, Durations
val AppleTextTertiary = Color(0xFF636366)         // System Gray 2 (#636366) - Filenames, Footnotes

// 4. Player & Progress Bar Elements
val AppleProgressBg = Color(0xFF2C2C2E)           // Inactive Progress Track (#2C2C2E)
val AppleProgressFill = Color(0xFFF5F5F5)         // Active Progress Fill (#F5F5F5)

// Aliases for compatibility with existing components
val QuranBgBlack = AppleBaseBackground
val QuranSurfaceDark = AppleSecondaryBackground
val QuranSurfaceCard = AppleSecondaryBackground
val QuranGlassBg = AppleTertiaryBackground
val QuranGlassBorder = AppleSubtleBorder
val QuranFocusGlassBg = AppleQuaternaryBackground
val QuranFocusBorder = AppleFocusBorder

val QuranTextPrimary = AppleTextPrimary
val QuranTextSecondary = AppleTextSecondary
val QuranTextTertiary = AppleTextTertiary
val QuranActivePlatinum = AppleTextPrimary
val QuranDivider = AppleSeparator
val QuranProgressBg = AppleProgressBg
val QuranProgressFill = AppleProgressFill
