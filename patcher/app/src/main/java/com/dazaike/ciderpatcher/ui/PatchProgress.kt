package com.dazaike.ciderpatcher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dazaike.ciderpatcher.core.CiderPatcher.Step

private val Step.icon: ImageVector
    get() = when (this) {
        Step.READ -> Icons.Rounded.Description
        Step.RESOLVE -> Icons.AutoMirrored.Rounded.ManageSearch
        Step.ASSEMBLE -> Icons.Rounded.Construction
        Step.CHECK -> Icons.AutoMirrored.Rounded.FactCheck
        Step.WRITE -> Icons.Rounded.Archive
        Step.SIGN -> Icons.Rounded.Verified
    }

private val Step.title: String
    get() = when (this) {
        Step.READ -> "Reading the APK"
        Step.RESOLVE -> "Finding Cider's internals"
        Step.ASSEMBLE -> "Building the sign-in code"
        Step.CHECK -> "Checking every reference"
        Step.WRITE -> "Writing the patched APK"
        Step.SIGN -> "Signing"
    }

private enum class StepState { Pending, Active, Done }

/** Spinning orb, progress bar and step checklist shown while the patcher runs. */
@Composable
fun PatchProgress(step: Step?, symbols: Int) {
    val steps = Step.entries
    val index = step?.ordinal ?: -1
    val progress by animateFloatAsState(
        targetValue = ((index + 0.6f) / steps.size).coerceIn(0.03f, 1f),
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessVeryLow),
        label = "patchProgress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PatchOrb(step)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(PillSquircle),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                "Step ${(index + 1).coerceAtLeast(1)} of ${steps.size} · ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            steps.forEachIndexed { i, s ->
                val state = when {
                    i < index -> StepState.Done
                    i == index -> StepState.Active
                    else -> StepState.Pending
                }
                StaggeredIn(i) { StepRow(s, state, if (s == Step.RESOLVE) symbols else 0) }
            }
        }
    }
}

@Composable
private fun StaggeredIn(index: Int, content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(320, delayMillis = index * 70)) +
            slideInHorizontally(tween(420, delayMillis = index * 70, easing = FastOutSlowInEasing)) { it / 5 },
    ) { content() }
}

@Composable
private fun StepRow(step: Step, state: StepState, symbols: Int) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when (state) {
            StepState.Pending -> scheme.surfaceContainerHighest
            StepState.Active -> scheme.primaryContainer
            StepState.Done -> scheme.tertiaryContainer
        },
        tween(350),
        label = "stepContainer",
    )
    val content by animateColorAsState(
        when (state) {
            StepState.Pending -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
            StepState.Active -> scheme.onPrimaryContainer
            StepState.Done -> scheme.onTertiaryContainer
        },
        tween(350),
        label = "stepContent",
    )
    val rowScale by animateFloatAsState(
        if (state == StepState.Active) 1.03f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "stepScale",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .scale(rowScale)
            .clip(MaterialTheme.shapes.medium)
            .background(if (state == StepState.Active) scheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = SquircleShape(12.dp), color = container, contentColor = content, modifier = Modifier.size(36.dp)) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow)) + fadeIn()) togetherWith
                        (scaleOut(tween(120)) + fadeOut(tween(120)))
                },
                contentAlignment = Alignment.Center,
                label = "stepIcon",
            ) { s ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (s) {
                        StepState.Pending -> Icon(step.icon, null, Modifier.size(18.dp))
                        StepState.Active -> CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.5.dp,
                            color = content,
                            strokeCap = StrokeCap.Round,
                        )
                        StepState.Done -> Icon(Icons.Rounded.Check, null, Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                step.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (state == StepState.Active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (state == StepState.Pending) scheme.onSurfaceVariant.copy(alpha = 0.6f) else scheme.onSurface,
            )
            AnimatedVisibility(step == Step.RESOLVE && symbols > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Found ", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    AnimatedContent(
                        targetState = symbols,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                        },
                        label = "symbolCount",
                    ) { n ->
                        Text("$n", style = MaterialTheme.typography.labelMedium, color = scheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(" symbols", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Rotating gradient ring around a small squircle that shows the current step's icon. */
@Composable
private fun PatchOrb(step: Step?) {
    val scheme = MaterialTheme.colorScheme
    val infinite = rememberInfiniteTransition(label = "orb")
    val rotation by infinite.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "orbRotation",
    )
    val ringColors = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary.copy(alpha = 0f), scheme.primary)
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            rotate(rotation) {
                drawArc(
                    brush = Brush.sweepGradient(ringColors),
                    startAngle = 0f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                )
            }
        }
        Surface(
            shape = SquircleShape(24.dp),
            color = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            modifier = Modifier.size(64.dp),
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                contentAlignment = Alignment.Center,
                label = "orbIcon",
            ) { s ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon((s ?: Step.READ).icon, null, Modifier.size(28.dp))
                }
            }
        }
    }
}

/** Bouncy check mark with an expanding ring, shown when patching finishes. */
@Composable
fun SuccessBurst() {
    val scheme = MaterialTheme.colorScheme
    val ring = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        check.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        ring.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = scheme.tertiary.copy(alpha = (1f - ring.value) * 0.6f),
                    radius = size.minDimension / 2 * (0.5f + ring.value * 0.5f),
                    style = Stroke(width = 6.dp.toPx() * (1f - ring.value) + 1f),
                )
            }
            Surface(
                shape = SquircleShape(30.dp),
                color = scheme.tertiaryContainer,
                contentColor = scheme.onTertiaryContainer,
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer {
                        scaleX = check.value
                        scaleY = check.value
                        rotationZ = (1f - check.value) * -45f
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(40.dp))
                }
            }
        }
    }
}
