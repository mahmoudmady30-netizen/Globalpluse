package com.globalpulse.news.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GlobalPulseColorScheme = darkColorScheme(
    background = Ink,
    surface = Panel,
    primary = Gold,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border,
    error = Bearish
)

// Deliberately no darkTheme parameter: the dashboard's identity is a dark
// financial-terminal look, so this always applies one color scheme rather
// than also building/maintaining a light variant nobody asked for. If a
// light mode is wanted later, that's when this gains a parameter back.
@Composable
fun GlobalPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlobalPulseColorScheme,
        typography = GlobalPulseTypography,
        content = content
    )
}

