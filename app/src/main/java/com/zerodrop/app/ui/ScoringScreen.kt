package com.zerodrop.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * ── Ambient mode (PRD §4.3) ──
 * When in ambient (微光) mode:
 *  - Background stays pure black
 *  - Score colors: white → dimmed gray
 *  - Serve dot: solid fill → hollow ring (outline only)
 *  - Serve indicator bar: hidden entirely
 *  - Set wins text: dimmed
 *  - Animations: disabled (no AnimatedVisibility)
 *  - Tap/long-press gestures: still active (wrist-up → exit ambient → tap registers)
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
    val ambient = LocalAmbientState.current
    val isAmbient = ambient.isAmbient

    val canScore = state.fsmState == FsmState.PLAYING && !isAmbient
    val isSideSwitch = state.fsmState == FsmState.SIDE_SWITCH

    val whoServes = state.serveSide shr 1
    val servesRight = (state.serveSide and 1) == 1

    // ---- Dynamic sizing ----
    val config = LocalConfiguration.current
    val minDim = minOf(config.screenWidthDp, config.screenHeightDp)
    val scale = (minDim / 200f).coerceIn(0.65f, 1.2f)

    // Round screens have less visible area near the edges — use a smaller font
    // so digits don't get clipped by the circular display boundary.
    val isRound = config.isScreenRound
    val scoreFontSz = if (isRound) (48 * scale).sp else (60 * scale).sp
    val setFontSz    = (10 * scale).sp
    val winsFontSz   = (11 * scale).sp
    val dotSz        = (10 * scale).dp
    val dotGapSz     = (6 * scale).dp
    val dotPhSz      = (16 * scale).dp
    val barH         = (3 * scale).dp

    // ── Ambient-aware colors ──
    val scoreColorActive = when {
        state.isMatchPoint -> SCORE_CRITICAL
        state.isGamePoint -> SCORE_WARNING
        else -> SCORE_WHITE
    }
    val scoreColor = if (isAmbient) AMBIENT_SCORE else scoreColorActive
    val setTextColor = if (isAmbient) AMBIENT_DIM else SCORE_DIM
    val serveBgColor = if (isAmbient) Color.Transparent else OLED_BLACK
    val serveLeftFill = if (isAmbient) Color.Transparent else SERVE_LEFT
    val serveRightFill = if (isAmbient) Color.Transparent else SERVE_RIGHT
    val serveLeftBorder = if (isAmbient) AMBIENT_SERVE_LEFT else Color.Transparent
    val serveRightBorder = if (isAmbient) AMBIENT_SERVE_RIGHT else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(serveBgColor)
            // Tap + long-press — only active when not in ambient
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
                color = setTextColor,
                fontSize = setFontSz,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            ScoreDisplayAmbient(
                score = state.leftScore,
                color = scoreColor,
                isServing = whoServes == 0,
                isAmbient = isAmbient,
                serveLeftFill = serveLeftFill,
                serveRightFill = serveRightFill,
                serveLeftBorder = serveLeftBorder,
                serveRightBorder = serveRightBorder,
                servesRight = servesRight,
                modifier = Modifier.weight(1f),
                scoreFontSize = scoreFontSz,
                dotSize = dotSz,
                dotGap = dotGapSz,
                dotPlaceholder = dotPhSz
            )

            // In ambient mode, hide the solid serve bar entirely
            if (!isAmbient) {
                ServeIndicator(
                    isSelfServing = whoServes == 0,
                    servesRight = servesRight,
                    modifier = Modifier.fillMaxWidth().height(barH)
                )
            } else {
                // Ambient: thin dim line separator instead
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AMBIENT_DIM)
                )
            }

            ScoreDisplayAmbient(
                score = state.rightScore,
                color = scoreColor,
                isServing = whoServes == 1,
                isAmbient = isAmbient,
                serveLeftFill = serveLeftFill,
                serveRightFill = serveRightFill,
                serveLeftBorder = serveLeftBorder,
                serveRightBorder = serveRightBorder,
                servesRight = servesRight,
                modifier = Modifier.weight(1f),
                scoreFontSize = scoreFontSz,
                dotSize = dotSz,
                dotGap = dotGapSz,
                dotPlaceholder = dotPhSz
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
                color = if (!isAmbient && state.leftSetWins > state.rightSetWins) SERVE_LEFT else setTextColor,
                fontSize = winsFontSz, fontWeight = FontWeight.Bold
            )
            Text(text = "-", color = setTextColor, fontSize = winsFontSz)
            Text(
                text = "${state.rightSetWins}",
                color = if (!isAmbient && state.rightSetWins > state.leftSetWins) SERVE_RIGHT else setTextColor,
                fontSize = winsFontSz, fontWeight = FontWeight.Bold
            )
        }
    }

    // ---- Overlays (only shown when NOT in ambient) ----

    if (!isAmbient && state.isEditMode) {
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

    if (!isAmbient && isSideSwitch) {
        val isSetEnd = state.needsSetEndSwitch
        SideSwitchDialog(
            afterSet = isSetEnd,
            setNumber = state.currentSet,
            onConfirm = { viewModel.confirmSideSwitch() }
        )
    }

    if (!isAmbient && state.fsmState == FsmState.FINISHED) {
        MatchFinishedDialog(
            leftSetWins = state.leftSetWins,
            rightSetWins = state.rightSetWins,
            onNewMatch = onNewMatch
        )
    }
}

// ─── Ambient-aware ScoreDisplay composable ──────────────────────

/**
 * Score display box for one side. In ambient mode:
 *  - Score text uses dimmed gray instead of bright white
 *  - Serve dot becomes a hollow ring (outline only) instead of solid fill
 *  - Critical/game-point colors desaturate in ambient
 */
@Composable
private fun ScoreDisplayAmbient(
    score: Int,
    color: Color,
    isServing: Boolean,
    isAmbient: Boolean,
    serveLeftFill: Color,
    serveRightFill: Color,
    serveLeftBorder: Color,
    serveRightBorder: Color,
    servesRight: Boolean,
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
                if (isAmbient) {
                    // ── Ambient: hollow outline ring instead of solid dot ──
                    Box(
                        modifier = Modifier
                            .size(dotSize + 2.dp)
                            .border(1.5.dp, if (servesRight) serveRightBorder else serveLeftBorder, CircleShape)
                    )
                } else {
                    // ── Interactive: solid colored dot ──
                    Box(
                        Modifier
                            .size(dotSize)
                            .background(
                                color = if (servesRight) serveRightFill else serveLeftFill,
                                shape = CircleShape
                            )
                    )
                }
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

// ─── Reused composables ─────────────────────────────────────────

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