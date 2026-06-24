package com.zerodrop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerodrop.app.GameViewModel
import com.zerodrop.app.ui.theme.*

/**
 * Pre-game setup screen:
 *  - Game mode: single set (1局) or best-of-3 (3局2胜)
 *  - Score limit: 11, 15, or 21
 *  - Doubles position: left / right starting half
 *  - Start button
 */
@Composable
fun SetupScreen(
    onStartMatch: (scoreLimit: Int, totalSets: Int, initHalf: Int) -> Unit,
    onShowQrCode: (data: String) -> Unit = {},
    viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory(LocalContext.current.applicationContext as android.app.Application))
) {
    val limits = listOf(11, 15, 21)
    var selectedLimit by remember { mutableIntStateOf(21) }
    var selectedMode by remember { mutableIntStateOf(3) }
    var selectedHalf by remember { mutableIntStateOf(-1) }  // -1=not set, 0=left, 1=right
    var prefsLoaded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val ambient = LocalAmbientState.current
    val hasHistory = viewModel.hasMatchHistory()
    var showClearConfirm by remember { mutableStateOf(false) }

    // 首次打开时加载用户上一次的赛制偏好
    LaunchedEffect(Unit) {
        if (!prefsLoaded) {
            viewModel.loadSetupPreferences().let { prefs ->
                selectedLimit = prefs.scoreLimit
                selectedMode = prefs.totalSets
                selectedHalf = prefs.initHalf
            }
            prefsLoaded = true
        }
    }

    val textColor = if (ambient.isAmbient) AMBIENT_SCORE else SCORE_WHITE
    val dimColor = if (ambient.isAmbient) AMBIENT_DIM else SCORE_DIM

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLED_BLACK),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.08f))

            Text(
                text = "ZeroDrop",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // ---- Game mode selector ----
            Text(text = "比赛模式", color = dimColor, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(1 to "单局", 3 to "3局2胜").forEach { (mode, label) ->
                    val isSelected = mode == selectedMode
                    Box(
                        modifier = Modifier
                            .width(68.dp).height(34.dp)
                            .background(
                                if (isSelected) SERVE_RIGHT else dimColor.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedMode = mode },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (isSelected) SCORE_WHITE else dimColor,
                            fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // ---- Doubles position selector ----
            Text(text = "双打站位", color = dimColor, fontSize = 11.sp)
            Text(text = "开始前我在", color = dimColor, fontSize = 9.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left half
                val leftSelected = selectedHalf == 0
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (leftSelected) SERVE_LEFT else dimColor.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .border(
                            if (leftSelected) 2.dp else 0.dp,
                            if (leftSelected) SERVE_LEFT else Color.Transparent,
                            CircleShape
                        )
                        .clickable { selectedHalf = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text("左", color = if (leftSelected) SCORE_WHITE else dimColor,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Right half
                val rightSelected = selectedHalf == 1
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (rightSelected) SERVE_RIGHT else dimColor.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .border(
                            if (rightSelected) 2.dp else 0.dp,
                            if (rightSelected) SERVE_RIGHT else Color.Transparent,
                            CircleShape
                        )
                        .clickable { selectedHalf = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text("右", color = if (rightSelected) SCORE_WHITE else dimColor,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // ---- Score limit selector ----
            Text(text = "计分上限", color = dimColor, fontSize = 11.sp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                limits.forEach { limit ->
                    val isSelected = limit == selectedLimit
                    Box(
                        modifier = Modifier
                            .width(100.dp).height(36.dp)
                            .background(
                                if (isSelected) SERVE_LEFT else dimColor.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedLimit = limit },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${limit}分", color = if (isSelected) SCORE_WHITE else dimColor,
                            fontSize = 17.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    viewModel.saveSetupPreferences(selectedLimit, selectedMode, selectedHalf)
                    onStartMatch(selectedLimit, selectedMode, selectedHalf)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("开始比赛", fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onShowQrCode(viewModel.getHistoryQrCodeUrl())
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6),
                    contentColor = Color.White
                )
            ) {
                Text("生成历史二维码", fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            if (hasHistory) {
                TextButton(
                    onClick = {
                        showClearConfirm = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF9CA3AF)
                    )
                ) {
                    Text("清除历史记录", fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.weight(0.12f))
        }
    }

    // 清除历史记录确认对话框
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清除") },
            text = { Text("确定清除所有历史比赛记录？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("清除", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消", color = dimColor)
                }
            }
        )
    }
}
