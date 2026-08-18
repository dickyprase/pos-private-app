package com.kopipos.android.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable fun KopiTheme(content:@Composable()->Unit){MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFFE87932),secondary=Color(0xFF8B5E3C),surface=Color(0xFFFFF9F5)),content=content)}
