package com.nihongo.app.ui.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.random.Random

private data class Blossom(val x: Float, val y: Float, val radius: Float, val alpha: Float)

@Composable
fun SplashIllustration(modifier: Modifier = Modifier) {
    val blossoms = remember {
        val random = Random(7)
        List(48) {
            Blossom(
                x = random.nextFloat() * 0.40f,
                y = random.nextFloat() * 0.28f,
                radius = 0.008f + random.nextFloat() * 0.014f,
                alpha = 0.6f + random.nextFloat() * 0.4f,
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Langit
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE6E9FF), Color(0xFFF6E9F3)),
            ),
        )

        // Matahari
        drawCircle(
            color = Color(0xFFF37A7A),
            radius = w * 0.13f,
            center = Offset(w * 0.68f, h * 0.30f),
        )

        // Gunung
        val mountain = Path().apply {
            moveTo(-0.1f * w, 0.86f * h)
            quadraticTo(0.30f * w, 0.74f * h, 0.43f * w, 0.40f * h)
            lineTo(0.49f * w, 0.40f * h)
            quadraticTo(0.62f * w, 0.74f * h, 1.1f * w, 0.86f * h)
            close()
        }
        drawPath(
            path = mountain,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF8D98E8), Color(0xFFCBD0F8)),
                startY = 0.40f * h,
                endY = 0.86f * h,
            ),
        )

        // Salju di puncak
        val snow = Path().apply {
            moveTo(0.43f * w, 0.40f * h)
            lineTo(0.49f * w, 0.40f * h)
            lineTo(0.545f * w, 0.53f * h)
            lineTo(0.51f * w, 0.50f * h)
            lineTo(0.48f * w, 0.55f * h)
            lineTo(0.45f * w, 0.50f * h)
            lineTo(0.415f * w, 0.545f * h)
            close()
        }
        drawPath(path = snow, color = Color(0xFFF7F8FF))

        // Danau
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFC3C9F4), Color(0xFFE9EBFF)),
                startY = 0.84f * h,
                endY = h,
            ),
            topLeft = Offset(0f, 0.84f * h),
            size = Size(w, 0.16f * h),
        )

        // Torii
        val red = Color(0xFFD9534F)
        val cx = 0.72f * w
        val toriiWidth = 0.16f * w
        val toriiHeight = 0.12f * h
        val baseY = 0.845f * h
        val postWidth = toriiWidth * 0.11f
        drawRect(red, Offset(cx - toriiWidth * 0.36f - postWidth / 2f, baseY - toriiHeight), Size(postWidth, toriiHeight))
        drawRect(red, Offset(cx + toriiWidth * 0.36f - postWidth / 2f, baseY - toriiHeight), Size(postWidth, toriiHeight))
        drawRect(red, Offset(cx - toriiWidth / 2f, baseY - toriiHeight), Size(toriiWidth, toriiHeight * 0.14f))
        drawRect(red, Offset(cx - toriiWidth * 0.42f, baseY - toriiHeight * 0.72f), Size(toriiWidth * 0.84f, toriiHeight * 0.09f))

        // Ranting sakura
        val branchColor = Color(0xFF7B4F5C)
        val branch = Path().apply {
            moveTo(-0.02f * w, 0.03f * h)
            quadraticTo(0.20f * w, 0.08f * h, 0.36f * w, 0.20f * h)
        }
        drawPath(branch, branchColor, style = Stroke(width = w * 0.012f, cap = StrokeCap.Round))
        val twig = Path().apply {
            moveTo(0.14f * w, 0.065f * h)
            quadraticTo(0.20f * w, 0.15f * h, 0.22f * w, 0.26f * h)
        }
        drawPath(twig, branchColor, style = Stroke(width = w * 0.008f, cap = StrokeCap.Round))

        blossoms.forEach { blossom ->
            drawCircle(
                color = Color(0xFFF7B6CC).copy(alpha = blossom.alpha),
                radius = blossom.radius * w,
                center = Offset(blossom.x * w, blossom.y * h),
            )
        }
    }
}

