package com.zerodrop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerodrop.app.ui.theme.*

/**
 * Overlay for mid-game score editing.
 * Tap +/- to adjust, serve area to toggle, buttons for undo / confirm / new match.
 */
@Composable
fun EditModeOverlay(
    leftScore: Int,
    rightScore: Int,
    serveSide: Int,
    canUndo: Boolean = false,
    onScoreChange: (left: Int, right: Int, serveSide: Int) -> Unit,
    onConfirm: () -> Unit,
    onUndo: () -> Unit = {},
    onNewMatch: () -> Unit = {}
) {
    var editLeft by remember(leftScore) { mutableIntStateOf(leftScore) }
    var editRight by remember(rightScore) { mutableIntStateOf(rightScore) }
    var editWhoServes by remember(serveSide) { mutableIntStateOf(serveSide shr 1) }

    val config = LocalConfiguration.current
    val minDim = minOf(config.screenWidthDp, config.screenHeightDp)
    val scale = (minDim / 200f).coerceIn(0.65f, 1.2f)

    val computeServeSide: () -> Int = {
        val court = if (editWhoServes == 0) {
            if (editLeft % 2 == 0) 1 else 0
        } else {
            if (editRight % 2 == 0) 1 else 0
        }
        (editWhoServes shl 1) or court
    }

    val updateScores: () -> Unit = {
        onScoreChange(editLeft, editRight, computeServeSide())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = (8 * scale).dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "编辑模式",
                color = SCORE_WARNING,
                fontSize = (12 * scale).sp,
                modifier = Modifier.padding(bottom = (10 * scale).dp)
            )

            // Self score
            EditScoreRow(
                label = "己方",
                score = editLeft,
                color = SCORE_WHITE,
                scale = scale,
                onIncrement = { editLeft++; updateScores() },
                onDecrement = { if (editLeft > 0) { editLeft--; updateScores() } }
            )

            Spacer(modifier = Modifier.height((12 * scale).dp))

            // Serve toggle
            ServeWhoToggle(
                whoServes = editWhoServes,
                scale = scale,
                onToggle = {
                    editWhoServes = 1 - editWhoServes
                    updateScores()
                }
            )

            Spacer(modifier = Modifier.height((12 * scale).dp))

            // Opponent score
            EditScoreRow(
                label = "对方",
                score = editRight,
                color = SCORE_WHITE,
                scale = scale,
                onIncrement = { editRight++; updateScores() },
                onDecrement = { if (editRight > 0) { editRight--; updateScores() } }
            )

            Spacer(modifier = Modifier.height((14 * scale).dp))

            // Button row: 撤回 | 确认
            Row(horizontalArrangement = Arrangement.spacedBy((10 * scale).dp)) {
                TextButton(
                    onClick = {
                        onUndo()          // pop the most recent score entry from history
                        // Sync local edit state with whatever undo restored
                        // (editLeft/editRight will be reset by remember() when parent recomposes)
                    },
                    enabled = canUndo
                ) {
                    Text("撤回", color = if (canUndo) SCORE_DIM else SCORE_DIM.copy(alpha = 0.25f),
                        fontSize = (10 * scale).sp)
                }
                Button(onClick = onConfirm) {
                    Text("确认", fontSize = (10 * scale).sp)
                }
            }

            Spacer(modifier = Modifier.height((10 * scale).dp))

            // 新比赛
            TextButton(onClick = onNewMatch) {
                Text("新比赛", color = SCORE_DIM.copy(alpha = 0.5f), fontSize = (9 * scale).sp)
            }
        }
    }
}

@Composable
private fun EditScoreRow(
    label: String, score: Int, color: Color, scale: Float,
    onIncrement: () -> Unit, onDecrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = SCORE_DIM, fontSize = (10 * scale).sp)
        Spacer(modifier = Modifier.height((4 * scale).dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((12 * scale).dp)
        ) {
            // − button
            Box(
                modifier = Modifier
                    .size((38 * scale).dp)
                    .clip(CircleShape)
                    .background(SCORE_DIM.copy(alpha = 0.2f))
                    .clickable { onDecrement() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "−", color = SCORE_WHITE, fontSize = (20 * scale).sp)
            }

            // Score display
            Box(
                modifier = Modifier
                    .width((90 * scale).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score", color = color,
                    fontSize = (42 * scale).sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
            }

            // + button
            Box(
                modifier = Modifier
                    .size((38 * scale).dp)
                    .clip(CircleShape)
                    .background(SCORE_DIM.copy(alpha = 0.2f))
                    .clickable { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", color = SCORE_WHITE, fontSize = (20 * scale).sp)
            }
        }
    }
}

@Composable
private fun ServeWhoToggle(whoServes: Int, scale: Float, onToggle: () -> Unit) {
    val isSelfServing = whoServes == 0
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SCORE_DIM.copy(alpha = 0.1f))
            .clickable { onToggle() }
            .padding((8 * scale).dp)
    ) {
        Text(text = "发球方", color = SCORE_DIM, fontSize = (10 * scale).sp)
        Spacer(modifier = Modifier.height((6 * scale).dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy((18 * scale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size((26 * scale).dp)
                    .background(color = if (isSelfServing) SERVE_LEFT else Color.Transparent, shape = CircleShape)
            )
            Text(text = "↔", color = SCORE_DIM, fontSize = (10 * scale).sp)
            Box(
                Modifier.size((26 * scale).dp)
                    .background(color = if (!isSelfServing) SERVE_RIGHT else Color.Transparent, shape = CircleShape)
            )
        }
    }
}
