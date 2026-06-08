package com.zerodrop.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "zerodrop_state")

/**
 * UI-facing game state, a simplified view over the C++ FSM snapshot.
 */
data class GameUiState(
    val leftScore: Int = 0,
    val rightScore: Int = 0,
    val scoreLimit: Int = 21,
    val serveSide: Int = 0,       // 发球信息 [bit1:who(0=self,1=opponent)][bit0:court(0=left,1=right)]
    val currentSet: Int = 1,
    val leftSetWins: Int = 0,
    val rightSetWins: Int = 0,
    val fsmState: FsmState = FsmState.SETUP,
    val isGamePoint: Boolean = false,
    val isMatchPoint: Boolean = false,
    val needsSideSwitch: Boolean = false,
    val needsSetEndSwitch: Boolean = false,
    val canUndo: Boolean = false,
    val isEditMode: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val fsm = ScoreBridge()
    private val vibrationManager = VibrationManager(application)
    private val context = application

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // ---- Persistence keys ----
    private val persistKey = stringPreferencesKey("fsm_serialized")

    init {
        loadPersistedState()
    }

    // ---- Public actions ----

    fun startMatch(scoreLimit: Int, totalSets: Int = 3) {
        fsm.setup(scoreLimit, totalSets)
        refreshState()
        persistState()
    }

    fun scoreLeft() {
        val success = fsm.scoreLeft()
        if (success) {
            vibrationManager.feedbackSelfScore()
            val snap = refreshState()
            if (snap.isGamePoint != 0 || snap.isMatchPoint != 0) {
                vibrationManager.feedbackCriticalPoint()
            }
            persistState()
        }
    }

    fun scoreRight() {
        val success = fsm.scoreRight()
        if (success) {
            vibrationManager.feedbackOpponentScore()
            val snap = refreshState()
            if (snap.isGamePoint != 0 || snap.isMatchPoint != 0) {
                vibrationManager.feedbackCriticalPoint()
            }
            persistState()
        }
    }

    fun undo() {
        val success = fsm.undo()
        if (success) {
            vibrationManager.feedbackUndo()
            refreshState()
            persistState()
        }
    }

    fun enterEditMode() {
        fsm.enterEditMode()
        vibrationManager.feedbackEditMode()
        refreshState()
    }

    fun setEditScores(left: Int, right: Int, serveSide: Int) {
        fsm.setEditScores(left, right, serveSide)
        refreshState()
    }

    fun confirmEdit() {
        fsm.confirmEdit()
        refreshState()
        persistState()
    }

    fun confirmSideSwitch() {
        fsm.confirmSideSwitch()
        vibrationManager.feedbackCriticalPoint()
        refreshState()
        persistState()
    }

    // ---- Internal ----

    private fun refreshState(): GameSnapshot {
        val arr = fsm.getStateSnapshot()
        val snap = GameSnapshot.fromIntArray(arr)
        val canUndo = fsm.getHistorySize() > 0
        _uiState.update {
            GameUiState(
                leftScore = snap.leftScore,
                rightScore = snap.rightScore,
                scoreLimit = snap.scoreLimit,
                serveSide = snap.serveSide,
                currentSet = snap.currentSet,
                leftSetWins = snap.leftSetWins,
                rightSetWins = snap.rightSetWins,
                fsmState = FsmState.fromCode(snap.fsmState),
                isGamePoint = snap.isGamePoint != 0,
                isMatchPoint = snap.isMatchPoint != 0,
                needsSideSwitch = snap.needsSideSwitch != 0,
                needsSetEndSwitch = snap.needsSetEndSwitch != 0,
                canUndo = canUndo,
                isEditMode = FsmState.fromCode(snap.fsmState) == FsmState.EDITING
            )
        }
        return snap
    }

    private fun persistState() {
        viewModelScope.launch {
            val data = fsm.serialize()
            context.dataStore.edit { prefs ->
                prefs[persistKey] = data
            }
        }
    }

    private fun loadPersistedState() {
        viewModelScope.launch {
            val stored = context.dataStore.data.first()[persistKey]
            if (!stored.isNullOrEmpty()) {
                fsm.deserialize(stored)
            }
            refreshState()
        }
    }

    override fun onCleared() {
        // Persist one last time before VM dies
        persistState()
        fsm.dispose()
        super.onCleared()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(application) as T
        }
    }
}
