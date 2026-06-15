package com.zerodrop.app

/**
 * Snapshots mirror the C++ GameSnapshot::SNAPSHOT_SIZE (= 14) layout.
 */
data class GameSnapshot(
    val leftScore: Int,
    val rightScore: Int,
    val scoreLimit: Int,
    val serveSide: Int,
    val currentSet: Int,
    val leftSetWins: Int,
    val rightSetWins: Int,
    val fsmState: Int,
    val isGamePoint: Int,
    val isMatchPoint: Int,
    val needsSideSwitch: Int,
    val sideSwitchedThisSet: Int,
    val needsSetEndSwitch: Int,
    val wearerHalf: Int
) {
    companion object {
        const val SNAPSHOT_SIZE = 14

        fun fromIntArray(arr: IntArray): GameSnapshot {
            require(arr.size >= 11)
            return GameSnapshot(
                arr[0], arr[1], arr[2], arr[3], arr[4],
                arr[5], arr[6], arr[7], arr[8], arr[9], arr[10],
                if (arr.size > 11) arr[11] else 0,
                if (arr.size > 12) arr[12] else 0,
                if (arr.size > 13) arr[13] else 0
            )
        }
    }
}

/** Map the C++ FsmState enum. */
enum class FsmState(val code: Int) {
    SETUP(0),
    PLAYING(1),
    EDITING(2),
    SIDE_SWITCH(3),
    FINISHED(4);

    companion object {
        fun fromCode(code: Int): FsmState = entries.firstOrNull { it.code == code } ?: SETUP
    }
}
