package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class ScholarlyExtendedColors(
  val verifiedBadgeBg: Color,
  val verifiedBadgeFg: Color,
  val ratingStar: Color,
  val heroGradientStart: Color,
  val heroGradientEnd: Color,
  val cardElevated: Color,
)

private val LightExtendedColors = ScholarlyExtendedColors(
  verifiedBadgeBg = EmeraldWashContainer,
  verifiedBadgeFg = VerifiedEmerald,
  ratingStar = Color(0xFFD97706),
  heroGradientStart = Color(0xFF1E3A5F),
  heroGradientEnd = Color(0xFF2D5382),
  cardElevated = LinenSurface,
)

private val DarkExtendedColors = ScholarlyExtendedColors(
  verifiedBadgeBg = TealDarkContainer,
  verifiedBadgeFg = BrightTeal,
  ratingStar = LuminousGold,
  heroGradientStart = Color(0xFF13253E),
  heroGradientEnd = Color(0xFF1E3A5F),
  cardElevated = ElevatedCharcoalSurface,
)

val LocalScholarlyColors = staticCompositionLocalOf { LightExtendedColors }

private val DarkColorScheme = darkColorScheme(
  primary = MidnightIndigo,
  onPrimary = OnMidnightIndigo,
  primaryContainer = MidnightIndigoContainer,
  onPrimaryContainer = OnMidnightIndigoContainer,
  secondary = LuminousGold,
  onSecondary = OnLuminousGold,
  secondaryContainer = GoldDarkContainer,
  onSecondaryContainer = OnGoldDarkContainer,
  tertiary = BrightTeal,
  onTertiary = OnBrightTeal,
  tertiaryContainer = TealDarkContainer,
  onTertiaryContainer = OnTealDarkContainer,
  background = MidnightSlateBackground,
  onBackground = ParchmentOnMidnight,
  surface = ElevatedCharcoalSurface,
  onSurface = ParchmentOnMidnight,
  surfaceVariant = CharcoalSurfaceVariant,
  onSurfaceVariant = MutedSilverText,
  outline = DarkOutline,
  outlineVariant = DarkOutlineVariant,
)

private val LightColorScheme = lightColorScheme(
  primary = AcademicIndigo,
  onPrimary = OnAcademicIndigo,
  primaryContainer = IndigoMistContainer,
  onPrimaryContainer = OnIndigoMistContainer,
  secondary = ScholarlyAmber,
  onSecondary = OnScholarlyAmber,
  secondaryContainer = AmberWashContainer,
  onSecondaryContainer = OnAmberWashContainer,
  tertiary = VerifiedEmerald,
  onTertiary = OnVerifiedEmerald,
  tertiaryContainer = EmeraldWashContainer,
  onTertiaryContainer = OnEmeraldWashContainer,
  background = ParchmentBackground,
  onBackground = InkOnParchment,
  surface = LinenSurface,
  onSurface = InkOnParchment,
  surfaceVariant = WarmStoneSurfaceVariant,
  onSurfaceVariant = SlateMuted,
  outline = HairlineOutline,
  outlineVariant = SubtleOutlineVariant,
  error = CrimsonError,
  errorContainer = ErrorWashContainer,
  onErrorContainer = OnErrorWashContainer,
)

val ScholarlyShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ScholarlyTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Warm Academic Atelier brand palette by default; dynamic color optional
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

  CompositionLocalProvider(LocalScholarlyColors provides extended) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = ScholarlyTypography,
      shapes = ScholarlyShapes,
      content = content,
    )
  }
}
