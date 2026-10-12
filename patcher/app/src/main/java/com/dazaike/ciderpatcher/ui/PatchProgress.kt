package com.dazaike.ciderpatcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dazaike.ciderpatcher.core.CiderPatcher.Step
import com.dazaike.ciderpatcher.ui.theme.LocalMotion
import com.dazaike.ciderpatcher.ui.theme.Prism
import com.dazaike.ciderpatcher.ui.theme.PrismText

private val Step.title: String
    get() = when (this) {
        Step.READ -> "Reading the APK"
        Step.RESOLVE -> "Finding Cider's internals"
        Step.ASSEMBLE -> "Building the sign-in code"
        Step.HOOK -> "Hooking Sid to the cloud"
        Step.CHECK -> "Checking every reference"
        Step.WRITE -> "Writing the patched APK"
        Step.SIGN -> "Signing"
    }

/** Progress bar and step checklist shown while the patcher runs. */
@Composable
fun PatchProgress(step: Step?, symbols: Int) {
    val colors = Prism.colors
    val motion = LocalMotion.current
    val steps = Step.entries
    val index = step?.ordinal ?: -1
    val progress by animateFloatAsState(
        targetValue = ((index + 0.6f) / steps.size).coerceIn(0.03f, 1f),
        animationSpec = motion.glide(),
        label = "patchProgress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassProgressBar(progress)
        PrismText(
            "Step ${(index + 1).coerceAtLeast(1)} of ${steps.size} · ${(progress * 100).toInt()}%",
            fontSize = 13.sp,
            color = colors.subText,
        )
        Column {
            steps.forEachIndexed { i, s ->
                Row(
                    Modifier.heightIn(min = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        when {
                            i < index -> PrismIcon(PrismIcons.Check, null, size = 18.dp, tint = Prism.accent)
                            i == index -> Spinner(size = 18.dp, color = Prism.accent)
                            else -> Box(Modifier.size(8.dp).background(colors.fill, CircleShape))
                        }
                    }
                    PrismText(
                        s.title,
                        fontSize = 15.sp,
                        fontWeight = if (i == index) FontWeight.Medium else FontWeight.Normal,
                        color = if (i <= index) colors.text else colors.subText,
                    )
                    if (s == Step.RESOLVE && symbols > 0) {
                        PrismText("· $symbols symbols", fontSize = 13.sp, color = colors.subText)
                    }
                }
            }
        }
    }
}
