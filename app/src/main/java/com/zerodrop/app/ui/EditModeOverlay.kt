package com.zerodrop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerodrop.app.ui.theme.*

/**
 * Overlay for mid-game score editing.
 * Swipe up/down on each score to adjust, tap serve indicator to toggle.
 */
@Composable
fun EditModeOverlay(
    leftScore: Int,
    rightScore: Int,
    serveSide: Int,
    onScoreChange: (left: Int, right: Int, serveSide: Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var editLeft by remember(leftScore) { mutableIntStateOf(leftScore) }
    var editRight by remember(rightScore) { mutableIntStateOf(rightScore) }
    var editServe by remember(serveSide) { mutableIntStateOf(serveSide) }

    // sync changes back to VM
    val updateScores: () -> Unit = {
        onScoreChange(editLeft, editRight, editServe)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "编辑模式",
                color = SCORE_WARNING,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Left score editor
            EditScoreRow(
                label = "己方",
                score = editLeft,
                color = SCORE_WHITE,
                onIncrement = {
                    editLeft++
                    updateScores()
                },
                onDecrement = {
                    if (editLeft > 0) editLeft--
                    updateScores()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Serve toggle
            ServeToggle(
                serveSide = editServe,
                onToggle = {
                    editServe = 1 - editServe
                    updateScores()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Right score editor
            EditScoreRow(
                label = "对方",
                score = editRight,
                color = SCORE_WHITE,
                onIncrement = {
                    editRight++
                    updateScores()
                },
                onDecrement = {
                    if (editRight > 0) editRight--
                    updateScores()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消", color = SCORE_DIM)
                }
                Button(onClick = onConfirm) {
                    Text("确认")
                }
            }
        }
    }
}

@Composable
private fun EditScoreRow(
    label: String,
    score: Int,
    color: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = SCORE_DIM, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Decrement
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SCORE_DIM.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    color = SCORE_WHITE,
                    fontSize = 24.sp
                )
            }

            // Score display — editable via vertical swipes
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 30f) onIncrement()
                            else if (dragAmount < -30f) onDecrement()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score",
                    color = color,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Increment
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SCORE_DIM.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = SCORE_WHITE,
                    fontSize = 24.sp
                )
            }
        }
    }
}

@Composable
private fun ServeToggle(serveSide: Int, onToggle: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SCORE_DIM.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Text(text = "发球方", color = SCORE_DIM, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (serveSide == 0) SERVE_LEFT else Color.Transparent,
                        shape = CircleShape
                    )
                    .then(
                        if (serveSide == 0) Modifier else
                            Modifier.background(color = Color.Transparent, shape = CircleShape)
                    )
            )
            Text(text = "← →", color = SCORE_DIM, fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (serveSide == 1) SERVE_RIGHT else Color.Transparent,
                        shape = CircleShape
                    )
            )
        }
    }
}
