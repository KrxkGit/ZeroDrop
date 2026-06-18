package com.zerodrop.app

import android.app.Application
import android.content.Context
import android.os.PowerManager
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
    val isEditMode: Boolean = false,
    val wearerHalf: Int = -1      // 佩戴者当前半区 0=左,1=右,-1=单打
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val fsm = ScoreBridge()
    private val vibrationManager = VibrationManager(application)
    private val context = application
    private val ongoingActivity = OngoingActivityManager(application)

    // ── WakeLock for match-in-progress screen-on ──
    // Held while the FSM is PLAYING to prevent the screen from sleeping.
    // Released when paused (EDITING), switching sides, or finished.
    private val wakeLock: PowerManager.WakeLock by lazy {
        val pm = application.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        pm.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "ZeroDrop:matchWakeLock"
        )
    }

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // 每局数据跟踪（用于二维码导出）
    private data class SetData(
        val serveSelf: Boolean,      // 首球发球方
        val initialRight: Boolean,   // 初始位置（右区）
        val points: StringBuilder = StringBuilder()  // 得分序列
    )
    private val setsHistory = mutableListOf<SetData>()

    // ---- Persistence keys ----
    private val persistKey = stringPreferencesKey("fsm_serialized")

    init {
        loadPersistedState()
    }

    // ---- Public actions ----

    fun startMatch(scoreLimit: Int, totalSets: Int = 3, initHalf: Int = -1) {
        fsm.setup(scoreLimit, totalSets, initHalf)
        // 初始化第一局数据，不清除历史记录
        val whoServes = (1 shr 1 and 1) == 0  // serveSide = 1 表示己方发球右区
        val servesRight = (1 and 1) == 1
        setsHistory.add(SetData(
            serveSelf = true,
            initialRight = servesRight
        ))
        refreshState()
        persistState()
        onMatchStateChanged()
    }

    fun newSetStarted() {
        // 新的一局开始，初始化新一局数据
        val currentServeSide = _uiState.value.serveSide
        val whoServes = (currentServeSide shr 1 and 1) == 0
        val servesRight = (currentServeSide and 1) == 1
        setsHistory.add(SetData(
            serveSelf = whoServes,
            initialRight = servesRight
        ))
    }

    fun scoreLeft() {
        val success = fsm.scoreLeft()
        if (success) {
            // 记录得分到当前局
            if (setsHistory.isNotEmpty()) {
                setsHistory.last().points.append("1")
            }
            vibrationManager.feedbackSelfScore()
            val snap = refreshState()
            if (snap.isGamePoint != 0 || snap.isMatchPoint != 0) {
                vibrationManager.feedbackCriticalPoint()
            }
            persistState()
            onMatchStateChanged()
        }
    }

    fun scoreRight() {
        val success = fsm.scoreRight()
        if (success) {
            // 记录得分到当前局
            if (setsHistory.isNotEmpty()) {
                setsHistory.last().points.append("0")
            }
            vibrationManager.feedbackOpponentScore()
            val snap = refreshState()
            if (snap.isGamePoint != 0 || snap.isMatchPoint != 0) {
                vibrationManager.feedbackCriticalPoint()
            }
            persistState()
            onMatchStateChanged()
        }
    }

    fun undo() {
        val success = fsm.undo()
        if (success) {
            vibrationManager.feedbackUndo()
            refreshState()
            persistState()
            onMatchStateChanged()
        }
    }

    fun enterEditMode() {
        fsm.enterEditMode()
        vibrationManager.feedbackEditMode()
        refreshState()
        onMatchStateChanged()
    }

    fun setEditScores(left: Int, right: Int, serveSide: Int, wearerHalf: Int = -1) {
        fsm.setEditScores(left, right, serveSide, wearerHalf)
        refreshState()
    }

    fun confirmEdit() {
        fsm.confirmEdit()
        refreshState()
        persistState()
        onMatchStateChanged()
    }

    fun confirmSideSwitch() {
        fsm.confirmSideSwitch()
        vibrationManager.feedbackCriticalPoint()
        refreshState()
        persistState()
        onMatchStateChanged()
    }

    /**
     * Called when the user exits the Scoring screen (new match, back to setup).
     * Releases all active system resources: WakeLock, Ongoing Activity.
     */
    fun onMatchEnded() {
        releaseWakeLock()
        ongoingActivity.stopMatch()
    }

    /** 导出比赛数据用于二维码生成 */
    fun exportMatchData(): String {
        // 生成多局数据字符串
        val setsData = StringBuilder()
        for (i in setsHistory.indices) {
            val set = setsHistory[i]
            // 格式: [首球发球方][初始位置][得分序列]
            val setData = "${if (set.serveSelf) '1' else '0'}${if (set.initialRight) '1' else '0'}${set.points}"
            setsData.append(setData)
            if (i < setsHistory.size - 1) {
                setsData.append(";")  // 用 ; 分隔各局数据
            }
        }
        val result = fsm.exportMatchData(setsData.toString())
        return result.ifEmpty { "EMPTY_DATA" } // 防止空数据
    }

    /** 生成历史数据二维码 URL */
    fun getHistoryQrCodeUrl(): String {
        val data = exportMatchData()
        return "${BuildConfig.QR_TARGET_URL}?m=$data"
    }

    /** 是否有历史比赛数据 */
    fun hasMatchHistory(): Boolean = setsHistory.any { it.points.isNotEmpty() }

    /** 清除历史数据 */
    fun clearHistory() {
        setsHistory.clear()
        _currentSet = 1
    }

    /** 生成二维码 URL */
    fun getQrCodeUrl(): String {
        val data = exportMatchData()
        return "${BuildConfig.QR_TARGET_URL}?m=$data"
    }

    // ---- Internal ----

    private var _currentSet = 1
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
                isEditMode = FsmState.fromCode(snap.fsmState) == FsmState.EDITING,
                wearerHalf = snap.wearerHalf
            )
        }

        // 检测新局开始
        if (snap.currentSet > _currentSet) {
            newSetStarted()
            _currentSet = snap.currentSet
        }

        return snap
    }

    /**
     * React to FSM state transitions:
     *  - PLAYING → acquire WakeLock, start/update Ongoing Activity
     *  - SIDE_SWITCH / EDITING → release WakeLock (screen can dim), keep Ongoing Activity
     *  - FINISHED → release everything
     */
    private fun onMatchStateChanged() {
        val state = _uiState.value
        when (state.fsmState) {
            FsmState.PLAYING -> {
                acquireWakeLock()
                ongoingActivity.updateMatchStatus(state.leftScore, state.rightScore, state.currentSet)
            }
            FsmState.SIDE_SWITCH, FsmState.EDITING -> {
                releaseWakeLock() // Allow screen to dim during pause
                ongoingActivity.updateMatchStatus(state.leftScore, state.rightScore, state.currentSet)
            }
            FsmState.FINISHED -> {
                releaseWakeLock()
                ongoingActivity.stopMatch()
            }
            FsmState.SETUP -> {
                // Match hasn't started yet — no action
            }
        }
    }

    private fun acquireWakeLock() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire(10 * 60 * 1000L) // 10 min timeout, refreshed each score
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
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
        releaseWakeLock()
        ongoingActivity.stopMatch()
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