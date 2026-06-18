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
    out[11] = sideSwitchedThisSet;
    out[12] = needsSetEndSwitch;
    out[13] = wearerHalf;
}

GameSnapshot GameSnapshot::fromIntArray(const int* in) {
    GameSnapshot snap;
    snap.leftScore             = in[0];
    snap.rightScore            = in[1];
    snap.scoreLimit            = in[2];
    snap.serveSide             = in[3];
    snap.currentSet            = in[4];
    snap.leftSetWins           = in[5];
    snap.rightSetWins          = in[6];
    snap.fsmState              = in[7];
    snap.isGamePoint           = in[8];
    snap.isMatchPoint          = in[9];
    snap.needsSideSwitch       = in[10];
    snap.sideSwitchedThisSet   = (SNAPSHOT_SIZE > 11) ? in[11] : 0;
    snap.needsSetEndSwitch     = (SNAPSHOT_SIZE > 12) ? in[12] : 0;
    snap.wearerHalf            = (SNAPSHOT_SIZE > 13) ? in[13] : 0;
    return snap;
}

// --- ScoreFsm ---

ScoreFsm::ScoreFsm() : m_state(FsmState::SETUP) {
    m_snapshot = GameSnapshot{0, 0, 21, 1, 1, 0, 0,
                              static_cast<int>(FsmState::SETUP), 0, 0, 0, 0, 0, 0};
}

void ScoreFsm::init(int scoreLimit, int totalSets, int initHalf) {
    m_history.clear();
    m_state = FsmState::PLAYING;
    m_totalSets = totalSets;
    m_setsToWin = (totalSets + 1) / 2;
    m_initHalf = initHalf;
    int wh = (initHalf < 0) ? 0 : initHalf;  // doubles: initHalf; singles: 0 (unused)
    m_snapshot = GameSnapshot{0, 0, scoreLimit, 1, 1, 0, 0,
                              static_cast<int>(FsmState::PLAYING), 0, 0, 0, 0, 0, wh};
}

void ScoreFsm::reset() {
    int limit = m_snapshot.scoreLimit;
    m_history.clear();
    m_state = FsmState::SETUP;
    m_initHalf = -1;
    m_snapshot = GameSnapshot{0, 0, limit, 1, 1, 0, 0,
                              static_cast<int>(FsmState::SETUP), 0, 0, 0, 0, 0, 0};
}

void ScoreFsm::pushHistory() {
    m_history.push_back(m_snapshot);
}

bool ScoreFsm::scoreLeft() {
    if (m_state != FsmState::PLAYING) return false;
    if (m_snapshot.needsSideSwitch) return false;

    pushHistory();

    // ── 双打: 摘下得分前"我方是否已持发球权" ──
    bool weWereServing = ((m_snapshot.serveSide >> 1) & 1) == 0;

    m_snapshot.leftScore++;

    // ── 双打佩戴者半区追踪 ──
    // 我方持球得分（retain serve）→ 搭档互换半区，佩戴者始终 toggle
    if (m_initHalf >= 0 && weWereServing) {
        m_snapshot.wearerHalf = 1 - m_snapshot.wearerHalf;
    }

    // 计算发球方 + 站位
    // 己方得分 → 己方发球 (who=0)
    // 发球方得分为偶数 → 右半区发球，奇数 → 左半区发球
    // serveSide 编码：[bit1:who(0=self,1=opponent)][bit0:court(0=left,1=right)]
    int courtL = (m_snapshot.leftScore % 2 == 0) ? 1 : 0;
    m_snapshot.serveSide = (0 << 1) | courtL;  // self serves, court=0/1

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

    // 对方得分 → 对方发球 (who=1)
    int courtR = (m_snapshot.rightScore % 2 == 0) ? 1 : 0;
    m_snapshot.serveSide = (1 << 1) | courtR;  // opponent serves, court=0/1

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
    // Don't push history here — entering edit mode is not a scoring action.
    // The actual score change happens when user confirms edit.
    m_state = FsmState::EDITING;
    m_snapshot.fsmState = static_cast<int>(FsmState::EDITING);
}

