package com.example.mymusic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class IconType { Play, Pause, Next, Previous, Sparkle, Locate, Down }

// A chevron shape: three points joined by a line
private fun DrawScope.drawChevron(
    color: Color, stroke: Stroke,
    x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float
) {
    val path = Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
        lineTo(x3, y3)
    }
    drawPath(path, color, style = stroke)
}

// A four-point star with curved-in sides
private fun DrawScope.drawSparkle(cx: Float, cy: Float, r: Float, color: Color) {
    val k = r * 0.12f
    val path = Path().apply {
        moveTo(cx, cy - r)
        quadraticBezierTo(cx + k, cy - k, cx + r, cy)
        quadraticBezierTo(cx + k, cy + k, cx, cy + r)
        quadraticBezierTo(cx - k, cy + k, cx - r, cy)
        quadraticBezierTo(cx - k, cy - k, cx, cy - r)
        close()
    }
    drawPath(path, color)
}

@Composable
fun AppIcon(type: IconType, modifier: Modifier = Modifier, tint: Color = TextMain) {
    Canvas(modifier = modifier) {
        val u = size.minDimension / 24f   // one unit on a 24 x 24 grid
        val line = Stroke(width = 2.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (type) {
            IconType.Play -> {
                val triangle = Path().apply {
                    moveTo(8f * u, 5.5f * u)
                    lineTo(18f * u, 12f * u)
                    lineTo(8f * u, 18.5f * u)
                    close()
                }
                drawPath(triangle, tint, style = Fill)
                drawPath(triangle, tint, style = line)   // rounds the corners
            }

            IconType.Pause -> {
                val barWidth = 3.4f * u
                drawLine(tint, Offset(8.5f * u, 6f * u), Offset(8.5f * u, 18f * u),
                    strokeWidth = barWidth, cap = StrokeCap.Round)
                drawLine(tint, Offset(15.5f * u, 6f * u), Offset(15.5f * u, 18f * u),
                    strokeWidth = barWidth, cap = StrokeCap.Round)
            }

            IconType.Next -> {
                drawChevron(tint, line, 5f * u, 6.5f * u, 10.5f * u, 12f * u, 5f * u, 17.5f * u)
                drawChevron(tint, line, 12.5f * u, 6.5f * u, 18f * u, 12f * u, 12.5f * u, 17.5f * u)
            }

            IconType.Previous -> {
                drawChevron(tint, line, 19f * u, 6.5f * u, 13.5f * u, 12f * u, 19f * u, 17.5f * u)
                drawChevron(tint, line, 11.5f * u, 6.5f * u, 6f * u, 12f * u, 11.5f * u, 17.5f * u)
            }

            IconType.Sparkle -> {
                drawSparkle(10.5f * u, 13.5f * u, 8.5f * u, tint)
                drawSparkle(19.5f * u, 4.5f * u, 3.2f * u, tint)
            }

            IconType.Locate -> {
                drawCircle(tint, radius = 6.5f * u, center = Offset(12f * u, 12f * u), style = line)
                drawCircle(tint, radius = 2.2f * u, center = Offset(12f * u, 12f * u))
                drawLine(tint, Offset(12f * u, 1.8f * u), Offset(12f * u, 4.2f * u),
                    strokeWidth = 2.4f * u, cap = StrokeCap.Round)
                drawLine(tint, Offset(12f * u, 19.8f * u), Offset(12f * u, 22.2f * u),
                    strokeWidth = 2.4f * u, cap = StrokeCap.Round)
                drawLine(tint, Offset(1.8f * u, 12f * u), Offset(4.2f * u, 12f * u),
                    strokeWidth = 2.4f * u, cap = StrokeCap.Round)
                drawLine(tint, Offset(19.8f * u, 12f * u), Offset(22.2f * u, 12f * u),
                    strokeWidth = 2.4f * u, cap = StrokeCap.Round)
            }

            IconType.Down -> {
                val thick = Stroke(width = 2.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawChevron(tint, thick, 6f * u, 9f * u, 12f * u, 15f * u, 18f * u, 9f * u)
            }
        }
    }
}

// A rounded-square button with one of our icons inside
@Composable
fun IconBtn(
    type: IconType,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    background: Color = Color.Transparent,
    tint: Color = TextMain,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(type, Modifier.size(iconSize), tint)
    }
}