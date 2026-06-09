package com.zerodrop.app.ui.theme

import androidx.compose.ui.graphics.Color

// Pure black for OLED power saving
val OLED_BLACK = Color(0xFF000000)

// Score colors
val SCORE_WHITE = Color(0xFFFFFFFF)
val SCORE_WARNING = Color(0xFFFFEB3B) // Yellow for game point
val SCORE_CRITICAL = Color(0xFFFF3D3D) // Red for match point
val SCORE_DIM = Color(0xFF888888)

// Serve indicators
val SERVE_LEFT = Color(0xFF4CAF50)  // Green
val SERVE_RIGHT = Color(0xFF2196F3) // Blue

// ── Ambient mode colors (PRD §4.3) ──
// In ambient mode: hide solid fills, use dimmer hollow outlines
val AMBIENT_BACKGROUND = Color(0xFF000000)       // pure black
val AMBIENT_SCORE = Color(0xFFCCCCCC)            // dimmed white for scores
val AMBIENT_SERVE_LEFT = Color(0xFF336633)       // dimmed green outline
val AMBIENT_SERVE_RIGHT = Color(0xFF1A3D5C)      // dimmed blue outline
val AMBIENT_SERVE_OUTLINE = Color(0xFF444444)    // for hollow ring in ambient
val AMBIENT_DIM = Color(0xFF555555)              // dimmed aux text
