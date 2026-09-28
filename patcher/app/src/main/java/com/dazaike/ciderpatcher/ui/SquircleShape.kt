package com.dazaike.ciderpatcher.ui

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Rounded rectangle whose corners are superellipse quadrants (|x|^n + |y|^n = 1) instead of circular arcs,
 * giving continuous "squircle" curvature. As a [CornerBasedShape] it plugs into Material [androidx.compose.material3.Shapes];
 * oversized radii are clamped to half the shorter side, so a large radius gives a squircle-ended capsule.
 */
@Immutable
class SquircleShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    private val exponent: Float = 5f,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    constructor(radius: Dp, exponent: Float = 5f) :
        this(CornerSize(radius), CornerSize(radius), CornerSize(radius), CornerSize(radius), exponent)

    override fun copy(topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize) =
        SquircleShape(topStart, topEnd, bottomEnd, bottomStart, exponent)

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        if (topStart + topEnd + bottomEnd + bottomStart == 0f) return Outline.Rectangle(Rect(Offset.Zero, size))
        val rtl = layoutDirection == LayoutDirection.Rtl
        val topLeft = if (rtl) topEnd else topStart
        val topRight = if (rtl) topStart else topEnd
        val bottomRight = if (rtl) bottomStart else bottomEnd
        val bottomLeft = if (rtl) bottomEnd else bottomStart
        val power = 2f / exponent
        val path = Path()
        var first = true
        // Walks one quadrant clockwise (screen coordinates) around the corner centre (cx, cy).
        fun corner(cx: Float, cy: Float, r: Float, startDegrees: Int) {
            for (i in 0..SEGMENTS) {
                val a = Math.toRadians(startDegrees + 90.0 * i / SEGMENTS)
                val c = cos(a).toFloat()
                val s = sin(a).toFloat()
                val x = cx + r * sign(c) * abs(c).pow(power)
                val y = cy + r * sign(s) * abs(s).pow(power)
                if (first) path.moveTo(x, y) else path.lineTo(x, y)
                first = false
            }
        }
        val w = size.width
        val h = size.height
        corner(w - topRight, topRight, topRight, 270)
        corner(w - bottomRight, h - bottomRight, bottomRight, 0)
        corner(bottomLeft, h - bottomLeft, bottomLeft, 90)
        corner(topLeft, topLeft, topLeft, 180)
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is SquircleShape && topStart == other.topStart && topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd && bottomStart == other.bottomStart && exponent == other.exponent

    override fun hashCode(): Int =
        ((((topStart.hashCode() * 31 + topEnd.hashCode()) * 31 + bottomEnd.hashCode()) * 31 + bottomStart.hashCode()) * 31) +
            exponent.hashCode()

    private companion object {
        const val SEGMENTS = 18
    }
}
