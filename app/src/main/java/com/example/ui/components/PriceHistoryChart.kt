package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PriceHistoryPoint
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceHistoryChart(
    points: List<PriceHistoryPoint>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textStyle = TextStyle(
        color = textColor.copy(alpha = 0.8f),
        fontSize = 10.sp
    )

    val chartLineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        if (points.size < 2) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Awaiting sufficient historic data...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Scraping updates to populate chart timeline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Reserves space on the right for price labels and bottom for dates
                val paddingRight = 140f
                val paddingBottom = 40f
                val paddingTop = 20f
                val paddingLeft = 10f

                val chartWidth = width - paddingRight - paddingLeft
                val chartHeight = height - paddingBottom - paddingTop

                val prices = points.map { pt -> pt.price }
                val minPrice = (prices.minOrNull() ?: 0.0) * 0.95 // 5% cushion
                val maxPrice = (prices.maxOrNull() ?: 100.0) * 1.05 // 5% cushion
                val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

                val activePoints = points.sortedBy { pt -> pt.timestamp }
                val minTime = activePoints.first().timestamp
                val maxTime = activePoints.last().timestamp
                val timeRange = if (maxTime - minTime == 0L) 1L else maxTime - minTime

                // 1. Draw gridlines & labels
                val gridLines = 4
                for (i in 0..gridLines) {
                    val ratio = i.toFloat() / gridLines
                    val y = paddingTop + chartHeight * (1f - ratio)
                    val priceLabel = minPrice + (priceRange * ratio)

                    // Draw grid dashed line
                    drawLine(
                        color = textColor.copy(alpha = 0.12f),
                        start = Offset(paddingLeft, y),
                        end = Offset(paddingLeft + chartWidth, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Draw Price Label on right
                    val formattedPrice = String.format(Locale.US, "₹%,.2f", priceLabel)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = formattedPrice,
                        style = textStyle,
                        topLeft = Offset(paddingLeft + chartWidth + 15f, y - 18f)
                    )
                }

                // 2. Map coordinates
                val coords = activePoints.map { pt ->
                    val x = paddingLeft + ((pt.timestamp - minTime).toFloat() / timeRange.toFloat()) * chartWidth
                    val y = paddingTop + (1f - ((pt.price - minPrice).toFloat() / priceRange.toFloat())) * chartHeight
                    Offset(x, y)
                }

                // 3. Draw gradient area fill below price curve
                val gradientPath = Path().apply {
                    moveTo(coords.first().x, paddingTop + chartHeight)
                    for (i in coords.indices) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                    lineTo(coords.last().x, paddingTop + chartHeight)
                    close()
                }

                drawPath(
                    path = gradientPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            chartLineColor.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )

                // 4. Draw curve line itself
                val curvePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        val pPrev = coords[i - 1]
                        val pCurr = coords[i]
                        // Draw smooth cubic or quadratic lines
                        val cx = (pPrev.x + pCurr.x) / 2
                        quadraticTo(pPrev.x, pPrev.y, cx, (pPrev.y + pCurr.y) / 2)
                    }
                    lineTo(coords.last().x, coords.last().y)
                }

                drawPath(
                    path = curvePath,
                    color = chartLineColor,
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 5. Draw point dots & dates
                val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
                coords.forEachIndexed { index, offset ->
                    // Draw point dot
                    drawCircle(
                        color = pointColor,
                        radius = 12f,
                        center = offset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = offset
                    )

                    // Add date text for first, middle, and last nodes to avoid clutter
                    if (index == 0 || index == coords.size - 1 || (coords.size > 2 && index == coords.size / 2)) {
                        val dateText = dateFormat.format(Date(activePoints[index].timestamp))
                        drawText(
                            textMeasurer = textMeasurer,
                            text = dateText,
                            style = textStyle,
                            topLeft = Offset(offset.x - 40f, paddingTop + chartHeight + 10f)
                        )
                    }
                }
            }
        }
    }
}
