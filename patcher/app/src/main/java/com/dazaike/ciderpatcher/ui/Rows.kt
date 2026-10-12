package com.dazaike.ciderpatcher.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dazaike.ciderpatcher.ui.theme.LocalMotion
import com.dazaike.ciderpatcher.ui.theme.Prism
import com.dazaike.ciderpatcher.ui.theme.PrismText
import com.dazaike.ciderpatcher.ui.theme.accentAlpha
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import kotlin.math.max

private val RowShape = RoundedRectangle(14.dp)

@Composable
fun SectionLabel(text: String) {
    PrismText(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Prism.colors.subText)
}

/** Flat tappable row: immediate press dip lighting, no ripple. */
@Composable
fun GhostRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes trailingIcon: Int = PrismIcons.ChevronRight,
) {
    val press = rememberPressState(enabled)
    val colors = Prism.colors
    val haptics = LocalHaptics.current
    Row(
        modifier
            .focusRing(press, RowShape, Prism.accent.copy(alpha = 0.85f))
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .drawBehind {
                val a = max(press.hover, press.progress * 1.6f)
                if (a > 0f) {
                    drawOutline(
                        outline = RowShape.createOutline(size, layoutDirection, this),
                        color = colors.fillWeak.copy(alpha = (colors.fillWeak.alpha * a).coerceAtMost(1f)),
                    )
                }
            }
            .pressInput(press, enabled)
            .clickable(
                interactionSource = press.interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                haptics.perform(HapticKind.Press)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            PrismText(title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                PrismText(
                    subtitle,
                    fontSize = 13.sp,
                    color = colors.subText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        PrismIcon(trailingIcon, null, size = 16.dp, tint = colors.subText)
    }
}

private val RailLabels = listOf("Patch", "Install", "Sign in")

/** Three-node progress rail: Patch, Install, Sign in. [current] is the step being worked on. */
@Composable
fun JourneyRail(current: Int, allDone: Boolean, modifier: Modifier = Modifier) {
    val colors = Prism.colors
    val accent = Prism.accent
    val motion = LocalMotion.current
    val doneColor = accent.copy(alpha = colors.accentAlpha(0.85f))
    val count = RailLabels.size
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val y = 14.dp.toPx()
                for (i in 0 until count - 1) {
                    val color = if (allDone || i < current) doneColor else colors.fill
                    drawLine(
                        color = color,
                        start = Offset(size.width * (i + 0.5f) / count, y),
                        end = Offset(size.width * (i + 1.5f) / count, y),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            },
    ) {
        RailLabels.forEachIndexed { i, label ->
            val done = allDone || i < current
            val active = !done && i == current
            val fill by animateColorAsState(
                if (done) doneColor else colors.fill,
                motion.fade(220),
                label = "railFill",
            )
            val textColor by animateColorAsState(
                if (i <= current || allDone) colors.text else colors.subText,
                motion.fade(220),
                label = "railText",
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(fill, CircleShape)
                        .then(if (active) Modifier.border(2.dp, accent, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        PrismIcon(PrismIcons.Check, null, size = 16.dp, tint = Prism.onAccent)
                    } else {
                        PrismText(
                            "${i + 1}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) colors.text else colors.subText,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                PrismText(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
            }
        }
    }
}

/** A [TextFieldState] mirroring a plain string held elsewhere (the view model). */
@Composable
fun rememberSyncedFieldState(value: String, onValueChange: (String) -> Unit): TextFieldState {
    val state = rememberTextFieldState(value)
    val currentChange by rememberUpdatedState(onValueChange)
    // The last text known to both sides; stops a fast keystroke being overwritten by a stale echo.
    val last = remember { arrayOf(value) }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect {
            if (it != last[0]) {
                last[0] = it
                currentChange(it)
            }
        }
    }
    LaunchedEffect(value) {
        if (value != last[0]) {
            last[0] = value
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }
    return state
}

/** Password field with a Copy button underneath; the clip is flagged sensitive. */
@Composable
fun SecretField(backdrop: Backdrop, label: String, state: TextFieldState) {
    val context = LocalContext.current
    val toasts = LocalToasts.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GlassPasswordField(state, label, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            GlassButton(
                backdrop,
                "Copy",
                {
                    copySensitive(context, label, state.text.toString().trim())
                    toasts.show("Copied $label")
                },
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Small,
                enabled = state.text.isNotBlank(),
            )
        }
    }
}

/** Copies [text] and flags it sensitive so Android's clipboard preview hides it. */
fun copySensitive(context: Context, label: String, text: String) {
    val clip = ClipData.newPlainText(label, text)
    clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}
