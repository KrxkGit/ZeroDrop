package com.zerodrop.app.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * Ambient (微光) mode state, consumed by UI composables.
 *
 * When [isAmbient] is true (PRD §4.3):
 *  - All smooth animations must be disabled
 *  - Serve indicator: solid fill → hollow outline
 *  - Score colors: pure white → dimmed gray
 *  - Refresh rate: reduced (system-handled via AmbientModeSupport)
 *  - Only essential info shown (scores + serve outline)
 */
data class AmbientUiState(
    val isAmbient: Boolean = false
)

/** CompositionLocal key for ambient-aware UI rendering. */
val LocalAmbientState = compositionLocalOf { AmbientUiState() }