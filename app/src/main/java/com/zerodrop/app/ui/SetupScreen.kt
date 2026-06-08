package com.zerodrop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerodrop.app.ui.theme.*

/**
 * Pre-game setup screen:
 *  - Game mode: single set (1局) or best-of-3 (3局2胜)
 *  - Score limit: 11, 15, or 21
 *  - Start button
 */
@Composable
fun SetupScreen(onStartMatch: (scoreLimit: Int, totalSets: Int) -> Unit) {
    val limits = listOf(11, 15, 21)
    var selectedLimit by remember { mutableIntStateOf(21) }
    var selectedMode by remember { mutableIntStateOf(3) } // 3 = best-of-3, 1 = single set
    val scrollState = rememberScrollState()

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
            Spacer(modifier = Modifier.weight(0.12f))

            Text(
                text = "ZeroDrop",
                color = SCORE_WHITE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // ---- Game mode selector ----
            Text(
                text = "比赛模式",
                color = SCORE_DIM,
                fontSize = 11.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(1 to "单局", 3 to "3局2胜").forEach { (mode, label) ->
                    val isSelected = mode == selectedMode
                    Box(
                        modifier = Modifier
                            .width(68.dp)
                            .height(34.dp)
                            .background(
                                color = if (isSelected) SERVE_RIGHT else SCORE_DIM.copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedMode = mode },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) SCORE_WHITE else SCORE_DIM,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ---- Score limit selector ----
            Text(
                text = "计分上限",
                color = SCORE_DIM,
                fontSize = 11.sp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                limits.forEach { limit ->
                    val isSelected = limit == selectedLimit
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(36.dp)
                            .background(
                                color = if (isSelected) SERVE_LEFT else SCORE_DIM.copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedLimit = limit },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${limit}分",
                            color = if (isSelected) SCORE_WHITE else SCORE_DIM,
                            fontSize = 17.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onStartMatch(selectedLimit, selectedMode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "开始比赛",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(0.18f))
        }
    }
}