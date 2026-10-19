package com.example.huertolindo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