bool ScoreFsm::setEditScores(int left, int right, int serveSide, int wearerHalf) {
    if (m_state != FsmState::EDITING) return false;
    if (left < 0 || right < 0 || serveSide < 0 || serveSide > 3) return false;

    m_snapshot.leftScore = left;
    m_snapshot.rightScore = right;
    m_snapshot.serveSide = serveSide;
    // 编辑中可以修正佩戴者半区位置
    if (wearerHalf >= 0) {
        m_snapshot.wearerHalf = wearerHalf;
    }
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

    bool wasSetEndSwitch = (m_snapshot.needsSetEndSwitch != 0);

    m_state = FsmState::PLAYING;
    m_snapshot.fsmState = static_cast<int>(FsmState::PLAYING);
    m_snapshot.needsSideSwitch = 0;
    m_snapshot.needsSetEndSwitch = 0;
    m_snapshot.sideSwitchedThisSet = 1;

    // Any side switch (set-end or mid-set) flips the wearer's half
    if (m_initHalf >= 0) {
        m_snapshot.wearerHalf = 1 - m_snapshot.wearerHalf;
    }

    if (wasSetEndSwitch) {
        // End-of-set switch: advance to next set now
        m_snapshot.currentSet++;
        if (m_snapshot.currentSet > m_totalSets) {
            m_state = FsmState::FINISHED;
            m_snapshot.fsmState = static_cast<int>(FsmState::FINISHED);
            return;
        }
        m_snapshot.leftScore = 0;
        m_snapshot.rightScore = 0;
        m_snapshot.serveSide = 1;  // self serves, right court
        m_snapshot.sideSwitchedThisSet = 0;
        recalculateMeta();
    }
}

void ScoreFsm::getStateIntArray(int* out) const {
    m_snapshot.toIntArray(out);
}

void ScoreFsm::restoreFromIntArray(const int* in) {
    m_snapshot = GameSnapshot::fromIntArray(in);
    m_state = static_cast<FsmState>(m_snapshot.fsmState);
}

std::string ScoreFsm::serialize() const {
    // 格式: "scoreLimit,totalSets,historySize,initHalf;..."
    std::ostringstream oss;
    oss << m_snapshot.scoreLimit << "," << m_totalSets << "," << m_history.size() << "," << m_initHalf << ";";

    // 当前快照 (13 fields)
    oss << m_snapshot.leftScore << "," << m_snapshot.rightScore << ","
        << m_snapshot.serveSide << "," << m_snapshot.currentSet << ","
        << m_snapshot.leftSetWins << "," << m_snapshot.rightSetWins << ","
        << m_snapshot.fsmState << ","
        << m_snapshot.isGamePoint << "," << m_snapshot.isMatchPoint << ","
        << m_snapshot.needsSideSwitch << ","
        << m_snapshot.sideSwitchedThisSet << ","
        << m_snapshot.wearerHalf;

    // 历史栈
    for (const auto& snap : m_history) {
        oss << ":";
        oss << snap.leftScore << "," << snap.rightScore << ","
            << snap.serveSide << "," << snap.currentSet << ","
            << snap.leftSetWins << "," << snap.rightSetWins << ","
            << snap.fsmState << ","
            << snap.isGamePoint << "," << snap.isMatchPoint << ","
            << snap.needsSideSwitch << ","
            << snap.sideSwitchedThisSet << ","
            << snap.wearerHalf;
    }

    return oss.str();
}

