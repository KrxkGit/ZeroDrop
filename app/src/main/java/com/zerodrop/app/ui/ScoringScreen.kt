package com.zerodrop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerodrop.app.FsmState
import com.zerodrop.app.GameViewModel
import com.zerodrop.app.ui.theme.*

/**
 * Main scoring screen. Full-screen touch area with minimal visual elements:
 *  - Left score (top half) — self
 *  - Right score (bottom half) — opponent
 *  - Serve side indicator (colored bar between)
 *  - Small set score (top-right corner)
 *
 * Gestures:
 *  - Swipe right → self +1
 *  - Swipe left → opponent +1
 *  - Swipe down → undo
 *  - Long press (2s) → edit mode
 */
@Composable
fun ScoringScreen(
    onNewMatch: () -> Unit = {},
    viewModel: GameViewModel = viewModel(
        factory = GameViewModel.Factory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val state by viewModel.uiState.collectAsState()
    val canScore = state.fsmState == FsmState.PLAYING

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLED_BLACK)
            // Horizontal drag → scoring
            .pointerInput(canScore) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50f) {
                        viewModel.scoreLeft()
                    } else if (dragAmount < -50f) {
                        viewModel.scoreRight()
                    }
                }
            }
            // Vertical drag down → undo
            .pointerInput(state.canUndo) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 80f) {
                        viewModel.undo()
                    }
                }
            }
    ) {
        // Long press detection
        LongPressDetector(
            enabled = canScore,
            onLongPress = { viewModel.enterEditMode() }
        )

        // Main score layout
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Set indicator
            Text(
                text = "Set ${state.currentSet}",
                color = SCORE_DIM,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Self score (top half)
            ScoreDisplay(
                score = state.leftScore,
                color = when {
                    state.isMatchPoint -> SCORE_CRITICAL
                    state.isGamePoint -> SCORE_WARNING
                    else -> SCORE_WHITE
                },
                isServing = state.serveSide == 0,
                serveColor = SERVE_LEFT,
                modifier = Modifier.weight(1f)
            )

            // Serve indicator bar
            ServeIndicator(
                serveSide = state.serveSide,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )

            // Opponent score (bottom half)
            ScoreDisplay(
                score = state.rightScore,
                color = when {
                    state.isMatchPoint -> SCORE_CRITICAL
                    state.isGamePoint -> SCORE_WARNING
                    else -> SCORE_WHITE
                },
                isServing = state.serveSide == 1,
                serveColor = SERVE_RIGHT,
                modifier = Modifier.weight(1f)
            )
        }

        // Top-right: set wins
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${state.leftSetWins}",
                color = if (state.leftSetWins > state.rightSetWins) SERVE_LEFT else SCORE_DIM,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = "-", color = SCORE_DIM, fontSize = 14.sp)
            Text(
                text = "${state.rightSetWins}",
                color = if (state.rightSetWins > state.leftSetWins) SERVE_RIGHT else SCORE_DIM,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Undo indicator (bottom left) — subtle
        if (state.canUndo) {
            Text(
                text = "↓ undo",
                color = SCORE_DIM.copy(alpha = 0.4f),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }

    // ---- Overlays ----

    if (state.needsSideSwitch && state.fsmState == FsmState.SIDE_SWITCH) {
        SideSwitchDialog(
            setNumber = state.currentSet,
            onConfirm = { viewModel.confirmSideSwitch() }
        )
    }

    if (state.fsmState == FsmState.FINISHED) {
        MatchFinishedDialog(
            leftSetWins = state.leftSetWins,
            rightSetWins = state.rightSetWins,
            onNewMatch = onNewMatch
        )
    }
}

// ─── Composables ────────────────────────────────────────────────

@Composable
private fun LongPressDetector(
    enabled: Boolean,
    onLongPress: () -> Unit
) {
    // Long press is handled via Compose's combinedClickable or pointerInput
    // We'll detect via pointerInput with a timer
    var pressStart by remember { mutableLongStateOf(0L) }
    var longPressFired by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyDown = event.changes.any { it.pressed }
                        if (anyDown && pressStart == 0L) {
                            pressStart = System.currentTimeMillis()
                            longPressFired = false
                        } else if (!anyDown) {
                            pressStart = 0L
                            longPressFired = false
                        }
                        if (pressStart > 0L && !longPressFired &&
                            System.currentTimeMillis() - pressStart > 1500L
                        ) {
                            longPressFired = true
                            onLongPress()
                        }
                    }
                }
            }
    )
}

@Composable
private fun ScoreDisplay(
    score: Int,
    color: Color,
    isServing: Boolean,
    serveColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isServing) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = serveColor, shape = CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
            Text(
                text = "$score",
                color = color,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ServeIndicator(serveSide: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (serveSide == 0) SERVE_LEFT else Color.Transparent)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (serveSide == 1) SERVE_RIGHT else Color.Transparent)
        )
    }
}

@Composable
fun SideSwitchDialog(setNumber: Int, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* blocked — must confirm */ },
        title = {
            Text("换边 (Set $setNumber)")
        },
        text = {
            Text("双方请交换场地，确认后继续比赛。")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确认换边")
            }
        }
    )
}

@Composable
fun MatchFinishedDialog(
    leftSetWins: Int,
    rightSetWins: Int,
    onNewMatch: () -> Unit
) {
    val winner = if (leftSetWins > rightSetWins) "己方" else "对方"
    AlertDialog(
        onDismissRequest = onNewMatch,
        title = {
            Text("🏆 比赛结束")
        },
        text = {
            Text(
                "${winner}获胜！\n" +
                "大比分: $leftSetWins - $rightSetWins"
            )
        },
        confirmButton = {
            Button(onClick = onNewMatch) {
                Text("新比赛")
            }
        }
    )
}
