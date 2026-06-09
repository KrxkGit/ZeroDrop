package com.zerodrop.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientModeSupport
import com.zerodrop.app.ui.AmbientUiState
import com.zerodrop.app.ui.LocalAmbientState
import com.zerodrop.app.ui.ScoringScreen
import com.zerodrop.app.ui.SetupScreen
import com.zerodrop.app.ui.theme.ZeroDropTheme

/**
 * Main entry point for ZeroDrop on Wear OS.
 *
 * Implements [AmbientModeSupport.AmbientCallbackProvider] to receive
 * ambient-mode lifecycle events (enter/exit/update). The ambient state
 * is bridged to Compose via [LocalAmbientState] for ambient-aware UI.
 */
class MainActivity : FragmentActivity(),
    AmbientModeSupport.AmbientCallbackProvider {

    private val ambientController: AmbientModeSupport.AmbientController by lazy {
        AmbientModeSupport.attach(this)
    }

    // Mutable ambient flag — updated by callbacks, read by Compose
    private val isAmbient = mutableStateOf(false)

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                isAmbient.value = true
            }
            override fun onUpdateAmbient() {
                // Update burn-in protection offsets if needed
            }
            override fun onExitAmbient() {
                isAmbient.value = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Touch the controller to trigger fragment attachment
        ambientController

        setContent {
            val ambient by isAmbient

            // ── Screen-on management ──
            // Keep screen on while app is in the foreground.
            // Ambient mode takes over when the watch dims — FLAG_KEEP_SCREEN_ON
            // keeps the display awake in interactive mode; the system handles
            // always-on display (AOD) automatically via AmbientModeSupport.
            DisposableEffect(Unit) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            CompositionLocalProvider(
                LocalAmbientState provides AmbientUiState(isAmbient = ambient)
            ) {
                ZeroDropTheme {
                    var screen by remember { mutableStateOf<Screen>(Screen.Setup) }
                    var scoreLimit by remember { mutableIntStateOf(21) }
                    var totalSets by remember { mutableIntStateOf(3) }

                    when (screen) {
                        Screen.Setup -> {
                            SetupScreen(
                                onStartMatch = { limit, sets ->
                                    scoreLimit = limit
                                    totalSets = sets
                                    screen = Screen.Scoring
                                }
                            )
                        }
                        Screen.Scoring -> {
                            val viewModel: GameViewModel = viewModel(
                                factory = GameViewModel.Factory(application)
                            )

                            LaunchedEffect(Unit) {
                                viewModel.startMatch(scoreLimit, totalSets)
                            }

                            ScoringScreen(
                                onNewMatch = {
                                    viewModel.onMatchEnded()
                                    screen = Screen.Setup
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class Screen {
    Setup,
    Scoring
}