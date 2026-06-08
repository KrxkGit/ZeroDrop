#ifndef ZERODROP_SCORE_FSM_H
#define ZERODROP_SCORE_FSM_H

#include <cstdint>
#include <vector>
#include <string>

namespace zerodrop {

enum class FsmState {
    SETUP,       // 赛前配置
    PLAYING,     // 比赛中
    EDITING,     // 编辑模式
    SIDE_SWITCH, // 换边阻塞
    FINISHED     // 比赛结束
};

enum class ServeSide {
    LEFT = 0,
    RIGHT = 1
};

struct GameSnapshot {
    int leftScore;    // 己方分数
    int rightScore;   // 对方分数
    int scoreLimit;   // 计分上限
    int serveSide;    // 发球方 (0=左, 1=右)
    int currentSet;   // 当前局
    int leftSetWins;  // 大比分已胜
    int rightSetWins;
    int fsmState;     // FsmState 序列化
    int isGamePoint;  // 0/1 是否局点
    int isMatchPoint; // 0/1 是否赛点
    int needsSideSwitch; // 0/1 是否需要换边

    static constexpr int SNAPSHOT_SIZE = 11;

    // 序列化到 IntArray
    void toIntArray(int* out) const;
    // 从 IntArray 反序列化
    static GameSnapshot fromIntArray(const int* in);
};

class ScoreFsm {
public:
    ScoreFsm();

    // 初始化/重置
    void init(int scoreLimit);
    void reset();

    // 加分操作
    bool scoreLeft();
    bool scoreRight();

    // 撤销
    bool undo();

    // 编辑模式
    void enterEditMode();
    bool setEditScores(int left, int right, int serveSide);
    void confirmEdit();

    // 换边确认
    void confirmSideSwitch();

    // 状态查询
    FsmState state() const { return m_state; }
    const GameSnapshot& currentSnapshot() const { return m_snapshot; }

    // 获取完整快照 (11个int)
    void getStateIntArray(int* out) const;
    // 从快照恢复
    void restoreFromIntArray(const int* in);

    // 序列化整个历史栈
    std::string serialize() const;
    bool deserialize(const std::string& data);

    // 历史栈大小
    size_t historySize() const { return m_history.size(); }

    // 判断是否需要换边
    static bool shouldSwitchSides(int scoreLimit, int left, int right);
    // 判断是否局点/赛点
    static bool isGamePoint(int scoreLimit, int left, int right);
    static bool isMatchPoint(int scoreLimit, int left, int right, int leftSetWins, int rightSetWins, int setsToWin);

private:
    FsmState m_state;
    GameSnapshot m_snapshot;
    std::vector<GameSnapshot> m_history;

    static constexpr int SETS_TO_WIN = 2; // 三局两胜

    void pushHistory();
    void recalculateMeta();
    bool isSetOver() const;
    void checkSetEnd();
};

} // namespace zerodrop

#endif // ZERODROP_SCORE_FSM_H
