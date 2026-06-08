package com.zerodrop.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.platform.LocalConfiguration
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
 * Scoring screen. Minimal interaction:
 *  - Tap top half      → self +1
 *  - Tap bottom half   → opponent +1
 *  - Long press (1.5s) → enter edit mode
 *
 * Undo is available inside edit mode as a button.
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
    val isSideSwitch = state.fsmState == FsmState.SIDE_SWITCH

    val whoServes = state.serveSide shr 1
    val servesRight = (state.serveSide and 1) == 1

    // ---- Dynamic sizing ----
    val config = LocalConfiguration.current
    val minDim = minOf(config.screenWidthDp, config.screenHeightDp)
    val scale = (minDim / 200f).coerceIn(0.65f, 1.2f)
    val scoreFontSz = (60 * scale).sp
    val setFontSz    = (10 * scale).sp
    val winsFontSz   = (11 * scale).sp
    val dotSz        = (10 * scale).dp
    val dotGapSz     = (6 * scale).dp
    val dotPhSz      = (16 * scale).dp
    val barH         = (3 * scale).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLED_BLACK)
            // Tap + long-press only — no swipe gesture
            .pointerInput(canScore) {
                detectTapGestures(
                    onTap = { offset ->
                        if (!canScore) return@detectTapGestures
                        val h = size.height.toFloat()
                        if (offset.y < h / 2) viewModel.scoreLeft()
                        else viewModel.scoreRight()
                    },
                    onLongPress = {
                        if (!canScore) return@detectTapGestures
                        viewModel.enterEditMode()
                    }
                )
            }
    ) {
        // ---- Score display ----
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildString {
                    append("Set ${state.currentSet}")
                    if (whoServes == 0) append(" · 己方发球") else append(" · 对方发球")
                },
                color = SCORE_DIM,
                fontSize = setFontSz,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            ScoreDisplay(
                score = state.leftScore,
                color = when {
                    state.isMatchPoint -> SCORE_CRITICAL
                    state.isGamePoint -> SCORE_WARNING
                    else -> SCORE_WHITE
                },
                isServing = whoServes == 0,
                serveColor = if (servesRight) SERVE_RIGHT else SERVE_LEFT,
                modifier = Modifier.weight(1f),
                scoreFontSize = scoreFontSz, dotSize = dotSz,
                dotGap = dotGapSz, dotPlaceholder = dotPhSz
            )

            ServeIndicator(
                isSelfServing = whoServes == 0,
                servesRight = servesRight,
                modifier = Modifier.fillMaxWidth().height(barH)
            )

            ScoreDisplay(
                score = state.rightScore,
                color = when {
                    state.isMatchPoint -> SCORE_CRITICAL
                    state.isGamePoint -> SCORE_WARNING
                    else -> SCORE_WHITE
                },
                isServing = whoServes == 1,
                serveColor = if (servesRight) SERVE_RIGHT else SERVE_LEFT,
                modifier = Modifier.weight(1f),
                scoreFontSize = scoreFontSz, dotSize = dotSz,
                dotGap = dotGapSz, dotPlaceholder = dotPhSz
            )
        }

        // ---- Top-right: set wins ----
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "${state.leftSetWins}",
                color = if (state.leftSetWins > state.rightSetWins) SERVE_LEFT else SCORE_DIM,
                fontSize = winsFontSz, fontWeight = FontWeight.Bold
            )
            Text(text = "-", color = SCORE_DIM, fontSize = winsFontSz)
            Text(
                text = "${state.rightSetWins}",
                color = if (state.rightSetWins > state.leftSetWins) SERVE_RIGHT else SCORE_DIM,
                fontSize = winsFontSz, fontWeight = FontWeight.Bold
            )
        }
    }

    // ---- Overlays ----

    if (state.isEditMode) {
        EditModeOverlay(
            leftScore = state.leftScore,
            rightScore = state.rightScore,
            serveSide = state.serveSide,
            canUndo = state.canUndo,
            onScoreChange = { l, r, sv -> viewModel.setEditScores(l, r, sv) },
            onConfirm = { viewModel.confirmEdit() },
            onUndo = { viewModel.undo() },
            onNewMatch = onNewMatch
        )
    }

    if (isSideSwitch) {
        // Mid-set side switch: needsSideSwitch=true
        // Set-end side switch: needsSetEndSwitch=true
        val isSetEnd = state.needsSetEndSwitch
        SideSwitchDialog(
            afterSet = isSetEnd,
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
private fun ScoreDisplay(
    score: Int, color: Color, isServing: Boolean, serveColor: Color,
    modifier: Modifier = Modifier,
    scoreFontSize: androidx.compose.ui.unit.TextUnit,
    dotSize: androidx.compose.ui.unit.Dp,
    dotGap: androidx.compose.ui.unit.Dp,
    dotPlaceholder: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isServing) {
                Box(Modifier.size(dotSize).background(color = serveColor, shape = CircleShape))
                Spacer(Modifier.height(dotGap))
            } else {
                Spacer(Modifier.height(dotPlaceholder))
            }
            Text(
                text = "$score", color = color, fontSize = scoreFontSize,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ServeIndicator(isSelfServing: Boolean, servesRight: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Box(
            Modifier.weight(1f).fillMaxHeight()
                .background(if (!servesRight) (if (isSelfServing) SERVE_LEFT else SERVE_RIGHT) else Color.Transparent)
        )
        Box(
            Modifier.weight(1f).fillMaxHeight()
                .background(if (servesRight) (if (isSelfServing) SERVE_LEFT else SERVE_RIGHT) else Color.Transparent)
        )
    }
}

@Composable
fun SideSwitchDialog(afterSet: Boolean, setNumber: Int, onConfirm: () -> Unit) {
    val body = if (afterSet)
        "第${setNumber}局结束，交换场地后继续。"
    else
        "双方请交换场地，确认后继续比赛。"
    AlertDialog(
        onDismissRequest = { /* blocked */ },
        title = { Text("换边") },
        text = { Text(body) },
        confirmButton = { Button(onClick = onConfirm) { Text("确认") } }
    )
}

@Composable
fun MatchFinishedDialog(leftSetWins: Int, rightSetWins: Int, onNewMatch: () -> Unit) {
    val winner = if (leftSetWins > rightSetWins) "己方" else "对方"
    AlertDialog(
        onDismissRequest = onNewMatch,
        title = { Text("🏆 比赛结束") },
        text = { Text("${winner}获胜！\n大比分: $leftSetWins - $rightSetWins") },
        confirmButton = { Button(onClick = onNewMatch) { Text("新比赛") } }
    )
}
