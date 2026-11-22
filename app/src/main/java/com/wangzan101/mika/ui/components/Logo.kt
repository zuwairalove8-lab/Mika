package com.wangzan101.mika.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Mika's mark: a graphite tile with an ice-blue M and a single teal spark.
 * Drawn in Compose so the same mark stays crisp at 20dp in a list and at
 * launcher size; the adaptive icon uses the same geometry as a vector.
 */
@Composable
fun MikaLogo(size: Dp, modifier: Modifier = Modifier, corner: Dp = size * 0.28f) {
    Canvas(modifier.size(size).clip(RoundedCornerShape(corner))) {
        drawRect(
            Brush.linearGradient(
                listOf(Color(0xFF1A2433), Color(0xFF0A0F16)),
                start = Offset(0f, 0f),
                end = Offset(this.size.width, this.size.height),
            ),
        )
        val w = this.size.width
        val h = this.size.height
        val mark = Path().apply {
            moveTo(w * 0.235f, h * 0.715f)
            lineTo(w * 0.235f, h * 0.305f)
            lineTo(w * 0.415f, h * 0.565f)
            lineTo(w * 0.500f, h * 0.405f)
            lineTo(w * 0.585f, h * 0.565f)
            lineTo(w * 0.765f, h * 0.305f)
            lineTo(w * 0.765f, h * 0.715f)
        }
        drawPath(
            mark,
            color = Color(0xFFD8E7FF),
            style = Stroke(width = w * 0.072f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(Color(0xFF5EEAD4), radius = w * 0.042f, center = Offset(w * 0.765f, h * 0.775f))
    }
}
