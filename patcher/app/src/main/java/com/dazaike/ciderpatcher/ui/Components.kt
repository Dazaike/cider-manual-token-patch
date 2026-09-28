package com.dazaike.ciderpatcher.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ButtonStyle { Filled, Tonal, Outlined }

/** Squircle button that springs down while pressed. With [loading], its icon becomes a spinner and taps are ignored. */
@Composable
fun SquircleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: ButtonStyle = ButtonStyle.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "buttonPress",
    )
    val scaled = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    val content: @Composable () -> Unit = {
        if (icon != null || loading) {
            AnimatedContent(
                targetState = loading,
                transitionSpec = { (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                contentAlignment = Alignment.Center,
                label = "buttonIcon",
            ) { busy ->
                Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                            strokeCap = StrokeCap.Round,
                        )
                    } else if (icon != null) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
    val click = { if (!loading) onClick() }
    val padding = ButtonDefaults.ButtonWithIconContentPadding
    when (style) {
        ButtonStyle.Filled -> Button(click, scaled, enabled, PillSquircle, contentPadding = padding, interactionSource = interaction) { content() }
        ButtonStyle.Tonal -> FilledTonalButton(click, scaled, enabled, PillSquircle, contentPadding = padding, interactionSource = interaction) { content() }
        ButtonStyle.Outlined -> OutlinedButton(click, scaled, enabled, PillSquircle, contentPadding = padding, interactionSource = interaction) { content() }
    }
}

/** Small squircle-clipped icon button (40dp touch target). */
@Composable
fun SquircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(40.dp)
            .clip(SquircleShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Squircle section card, badged with a step [number] or an [icon], that animates its size as its content changes. */
@Composable
fun SectionCard(
    number: Int?,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            Modifier
                .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = SquircleShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            number != null -> Text("$number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            icon != null -> Icon(icon, null, Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

/** Horizontal shake, replayed whenever [trigger] changes to a non-null value. */
fun Modifier.shake(trigger: Any?): Modifier = composed {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        offset.snapTo(0f)
        if (trigger == null) return@LaunchedEffect
        offset.animateTo(0f, keyframes {
            durationMillis = 460
            -22f at 60
            18f at 130
            -12f at 200
            8f at 270
            -4f at 340
        })
    }
    graphicsLayer { translationX = offset.value }
}
