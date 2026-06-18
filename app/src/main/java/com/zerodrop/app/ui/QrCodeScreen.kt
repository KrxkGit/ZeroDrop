package com.zerodrop.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * 赛后二维码展示界面
 * 展示二维码供手机扫码查看复盘数据
 */
@Composable
fun QrCodeScreen(
    matchData: String,
    leftScore: Int,
    rightScore: Int,
    onBack: () -> Unit
) {
    // 添加加载状态
    var isLoading by remember { mutableStateOf(true) }
    var qrBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 使用 LaunchedEffect 在后台生成二维码
    LaunchedEffect(matchData) {
        if (matchData.isNotEmpty()) {
            isLoading = true
            errorMessage = null
            try {
                qrBitmap = generateQrCode(matchData)
            } catch (e: Exception) {
                errorMessage = "二维码生成出错"
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "无数据"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 标题
            Text(
                text = "扫码查看复盘",
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 10.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 二维码
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (!isLoading && qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!,
                        contentDescription = "二维码",
                        modifier = Modifier.size(120.dp)
                    )
                } else if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                } else {
                    Text(
                        text = errorMessage ?: "暂无数据",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 返回按钮
            Button(
                onClick = onBack,
                modifier = Modifier
                    .width(120.dp)
                    .height(32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6),
                    contentColor = Color.White
                )
            ) {
                Text("返回首页", fontSize = 11.sp)
            }
        }
    }
}

/**
 * 生成二维码 ImageBitmap（优化版本）
 */
private fun generateQrCode(data: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        // 优化提示参数，减少计算量
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,  // 使用最低纠错级别
            EncodeHintType.MARGIN to 1,  // 减小边距
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val writer = QRCodeWriter()
        // 使用更小的尺寸提高性能
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 200, 200, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        // 优化像素遍历
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                pixels[rowOffset + x] = if (bitMatrix[x, y]) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
            }
        }

        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}