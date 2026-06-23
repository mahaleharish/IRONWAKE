package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.nativeCanvas
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ShinySteel
import kotlin.math.sqrt

@Composable
fun PatternTraceGrid(
    gridSize: Int = 3, // 3 for 3x3, 4 for 4x4
    patternColor: Color = NeonGreen,
    correctPattern: List<Int> = listOf(0, 1, 2, 4, 6), // Suggested Z or similar connection
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    onDragStateChanged: (Boolean) -> Unit = {}
) {
    var connectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var isError by remember { mutableStateOf(false) }
    var highlightedDotIndex by remember { mutableStateOf<Int?>(null) }

    // Sequential guide animation to highlight correct pattern nodes one-by-one
    androidx.compose.runtime.LaunchedEffect(correctPattern, connectedDots) {
        if (connectedDots.isNotEmpty()) {
            highlightedDotIndex = null
            return@LaunchedEffect
        }
        while (true) {
            if (correctPattern.isEmpty()) {
                highlightedDotIndex = null
                kotlinx.coroutines.delay(1000)
                continue
            }
            for (dot in correctPattern) {
                highlightedDotIndex = dot
                kotlinx.coroutines.delay(700)
            }
            highlightedDotIndex = null
            kotlinx.coroutines.delay(1200)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gridSize) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            onDragStateChanged(true)
                            isError = false
                            connectedDots = emptyList()
                            currentTouchPoint = startOffset
                            
                            // Check if initial touch hit a dot
                            val cellWidth = size.width.toFloat() / (gridSize + 1)
                            val cellHeight = size.height.toFloat() / (gridSize + 1)
                            val hitIndex = getHitDotIndex(
                                offset = startOffset,
                                gridSize = gridSize,
                                cellWidth = cellWidth,
                                cellHeight = cellHeight,
                                hitRadius = 35.dp.toPx()
                            )
                            if (hitIndex != -1) {
                                if (correctPattern.isNotEmpty() && hitIndex == correctPattern[0]) {
                                    connectedDots = listOf(hitIndex)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val nextPoint = change.position
                            currentTouchPoint = nextPoint
                            
                            val cellWidth = size.width.toFloat() / (gridSize + 1)
                            val cellHeight = size.height.toFloat() / (gridSize + 1)
                            val hitIndex = getHitDotIndex(
                                offset = nextPoint,
                                gridSize = gridSize,
                                cellWidth = cellWidth,
                                cellHeight = cellHeight,
                                hitRadius = 35.dp.toPx()
                            )
                            
                            if (hitIndex != -1 && !connectedDots.contains(hitIndex)) {
                                if (connectedDots.isEmpty()) {
                                    if (correctPattern.isNotEmpty() && hitIndex == correctPattern[0]) {
                                        connectedDots = listOf(hitIndex)
                                    }
                                } else {
                                    val expectedNextIndex = correctPattern.getOrNull(connectedDots.size)
                                    if (hitIndex == expectedNextIndex) {
                                        connectedDots = connectedDots + hitIndex
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            if (connectedDots.isNotEmpty()) {
                                onPatternComplete(connectedDots)
                            }
                            currentTouchPoint = null
                            connectedDots = emptyList()
                            onDragStateChanged(false)
                        },
                        onDragCancel = {
                            currentTouchPoint = null
                            connectedDots = emptyList()
                            onDragStateChanged(false)
                        }
                    )
                }
        ) {
            val cellWidth = size.width / (gridSize + 1)
            val cellHeight = size.height / (gridSize + 1)

            // 0. Draw faint sequential guide lines for correctPattern sequence
            if (correctPattern.size > 1) {
                for (i in 0 until correctPattern.size - 1) {
                    val dot1 = correctPattern[i]
                    val dot2 = correctPattern[i + 1]

                    val r1 = dot1 / gridSize
                    val c1 = dot1 % gridSize
                    val r2 = dot2 / gridSize
                    val c2 = dot2 % gridSize

                    val p1 = Offset((c1 + 1) * cellWidth, (r1 + 1) * cellHeight)
                    val p2 = Offset((c2 + 1) * cellWidth, (r2 + 1) * cellHeight)

                    drawLine(
                        color = patternColor.copy(alpha = 0.20f),
                        start = p1,
                        end = p2,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            val scale = if (gridSize >= 4) 0.7f else 1.0f
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = (12.dp * scale).toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            }

            // 1. Draw dot positions representing target pattern (dim neon background helper)
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val index = row * gridSize + col
                    val cx = (col + 1) * cellWidth
                    val cy = (row + 1) * cellHeight
                    
                    // Draw outer metallic shell
                    drawCircle(
                        color = ShinySteel,
                        radius = (20.dp * scale).toPx(),
                        center = Offset(cx, cy)
                    )

                    // Highlight target dots with concentric halos
                    val inCorrectPattern = correctPattern.contains(index)
                    if (inCorrectPattern) {
                        drawCircle(
                            color = patternColor.copy(alpha = 0.4f),
                            radius = (24.dp * scale).toPx(),
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = (2.dp * scale).toPx())
                        )
                    }

                    // Draw animated sequential guide glow
                    val isSeqHighlighted = index == highlightedDotIndex
                    if (isSeqHighlighted) {
                        drawCircle(
                            color = patternColor.copy(alpha = 0.75f),
                            radius = (29.dp * scale).toPx(),
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = (3.dp * scale).toPx())
                        )
                    }

                    // Draw connection center
                    val isConnected = connectedDots.contains(index)
                    val dotColor = when {
                        isConnected -> patternColor
                        inCorrectPattern -> patternColor.copy(alpha = 0.35f)
                        else -> Color.DarkGray
                    }
                    val pulseRadius = if (isConnected) (12.dp * scale).toPx() else (8.dp * scale).toPx()
                    drawCircle(
                        color = dotColor,
                        radius = pulseRadius,
                        center = Offset(cx, cy)
                    )

                    // If it is in correct pattern, draw sequence index
                    if (inCorrectPattern) {
                        val seqNum = correctPattern.indexOf(index) + 1
                        val textY = cy + (textPaint.textSize / 3f)
                        drawContext.canvas.nativeCanvas.drawText(
                            seqNum.toString(),
                            cx,
                            textY,
                            textPaint
                        )
                    }
                }
            }

            // 2. Draw active tracing lines done by user (or persist successfully matched lines)
            val dotsToDraw = if (patternColor != NeonGreen) correctPattern else connectedDots
            if (dotsToDraw.isNotEmpty()) {
                for (i in 0 until dotsToDraw.size - 1) {
                    val dot1 = dotsToDraw[i]
                    val dot2 = dotsToDraw[i + 1]

                    val r1 = dot1 / gridSize
                    val c1 = dot1 % gridSize
                    val r2 = dot2 / gridSize
                    val c2 = dot2 % gridSize

                    val p1 = Offset((c1 + 1) * cellWidth, (r1 + 1) * cellHeight)
                    val p2 = Offset((c2 + 1) * cellWidth, (r2 + 1) * cellHeight)

                    drawLine(
                        color = patternColor,
                        start = p1,
                        end = p2,
                        strokeWidth = (6.dp * scale).toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Draw line to current touch finger position
                if (connectedDots.isNotEmpty()) {
                    currentTouchPoint?.let { touch ->
                        val lastDot = connectedDots.last()
                        val r = lastDot / gridSize
                        val c = lastDot % gridSize
                        val lastDotCenter = Offset((c + 1) * cellWidth, (r + 1) * cellHeight)

                        drawLine(
                            color = patternColor.copy(alpha = 0.8f),
                            start = lastDotCenter,
                            end = touch,
                            strokeWidth = (4.dp * scale).toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

private fun getHitDotIndex(
    offset: Offset,
    gridSize: Int,
    cellWidth: Float,
    cellHeight: Float,
    hitRadius: Float
): Int {
    for (row in 0 until gridSize) {
        for (col in 0 until gridSize) {
            val cx = (col + 1) * cellWidth
            val cy = (row + 1) * cellHeight
            val dx = offset.x - cx
            val dy = offset.y - cy
            if (sqrt(dx * dx + dy * dy) <= hitRadius) {
                return row * gridSize + col
            }
        }
    }
    return -1
}
