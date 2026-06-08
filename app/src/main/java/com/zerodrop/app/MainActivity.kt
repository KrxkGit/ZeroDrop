package com.zerodrop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerodrop.app.ui.ScoringScreen
import com.zerodrop.app.ui.SetupScreen
import com.zerodrop.app.ui.theme.ZeroDropTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ZeroDropTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Setup) }
                var scoreLimit by remember { mutableIntStateOf(21) }

                when (screen) {
                    Screen.Setup -> {
                        SetupScreen(
                            onStartMatch = { limit ->
                                scoreLimit = limit
                                screen = Screen.Scoring
                            }
                        )
                    }
                    Screen.Scoring -> {
                        val viewModel: GameViewModel = viewModel(
                            factory = GameViewModel.Factory(application)
                        )

                        // Start the match with configured limit on first composition
                        LaunchedEffect(Unit) {
                            viewModel.startMatch(scoreLimit)
                        }

                        ScoringScreen(
                            onNewMatch = {
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

private enum class Screen {
    Setup,
    Scoring
}
