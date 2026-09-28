package com.dazaike.ciderpatcher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Every Material container shape (fields, cards, dialogs, sheets) is a squircle. */
private val SquircleShapes = Shapes(
    extraSmall = SquircleShape(12.dp),
    small = SquircleShape(16.dp),
    medium = SquircleShape(24.dp),
    large = SquircleShape(30.dp),
    extraLarge = SquircleShape(36.dp),
)

/** Buttons, chips and other pill-shaped controls. */
val PillSquircle = SquircleShape(100.dp)

@Composable
fun CiderPatcherTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    MaterialTheme(colorScheme = scheme, shapes = SquircleShapes, content = content)
}
