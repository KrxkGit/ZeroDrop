package com.zerodrop.app

/**
 * JNI bridge to the C++ ScoreFsm.
 *
 * The native layer returns state via IntArray snapshots with 11 elements:
 *   [0] leftScore     [1] rightScore    [2] scoreLimit    [3] serveSide
 *   [4] currentSet    [5] leftSetWins   [6] rightSetWins  [7] fsmState
 *   [8] isGamePoint   [9] isMatchPoint  [10] needsSideSwitch
 */
class ScoreBridge {

    private var nativePtr: Long = 0

    init {
        System.loadLibrary("score_fsm")
        nativePtr = nativeInit()
    }

    fun setup(scoreLimit: Int) = nativeSetup(scoreLimit)

    fun scoreLeft(): Boolean = nativeScoreLeft()
    fun scoreRight(): Boolean = nativeScoreRight()
    fun undo(): Boolean = nativeUndo()

    fun enterEditMode() = nativeEnterEditMode()
    fun setEditScores(left: Int, right: Int, serveSide: Int): Boolean =
        nativeSetEditScores(left, right, serveSide)
    fun confirmEdit() = nativeConfirmEdit()
    fun confirmSideSwitch() = nativeConfirmSideSwitch()

    fun getStateSnapshot(): IntArray {
        val arr = IntArray(GameSnapshot.SNAPSHOT_SIZE)
        nativeGetStateSnapshot(arr)
        return arr
    }

    fun restoreState(snapshot: IntArray) = nativeRestoreState(snapshot)

    fun serialize(): String = nativeSerialize()
    fun deserialize(data: String): Boolean = nativeDeserialize(data)

    fun isGamePoint(): Boolean = nativeIsGamePoint()
    fun getHistorySize(): Int = nativeGetHistorySize()

    fun dispose() {
        if (nativePtr != 0L) {
            nativeDestroy()
            nativePtr = 0
        }
    }

    protected fun finalize() {
        dispose()
    }

    // Native methods
    private external fun nativeInit(): Long
    private external fun nativeDestroy()
    private external fun nativeSetup(scoreLimit: Int)
    private external fun nativeScoreLeft(): Boolean
    private external fun nativeScoreRight(): Boolean
    private external fun nativeUndo(): Boolean
    private external fun nativeEnterEditMode()
    private external fun nativeSetEditScores(left: Int, right: Int, serveSide: Int): Boolean
    private external fun nativeConfirmEdit()
    private external fun nativeConfirmSideSwitch()
    private external fun nativeGetStateSnapshot(outArray: IntArray)
    private external fun nativeRestoreState(inArray: IntArray)
    private external fun nativeSerialize(): String
    private external fun nativeDeserialize(data: String): Boolean
    private external fun nativeIsGamePoint(): Boolean
    private external fun nativeGetHistorySize(): Int
}
