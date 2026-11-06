package com.language.finebulary.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.language.finebulary.data.pref.AppThemeColor
import com.language.finebulary.data.pref.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = CoffeePrimary,
    secondary = GoldSecondary,
    tertiary = SoftRose,
    background = DeepCoffeeBase,
    surface = SurfaceCharcoal,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = SilverText,
    onSurface = SilverText,
    onSurfaceVariant = SilverText.copy(alpha = 0.7f)
)

private val LightColorScheme = lightColorScheme(
    primary = WarmCoral,
    secondary = WarmCacao,
    tertiary = MutedSlate,
    background = CreamPaper,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = InkBlack,
    onSurface = InkBlack,
    onSurfaceVariant = MutedSlate
)

private val OceanColorScheme = lightColorScheme(
    primary = OceanPrimary,
    secondary = OceanSecondary,
    background = OceanBackground,
    surface = OceanSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF01579B),
    onSurface = Color(0xFF01579B)
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = OceanDarkPrimary,
    secondary = OceanDarkSecondary,
    background = OceanDarkBackground,
    surface = OceanDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE0F7FA),
    onSurface = Color(0xFFE0F7FA)
)

private val ForestColorScheme = lightColorScheme(
    primary = ForestPrimary,
    secondary = ForestSecondary,
    background = ForestBackground,
    surface = ForestSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1B5E20),
    onSurface = Color(0xFF1B5E20)
)

private val ForestDarkColorScheme = darkColorScheme(
    primary = ForestDarkPrimary,
    secondary = ForestDarkSecondary,
    background = ForestDarkBackground,
    surface = ForestDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE8F5E9),
    onSurface = Color(0xFFE8F5E9)
)

private val NightPlusColorScheme = darkColorScheme(
    primary = NightPlusPrimary,
    secondary = NightPlusSecondary,
    background = NightPlusBackground,
    surface = NightPlusSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = NightPlusText,
    onSurface = NightPlusText
)

@Composable
fun FinebularyTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    themeColor: AppThemeColor = AppThemeColor.WARM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.NIGHT -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        themeColor == AppThemeColor.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeColor == AppThemeColor.OCEAN -> if (darkTheme) OceanDarkColorScheme else OceanColorScheme
        themeColor == AppThemeColor.FOREST -> if (darkTheme) ForestDarkColorScheme else ForestColorScheme
        themeColor == AppThemeColor.NIGHT_PLUS -> NightPlusColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
