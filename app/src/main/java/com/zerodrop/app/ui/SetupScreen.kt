package com.zerodrop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerodrop.app.ui.theme.*

/**
 * Pre-game setup screen. Minimal config:
 *  - Score limit selector (11, 15, or 21)
 *  - Start button
 */
@Composable
fun SetupScreen(onStartMatch: (Int) -> Unit) {
    val limits = listOf(11, 15, 21)
    var selectedLimit by remember { mutableIntStateOf(21) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLED_BLACK),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "ZeroDrop",
                color = SCORE_WHITE,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "计分上限",
                color = SCORE_DIM,
                fontSize = 14.sp
            )

            // Score limit selector row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                limits.forEach { limit ->
                    val isSelected = limit == selectedLimit
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = if (isSelected) SERVE_LEFT else SCORE_DIM.copy(alpha = 0.2f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$limit",
                            color = if (isSelected) SCORE_WHITE else SCORE_DIM,
                            fontSize = 24.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onStartMatch(selectedLimit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "开始比赛",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
