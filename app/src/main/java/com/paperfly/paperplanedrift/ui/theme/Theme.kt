package com.paperfly.paperplanedrift.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.paperfly.paperplanedrift.R

// --- Craft Paper Minimal palette (design brief, style direction A) ---
object PaperColors {
    val Cream = Color(0xFFF6EFE2)        // background — kraft cream
    val Terracotta = Color(0xFFE2703A)   // primary accent: plane, CTAs, HUD
    val TerracottaShadow = Color(0xFFB85A2C)
    val Teal = Color(0xFF4F8C93)         // wind gusts only
    val Sage = Color(0xFF7E9A5B)         // success / milestones / equipped
    val Tan = Color(0xFFA38F76)          // shadows, obstacle shading, secondary text
    val Ink = Color(0xFF3A322A)          // text, outlines, linework
    val Disabled = Color(0xFFD8CDBA)     // locked tiles, inactive buttons
}

@OptIn(ExperimentalTextApi::class)
val BalooFamily = FontFamily(
    Font(R.font.baloo2, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.baloo2, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.baloo2, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.baloo2, weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

@OptIn(ExperimentalTextApi::class)
val KarlaFamily = FontFamily(
    Font(R.font.karla, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.karla, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.karla, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val PaperColorScheme = lightColorScheme(
    primary = PaperColors.Terracotta,
    onPrimary = PaperColors.Cream,
    secondary = PaperColors.Teal,
    onSecondary = PaperColors.Cream,
    tertiary = PaperColors.Sage,
    background = PaperColors.Cream,
    onBackground = PaperColors.Ink,
    surface = PaperColors.Cream,
    onSurface = PaperColors.Ink,
    surfaceVariant = PaperColors.Disabled,
    onSurfaceVariant = PaperColors.Tan,
    outline = PaperColors.Ink,
)

// Type scale from the brief: Baloo 2 for display/headers, Karla for body/UI.
private val PaperTypography = Typography(
    displayLarge = TextStyle(fontFamily = BalooFamily, fontWeight = FontWeight.Bold, fontSize = 48.sp),
    headlineLarge = TextStyle(fontFamily = BalooFamily, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp),
    headlineMedium = TextStyle(fontFamily = BalooFamily, fontWeight = FontWeight.SemiBold, fontSize = 36.sp),
    headlineSmall = TextStyle(fontFamily = BalooFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = BalooFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = KarlaFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = BalooFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelLarge = TextStyle(fontFamily = KarlaFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = KarlaFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp),
    bodyMedium = TextStyle(fontFamily = KarlaFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodySmall = TextStyle(fontFamily = KarlaFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp),
)

@Composable
fun PaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaperColorScheme,
        typography = PaperTypography,
        content = content,
    )
}
