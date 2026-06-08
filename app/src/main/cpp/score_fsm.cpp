#include "score_fsm.h"
#include <algorithm>
#include <sstream>
#include <stdexcept>

namespace zerodrop {

// --- GameSnapshot ---

void GameSnapshot::toIntArray(int* out) const {
    out[0] = leftScore;
    out[1] = rightScore;
    out[2] = scoreLimit;
    out[3] = serveSide;
    out[4] = currentSet;
    out[5] = leftSetWins;
    out[6] = rightSetWins;
    out[7] = fsmState;
    out[8] = isGamePoint;
    out[9] = isMatchPoint;
    out[10] = needsSideSwitch;
}

GameSnapshot GameSnapshot::fromIntArray(const int* in) {
    GameSnapshot snap;
    snap.leftScore     = in[0];
    snap.rightScore    = in[1];
    snap.scoreLimit    = in[2];
    snap.serveSide     = in[3];
    snap.currentSet    = in[4];
    snap.leftSetWins   = in[5];
    snap.rightSetWins  = in[6];
    snap.fsmState      = in[7];
    snap.isGamePoint   = in[8];
    snap.isMatchPoint  = in[9];
    snap.needsSideSwitch = in[10];
    return snap;
}

// --- ScoreFsm ---

ScoreFsm::ScoreFsm() : m_state(FsmState::SETUP) {
    m_snapshot = GameSnapshot{0, 0, 21, 0, 1, 0, 0,
                              static_cast<int>(FsmState::SETUP), 0, 0, 0};
}

void ScoreFsm::init(int scoreLimit) {
    m_history.clear();
    m_state = FsmState::PLAYING;
    m_snapshot = GameSnapshot{0, 0, scoreLimit, 0, 1, 0, 0,
                              static_cast<int>(FsmState::PLAYING), 0, 0, 0};
}

void ScoreFsm::reset() {
    int limit = m_snapshot.scoreLimit;
    m_history.clear();
    m_state = FsmState::SETUP;
    m_snapshot = GameSnapshot{0, 0, limit, 0, 1, 0, 0,
                              static_cast<int>(FsmState::SETUP), 0, 0, 0};
}

void ScoreFsm::pushHistory() {
    m_history.push_back(m_snapshot);
}

bool ScoreFsm::scoreLeft() {
    if (m_state != FsmState::PLAYING) return false;
    if (m_snapshot.needsSideSwitch) return false;

    pushHistory();
    m_snapshot.leftScore++;

    // 计算发球权
    // 羽毛球发球规则：总分偶数为右侧（0），奇数为左侧（若双方分数和为偶数则右发球）
    int total = m_snapshot.leftScore + m_snapshot.rightScore;
    m_snapshot.serveSide = (total % 2 == 0) ? 0 : 1;

    // 局点/赛点检测
    recalculateMeta();

    checkSetEnd();
    return true;
}

bool ScoreFsm::scoreRight() {
    if (m_state != FsmState::PLAYING) return false;
    if (m_snapshot.needsSideSwitch) return false;

    pushHistory();
    m_snapshot.rightScore++;

    int total = m_snapshot.leftScore + m_snapshot.rightScore;
    m_snapshot.serveSide = (total % 2 == 0) ? 0 : 1;

    recalculateMeta();

    checkSetEnd();
    return true;
}

bool ScoreFsm::undo() {
    if (m_history.empty()) return false;
    if (m_state == FsmState::SIDE_SWITCH) return false;

    // 如果在编辑模式，先退出
    if (m_state == FsmState::EDITING) {
        m_state = FsmState::PLAYING;
    }

    m_snapshot = m_history.back();
    m_history.pop_back();

    // 恢复正确状态
    if (m_snapshot.fsmState == static_cast<int>(FsmState::SIDE_SWITCH)) {
        m_state = FsmState::SIDE_SWITCH;
    } else if (m_state == FsmState::FINISHED) {
        // 如果之前是结束状态，恢复到 playing
        m_state = FsmState::PLAYING;
        m_snapshot.fsmState = static_cast<int>(FsmState::PLAYING);
    } else {
        m_state = FsmState::PLAYING;
    }

    recalculateMeta();
    return true;
}

void ScoreFsm::enterEditMode() {
    if (m_state != FsmState::PLAYING) return;
    pushHistory();
    m_state = FsmState::EDITING;
    m_snapshot.fsmState = static_cast<int>(FsmState::EDITING);
}

bool ScoreFsm::setEditScores(int left, int right, int serveSide) {
    if (m_state != FsmState::EDITING) return false;
    if (left < 0 || right < 0 || serveSide < 0 || serveSide > 1) return false;

    m_snapshot.leftScore = left;
    m_snapshot.rightScore = right;
    m_snapshot.serveSide = serveSide;
    recalculateMeta();
    return true;
}

void ScoreFsm::confirmEdit() {
    if (m_state != FsmState::EDITING) return;
    m_state = FsmState::PLAYING;
    m_snapshot.fsmState = static_cast<int>(FsmState::PLAYING);
    checkSetEnd();
}

void ScoreFsm::confirmSideSwitch() {
    if (m_state != FsmState::SIDE_SWITCH) return;
    m_state = FsmState::PLAYING;
    m_snapshot.fsmState = static_cast<int>(FsmState::PLAYING);
    m_snapshot.needsSideSwitch = 0;
}

void ScoreFsm::getStateIntArray(int* out) const {
    m_snapshot.toIntArray(out);
}

void ScoreFsm::restoreFromIntArray(const int* in) {
    m_snapshot = GameSnapshot::fromIntArray(in);
    m_state = static_cast<FsmState>(m_snapshot.fsmState);
}

std::string ScoreFsm::serialize() const {
    // 格式: "scoreLimit,historySize:l0,r0,sv0,set0,lw0,rw0,...;l1,r1,..."
    std::ostringstream oss;
    oss << m_snapshot.scoreLimit << "," << m_history.size() << ";";

    // 当前快照
    oss << m_snapshot.leftScore << "," << m_snapshot.rightScore << ","
        << m_snapshot.serveSide << "," << m_snapshot.currentSet << ","
        << m_snapshot.leftSetWins << "," << m_snapshot.rightSetWins << ","
        << m_snapshot.fsmState << ","
        << m_snapshot.isGamePoint << "," << m_snapshot.isMatchPoint << ","
        << m_snapshot.needsSideSwitch;

    // 历史栈
    for (const auto& snap : m_history) {
        oss << ":";
        oss << snap.leftScore << "," << snap.rightScore << ","
            << snap.serveSide << "," << snap.currentSet << ","
            << snap.leftSetWins << "," << snap.rightSetWins << ","
            << snap.fsmState << ","
            << snap.isGamePoint << "," << snap.isMatchPoint << ","
            << snap.needsSideSwitch;
    }

    return oss.str();
}

bool ScoreFsm::deserialize(const std::string& data) {
    try {
        std::istringstream iss(data);
        std::string header;
        std::getline(iss, header, ';');

        // Parse header
        auto comma1 = header.find(',');
        if (comma1 == std::string::npos) return false;
        int limit = std::stoi(header.substr(0, comma1));
        size_t historySize = std::stoul(header.substr(comma1 + 1));

        // Parse current snapshot
        std::string snapStr;
        std::getline(iss, snapStr, ':');
        std::istringstream snapStream(snapStr);
        int vals[11];
        for (int i = 0; i < 10; ++i) {
            std::string token;
            std::getline(snapStream, token, ',');
            vals[i] = std::stoi(token);
        }
        vals[10] = 0; // default needsSideSwitch

        m_snapshot = GameSnapshot{
            vals[0], vals[1], limit,
            vals[2], vals[3],
            vals[4], vals[5],
            vals[6],
            vals[7], vals[8],
            vals[9]
        };
        m_state = static_cast<FsmState>(m_snapshot.fsmState);

        // Parse history
        m_history.clear();
        m_history.reserve(historySize);
        std::string histItem;
        while (std::getline(iss, histItem, ':')) {
            std::istringstream histStream(histItem);
            int hvals[10];
            for (int i = 0; i < 10; ++i) {
                std::string token;
                std::getline(histStream, token, ',');
                hvals[i] = std::stoi(token);
            }
            m_history.push_back(GameSnapshot{
                hvals[0], hvals[1], limit,
                hvals[2], hvals[3],
                hvals[4], hvals[5],
                hvals[6],
                hvals[7], hvals[8],
                hvals[9]
            });
        }

        return true;
    } catch (...) {
        return false;
    }
}

void ScoreFsm::recalculateMeta() {
    int limit = m_snapshot.scoreLimit;
    int left = m_snapshot.leftScore;
    int right = m_snapshot.rightScore;

    m_snapshot.isGamePoint = isGamePoint(limit, left, right) ? 1 : 0;
    m_snapshot.isMatchPoint = isMatchPoint(limit, left, right,
                                           m_snapshot.leftSetWins,
                                           m_snapshot.rightSetWins,
                                           SETS_TO_WIN) ? 1 : 0;
    m_snapshot.needsSideSwitch = shouldSwitchSides(limit, left, right) ? 1 : 0;
}

bool ScoreFsm::isSetOver() const {
    int limit = m_snapshot.scoreLimit;
    int left = m_snapshot.leftScore;
    int right = m_snapshot.rightScore;

    // 至少打到 limit 分，且领先 2 分
    if (left >= limit && left - right >= 2) return true;
    if (right >= limit && right - left >= 2) return true;
    // 封顶 30 分（先到 30 分者胜）
    if (left >= 30 && left > right) return true;
    if (right >= 30 && right > left) return true;
    return false;
}

void ScoreFsm::checkSetEnd() {
    if (!isSetOver()) {
        // 检查换边
        if (m_snapshot.needsSideSwitch) {
            m_state = FsmState::SIDE_SWITCH;
            m_snapshot.fsmState = static_cast<int>(FsmState::SIDE_SWITCH);
        }
        return;
    }

    // 本局结束
    if (m_snapshot.leftScore > m_snapshot.rightScore) {
        m_snapshot.leftSetWins++;
    } else {
        m_snapshot.rightSetWins++;
    }

    // 检查是否赢得比赛 (三局两胜)
    if (m_snapshot.leftSetWins >= SETS_TO_WIN ||
        m_snapshot.rightSetWins >= SETS_TO_WIN) {
        m_state = FsmState::FINISHED;
        m_snapshot.fsmState = static_cast<int>(FsmState::FINISHED);
        return;
    }

    // 开始新一局
    m_snapshot.currentSet++;
    m_snapshot.leftScore = 0;
    m_snapshot.rightScore = 0;
    m_snapshot.serveSide = 0;
    recalculateMeta();
}

bool ScoreFsm::shouldSwitchSides(int scoreLimit, int left, int right) {
    // 决胜局（第三局）中任意一方达到 11 分时换边
    int total = left + right;
    if (total == 0) return false;

    // 简化逻辑：在决胜局中，当总分达到 scoreLimit/2 时换边
    // 标准规则：第三局 11 分换边（对于 21 分制）
    // 这里用通用的方式：当任何一方达到 (limit/2 向上取整) 时触发换边
    // 仅在 set 3（决胜局）触发
    // 实际通过 currentSet 判断：在 recalculateMeta 中需要更多上下文
    // 对外暴露一个简单的检测函数，由上层传入 set 信息
    int halfLimit = (scoreLimit + 1) / 2;
    return (left >= halfLimit || right >= halfLimit);
}

bool ScoreFsm::isGamePoint(int scoreLimit, int left, int right) {
    // 任意一方达到 limit-1 或更高但未结束
    return (left >= scoreLimit - 1 || right >= scoreLimit - 1);
}

bool ScoreFsm::isMatchPoint(int scoreLimit, int left, int right,
                             int leftSetWins, int rightSetWins, int setsToWin) {
    // 任意一方赛点: 已经赢了一局并且当前局到达局点
    if (leftSetWins >= setsToWin - 1 && left >= scoreLimit - 1) return true;
    if (rightSetWins >= setsToWin - 1 && right >= scoreLimit - 1) return true;
    return false;
}

} // namespace zerodrop
