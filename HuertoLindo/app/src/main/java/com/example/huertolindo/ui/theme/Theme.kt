package com.example.huertolindo.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Color(0xFF16351F),
    primaryContainer = Color(0xFF294B34),
    onPrimaryContainer = Color(0xFFD4E8D8),
    secondary = GreenGrey80,
    secondaryContainer = Color(0xFF35483A),
    onSecondaryContainer = Color(0xFFD8E5DA),
    tertiary = Lime80,
    tertiaryContainer = Color(0xFF4D512F),
    onTertiaryContainer = Color(0xFFE2EAC5),
    errorContainer = Color(0xFF5C2828),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111713),
    surface = Color(0xFF1C241F),
    onBackground = Color(0xFFF7FAF7),
    onSurface = Color(0xFFF7FAF7)
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7EBDD),
    onPrimaryContainer = Color(0xFF183923),
    secondary = GreenGrey40,
    secondaryContainer = Color(0xFFDDE8DE),
    onSecondaryContainer = Color(0xFF26392B),
    tertiary = Lime40,
    tertiaryContainer = Color(0xFFEAF0D3),
    onTertiaryContainer = Color(0xFF343D1B),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7FAF7),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF171B18),
    onSurface = Color(0xFF171B18)
)

@Composable
fun HuertoLindoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
