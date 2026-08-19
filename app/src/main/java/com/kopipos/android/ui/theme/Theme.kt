package com.kopipos.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KopiColors = lightColorScheme(
    primary = Color(0xFFE56B2F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF0E7),
    background = Color(0xFFF7F8FA),
    surface = Color.White,
    onSurface = Color(0xFF1D232A),
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFD9DEE5),
    outlineVariant = Color(0xFFE8EBEF),
    error = Color(0xFFC53D3D)
)

@Composable
fun KopiTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = KopiColors, content = content) }
