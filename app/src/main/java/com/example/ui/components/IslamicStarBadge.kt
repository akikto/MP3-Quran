package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * An authentic 8-pointed Islamic Star (Rub el Hizb) badge
 * showing the Surah or verse number inside.
 */
@Composable
fun IslamicStarBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isActive: Boolean = false,
    goldColor: Color = GoldAccent
) {
    val starColor = if (isActive) goldColor else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val outlineColor = if (isActive) Color.White else goldColor.copy(alpha = 0.7f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = (this.size.minDimension / 2f) * 0.95f
            val innerRadius = outerRadius * 0.73f

            val points = 8
            val path = Path()

            for (i in 0 until points * 2) {
                val radius = if (i % 2 == 0) outerRadius else innerRadius
                val angle = (i * PI / points) - (PI / 2)
                val x = center.x + (radius * cos(angle)).toFloat()
                val y = center.y + (radius * sin(angle)).toFloat()

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            // Fill
            drawPath(path = path, color = starColor, style = Fill)
            // Delicate outline
            drawPath(path = path, color = outlineColor, style = Stroke(width = 1.5.dp.toPx()))
        }

        Text(
            text = number.toString(),
            color = if (isActive) Color(0xFF1E2822) else Color.White,
            fontSize = if (number > 99) 11.sp else 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