@Composable
fun MenekiNeko(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val white = Color(0xFFFFFFFF)
        val outline = Color(0xFFD9DBEE)
        val ink = Color(0xFF2B2B3A)
        val pink = Color(0xFFF2A3B8)
        val outlineStroke = Stroke(width = s * 0.015f)

        // Badan
        drawOval(white, Offset(0.18f * s, 0.52f * s), Size(0.64f * s, 0.46f * s))
        drawOval(outline, Offset(0.18f * s, 0.52f * s), Size(0.64f * s, 0.46f * s), style = outlineStroke)

        // Telinga
        val leftEar = Path().apply {
            moveTo(0.22f * s, 0.36f * s)
            lineTo(0.26f * s, 0.10f * s)
            lineTo(0.46f * s, 0.24f * s)
            close()
        }
        val rightEar = Path().apply {
            moveTo(0.78f * s, 0.36f * s)
            lineTo(0.74f * s, 0.10f * s)
            lineTo(0.54f * s, 0.24f * s)
            close()
        }
        drawPath(leftEar, white)
        drawPath(rightEar, white)
        drawPath(leftEar, outline, style = outlineStroke)
        drawPath(rightEar, outline, style = outlineStroke)
        drawCircle(pink, radius = s * 0.03f, center = Offset(0.30f * s, 0.21f * s))
        drawCircle(pink, radius = s * 0.03f, center = Offset(0.70f * s, 0.21f * s))

        // Kepala
        drawOval(white, Offset(0.16f * s, 0.20f * s), Size(0.68f * s, 0.50f * s))
        drawOval(outline, Offset(0.16f * s, 0.20f * s), Size(0.68f * s, 0.50f * s), style = outlineStroke)

        // Bercak warna
        drawOval(Color(0xFFF2A65A), Offset(0.24f * s, 0.23f * s), Size(0.18f * s, 0.12f * s))
        drawOval(ink, Offset(0.60f * s, 0.23f * s), Size(0.16f * s, 0.11f * s))

        // Mata
        drawArc(ink, 200f, 140f, false, Offset(0.30f * s, 0.42f * s), Size(0.12f * s, 0.08f * s), style = Stroke(width = s * 0.02f, cap = StrokeCap.Round))
        drawArc(ink, 200f, 140f, false, Offset(0.58f * s, 0.42f * s), Size(0.12f * s, 0.08f * s), style = Stroke(width = s * 0.02f, cap = StrokeCap.Round))

        // Hidung dan mulut
        drawCircle(pink, radius = s * 0.018f, center = Offset(0.50f * s, 0.53f * s))
        drawArc(ink, 20f, 140f, false, Offset(0.44f * s, 0.53f * s), Size(0.06f * s, 0.05f * s), style = Stroke(width = s * 0.012f, cap = StrokeCap.Round))
        drawArc(ink, 20f, 140f, false, Offset(0.50f * s, 0.53f * s), Size(0.06f * s, 0.05f * s), style = Stroke(width = s * 0.012f, cap = StrokeCap.Round))

        // Kalung merah dan lonceng emas
        drawRoundRect(Color(0xFFD9534F), Offset(0.26f * s, 0.66f * s), Size(0.48f * s, 0.07f * s), CornerRadius(s * 0.035f))
        drawCircle(Color(0xFFF2C14E), radius = s * 0.055f, center = Offset(0.50f * s, 0.75f * s))
        drawCircle(Color(0xFFFFE9A6), radius = s * 0.018f, center = Offset(0.485f * s, 0.735f * s))

        // Tangan yang melambai
        drawRoundRect(white, Offset(0.04f * s, 0.30f * s), Size(0.17f * s, 0.30f * s), CornerRadius(s * 0.085f))
        drawRoundRect(outline, Offset(0.04f * s, 0.30f * s), Size(0.17f * s, 0.30f * s), CornerRadius(s * 0.085f), style = outlineStroke)
        drawCircle(pink, radius = s * 0.025f, center = Offset(0.125f * s, 0.36f * s))
    }
}