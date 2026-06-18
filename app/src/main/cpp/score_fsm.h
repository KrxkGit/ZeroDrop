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
    int serveSide;    // 发球信息 [bit1:who(0=self,1=opponent)][bit0:court(0=left,1=right)]
    int currentSet;   // 当前局
    int leftSetWins;  // 大比分已胜
    int rightSetWins;
    int fsmState;     // FsmState 序列化
    int isGamePoint;  // 0/1 是否局点
    int isMatchPoint; // 0/1 是否赛点
    int needsSideSwitch;     // 0/1 是否需要换边
    int sideSwitchedThisSet;  // 0/1 本局是否已换边
    int needsSetEndSwitch;    // 0/1 本局结束后是否需要换边（非决胜局结束）
    int wearerHalf;   // 0/1 佩戴者当前左右半区 (0=left, 1=right)，双打用

    static constexpr int SNAPSHOT_SIZE = 14;

    // 序列化到 IntArray
    void toIntArray(int* out) const;
    // 从 IntArray 反序列化
    static GameSnapshot fromIntArray(const int* in);
};

class ScoreFsm {
public:
    ScoreFsm();

    // 初始化/重置 — totalSets: 1=单局, 3=三局两胜; initHalf: 佩戴者初始半区 0=left,1=right
    // 默认 -1 表示单打模式（不追踪半区）
    void init(int scoreLimit, int totalSets = 3, int initHalf = -1);
    void reset();

    // 加分操作
    bool scoreLeft();
    bool scoreRight();

    // 撤销
    bool undo();

    // 编辑模式
    void enterEditMode();
    bool setEditScores(int left, int right, int serveSide, int wearerHalf = -1);
    void confirmEdit();

    // 换边确认
    void confirmSideSwitch();

    // 状态查询
    FsmState state() const { return m_state; }
    const GameSnapshot& currentSnapshot() const { return m_snapshot; }

    // 获取完整快照
    void getStateIntArray(int* out) const;
    // 从快照恢复
    void restoreFromIntArray(const int* in);

    // 序列化整个历史栈
    std::string serialize() const;
    bool deserialize(const std::string& data);

    // 历史栈大小
    size_t historySize() const { return m_history.size(); }

    // 导出比赛数据用于二维码生成（多局）
    // setsData: 多局数据，格式 "set1_data;set2_data;..."
    //   每局数据：[首球发球方1=己方,0=对方][初始位置1=右区,0=左区][得分序列]
    //   得分序列：1=己方得分, 0=对方得分
    // 返回：用 | 分隔的多局数据
    static std::string exportMatchData(const std::string& setsData);

    // 判断是否需要在局中换边（决胜局中途半场分）
    static bool shouldSwitchSides(int scoreLimit, int left, int right,
                                   int currentSet, int totalSets, bool alreadySwitched);
    // 判断是否需要在局结束后换边（每局结束后，除最后一局）
    static bool shouldSwitchAfterSet(int currentSet, int totalSets);
    // 判断是否局点/赛点
    static bool isGamePoint(int scoreLimit, int left, int right);
    static bool isMatchPoint(int scoreLimit, int left, int right,
                              int leftSetWins, int rightSetWins, int setsToWin);

private:
    FsmState m_state;
    GameSnapshot m_snapshot;
    std::vector<GameSnapshot> m_history;

    int m_totalSets = 3;      // 1=单局, 3=三局两胜
    int m_setsToWin = 2;      // (m_totalSets + 1) / 2
    int m_initHalf = -1;      // 佩戴者初始半区，-1=单打，0=左，1=右

    void pushHistory();
    void recalculateMeta();
    bool isSetOver() const;
    void checkSetEnd();
};

} // namespace zerodrop

#endif // ZERODROP_SCORE_FSM_H
