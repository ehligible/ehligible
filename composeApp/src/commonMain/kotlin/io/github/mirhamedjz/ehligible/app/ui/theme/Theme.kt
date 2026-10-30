package io.github.mirhamedjz.ehligible.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Maple red, warm paper, a gold accent for celebrations and a sky blue for "trip mode". */
private val Light = lightColorScheme(
    primary = Color(0xFFD62828),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF9C4F00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF321200),
    tertiary = Color(0xFF2F5DA8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8E2FF),
    onTertiaryContainer = Color(0xFF001A41),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF0EE),
    surfaceContainer = Color(0xFFFCEAE7),
    surfaceContainerHigh = Color(0xFFF6E4E1),
    surfaceContainerHighest = Color(0xFFF0DEDC),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BF),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFFA4161A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB77C),
    onSecondary = Color(0xFF4F2500),
    secondaryContainer = Color(0xFF713700),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = Color(0xFFADC6FF),
    onTertiary = Color(0xFF002E69),
    tertiaryContainer = Color(0xFF0D4594),
    onTertiaryContainer = Color(0xFFD8E2FF),
    background = Color(0xFF1A1112),
    onBackground = Color(0xFFF1DEDC),
    surface = Color(0xFF1A1112),
    onSurface = Color(0xFFF1DEDC),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    surfaceContainerLowest = Color(0xFF140C0D),
    surfaceContainerLow = Color(0xFF231919),
    surfaceContainer = Color(0xFF271D1D),
    surfaceContainerHigh = Color(0xFF322827),
    surfaceContainerHighest = Color(0xFF3D3231),
    outline = Color(0xFFA08C8A),
    outlineVariant = Color(0xFF534341),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** Colours outside the Material scheme. */
@Immutable
data class BrandColors(
    /** The red of the filling leaf; stays vivid in dark mode. */
    val maple: Color,
    val leafEmpty: Color,
    val gold: Color,
    val confetti: List<Color>,
)

private val LightBrand = BrandColors(
    maple = Color(0xFFD62828),
    leafEmpty = Color(0xFFF6DCD8),
    gold = Color(0xFFF4A259),
    confetti = listOf(Color(0xFFD62828), Color(0xFFF4A259), Color(0xFF2F5DA8), Color(0xFF2A9D8F), Color(0xFFFFC8DD)),
)

private val DarkBrand = BrandColors(
    maple = Color(0xFFE5383B),
    leafEmpty = Color(0xFF3D2A2B),
    gold = Color(0xFFF4A259),
    confetti = listOf(Color(0xFFE5383B), Color(0xFFF4A259), Color(0xFF7FA7FF), Color(0xFF52C7B8), Color(0xFFFFC8DD)),
)

val LocalBrandColors = staticCompositionLocalOf { LightBrand }

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

private fun typography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, fontSize = 88.sp, lineHeight = 92.sp),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun EhligibleTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors: ColorScheme = if (darkTheme) Dark else Light
    CompositionLocalProvider(LocalBrandColors provides if (darkTheme) DarkBrand else LightBrand) {
        MaterialTheme(colorScheme = colors, typography = typography(), shapes = AppShapes, content = content)
    }
}

object Brand {
    val colors: BrandColors
        @Composable get() = LocalBrandColors.current
}

/** Number style for the big countdown, with tabular figures so digits don't jump. */
val CountdownNumberStyle: TextStyle
    @Composable get() = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum")
