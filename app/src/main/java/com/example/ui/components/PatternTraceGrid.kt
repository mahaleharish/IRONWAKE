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
    modifier: Modifier = Modifier
) {
    var connectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var isError by remember { mutableStateOf(false) }

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
                                connectedDots = listOf(hitIndex)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextPoint = currentTouchPoint?.let { it + dragAmount } ?: change.position
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
                                connectedDots = connectedDots + hitIndex
                            }
                        },
                        onDragEnd = {
                            if (connectedDots.isNotEmpty()) {
                                onPatternComplete(connectedDots)
                            }
                            currentTouchPoint = null
                            connectedDots = emptyList()
                        },
                        onDragCancel = {
                            currentTouchPoint = null
                            connectedDots = emptyList()
                        }
                    )
                }
        ) {
            val cellWidth = size.width / (gridSize + 1)
            val cellHeight = size.height / (gridSize + 1)

            // 1. Draw dot positions representing target pattern (dim neon background helper)
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val index = row * gridSize + col
                    val cx = (col + 1) * cellWidth
                    val cy = (row + 1) * cellHeight
                    
                    // Draw outer metallic shell
                    drawCircle(
                        color = ShinySteel,
                        radius = 18.dp.toPx(),
                        center = Offset(cx, cy)
                    )

                    // Draw connection center
                    val isConnected = connectedDots.contains(index)
                    val dotColor = when {
                        isConnected -> patternColor
                        correctPattern.contains(index) -> NeonCyan.copy(alpha = 0.35f)
                        else -> Color.DarkGray
                    }
                    val pulseRadius = if (isConnected) 10.dp.toPx() else 6.dp.toPx()
                    drawCircle(
                        color = dotColor,
                        radius = pulseRadius,
                        center = Offset(cx, cy)
                    )
                }
            }

            // 2. Draw tracing lines
            if (connectedDots.isNotEmpty()) {
                for (i in 0 until connectedDots.size - 1) {
                    val dot1 = connectedDots[i]
                    val dot2 = connectedDots[i + 1]

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
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Draw line to current touch finger position
                currentTouchPoint?.let { touch ->
                    val lastDot = connectedDots.last()
                    val r = lastDot / gridSize
                    val c = lastDot % gridSize
                    val lastDotCenter = Offset((c + 1) * cellWidth, (r + 1) * cellHeight)

                    drawLine(
                        color = patternColor.copy(alpha = 0.8f),
                        start = lastDotCenter,
                        end = touch,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
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