bool ScoreFsm::deserialize(const std::string& data) {
    try {
        std::istringstream iss(data);
        std::string header;
        std::getline(iss, header, ';');

        // Parse header: "scoreLimit,totalSets,historySize[,initHalf]"
        auto comma1 = header.find(',');
        if (comma1 == std::string::npos) return false;
        int limit = std::stoi(header.substr(0, comma1));
        auto comma2 = header.find(',', comma1 + 1);
        int totalSets = 3;
        size_t historySize = 0;
        m_initHalf = -1;
        if (comma2 != std::string::npos) {
            auto comma3 = header.find(',', comma2 + 1);
            totalSets = std::stoi(header.substr(comma1 + 1, comma2 - comma1 - 1));
            if (comma3 != std::string::npos) {
                // New format: scoreLimit,totalSets,historySize,initHalf
                historySize = std::stoul(header.substr(comma2 + 1, comma3 - comma2 - 1));
                m_initHalf = std::stoi(header.substr(comma3 + 1));
            } else {
                // intermediate format: scoreLimit,totalSets,historySize
                historySize = std::stoul(header.substr(comma2 + 1));
            }
        } else {
            // Old format: scoreLimit,historySize
            historySize = std::stoul(header.substr(comma1 + 1));
        }
        m_totalSets = totalSets;
        m_setsToWin = (totalSets + 1) / 2;

        // Parse current snapshot (backwards-compat: old format had 11 fields:
        //   leftScore,rightScore,serveSide,currentSet,leftSetWins,rightSetWins,
        //   fsmState,isGamePoint,isMatchPoint,needsSideSwitch,sideSwitchedThisSet
        // New format appends wearerHalf at index 11)
        std::string snapStr;
        std::getline(iss, snapStr, ':');
        std::istringstream snapStream(snapStr);
        int vals[14] = {0};
        int fieldCount = 0;
        for (int i = 0; i < 14; ++i) {
            std::string token;
            if (!std::getline(snapStream, token, ',')) break;
            vals[i] = std::stoi(token);
            fieldCount++;
        }
        int needsSideSwitchVal = (fieldCount >= 10) ? vals[9] : 0;
        int sideSwitchedVal    = (fieldCount >= 11) ? vals[10] : 0;
        int wearerHalfVal      = (fieldCount >= 12) ? vals[11] : 0;

        m_snapshot = GameSnapshot{
            vals[0], vals[1], limit,
            vals[2], vals[3],
            vals[4], vals[5],
            vals[6],
            vals[7], vals[8],
            needsSideSwitchVal, sideSwitchedVal,
            0,  // needsSetEndSwitch — not in text serialization, recomputed later
            wearerHalfVal
        };
        m_state = static_cast<FsmState>(m_snapshot.fsmState);

        // Parse history (same layout as current snapshot)
        m_history.clear();
        m_history.reserve(historySize);
        std::string histItem;
        while (std::getline(iss, histItem, ':')) {
            std::istringstream histStream(histItem);
            int hvals[14] = {0};
            int hfieldCount = 0;
            for (int i = 0; i < 14; ++i) {
                std::string token;
                if (!std::getline(histStream, token, ',')) break;
                hvals[i] = std::stoi(token);
                hfieldCount++;
            }
            int hSideSwitchVal = (hfieldCount >= 10) ? hvals[9] : 0;
            int hSideSwitched  = (hfieldCount >= 11) ? hvals[10] : 0;
            int hWearerVal     = (hfieldCount >= 12) ? hvals[11] : 0;
            m_history.push_back(GameSnapshot{
                hvals[0], hvals[1], limit,
                hvals[2], hvals[3],
                hvals[4], hvals[5],
                hvals[6],
                hvals[7], hvals[8],
                hSideSwitchVal, hSideSwitched,
                0,  // needsSetEndSwitch
                hWearerVal
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
                                           m_setsToWin) ? 1 : 0;
    m_snapshot.needsSideSwitch = shouldSwitchSides(limit, left, right, m_snapshot.currentSet, m_totalSets, m_snapshot.sideSwitchedThisSet != 0) ? 1 : 0;
    m_snapshot.needsSetEndSwitch = shouldSwitchAfterSet(m_snapshot.currentSet, m_totalSets) ? 1 : 0;
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

    // 检查是否赢得比赛
    if (m_snapshot.leftSetWins >= m_setsToWin ||
        m_snapshot.rightSetWins >= m_setsToWin) {
        m_state = FsmState::FINISHED;
        m_snapshot.fsmState = static_cast<int>(FsmState::FINISHED);
        return;
    }

    // Set over, match not over → side switch after every set (BWF rule)
    m_snapshot.needsSetEndSwitch = 1;
    m_state = FsmState::SIDE_SWITCH;
    m_snapshot.fsmState = static_cast<int>(FsmState::SIDE_SWITCH);
}

bool ScoreFsm::shouldSwitchSides(int scoreLimit, int left, int right,
                                  int currentSet, int totalSets, bool alreadySwitched) {
    // Never switch in a single-set match
    if (totalSets <= 1) return false;
    // Only switch in the deciding set:
    //   best-of-3 → only set 3 (currentSet == 3)
    //   best-of-5 → only set 5, etc.
    if (currentSet < totalSets) return false;
    if (alreadySwitched) return false;

    int total = left + right;
    if (total == 0) return false;

    int halfLimit = (scoreLimit + 1) / 2; // e.g. 8 for 15-pt, 11 for 21-pt
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

bool ScoreFsm::shouldSwitchAfterSet(int currentSet, int totalSets) {
    // Side switch after every set EXCEPT the final one
    return (totalSets > 1 && currentSet < totalSets);
}

std::string ScoreFsm::exportMatchData(const std::string& setsData) {
    // setsData 格式: "set1_data;set2_data;..."
    // 每局数据: [首球发球方][初始位置][得分序列]
    // 需要把 ; 换成 | 返回
    std::string result = setsData;
    // 将所有 ; 替换为 |
    for (size_t i = 0; i < result.size(); ++i) {
        if (result[i] == ';') {
            result[i] = '|';
        }
    }
    return result;
}

} // namespace zerodrop
