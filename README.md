# ZeroDrop 🏸

Wear OS 独立羽毛球计分应用。纯黑 OLED 界面、盲操手势、C++ 状态机核心。

## 项目结构

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/zerodrop/app/
│   │   ├── MainActivity.kt          # 入口 Activity
│   │   ├── GameViewModel.kt         # 状态管理 + DataStore 持久化
│   │   ├── ScoreBridge.kt           # JNI 桥接层
│   │   ├── GameSnapshot.kt          # 快照数据模型
│   │   ├── VibrationManager.kt      # 触觉反馈管理
│   │   └── ui/
│   │       ├── ScoringScreen.kt     # 计分主界面
│   │       ├── SetupScreen.kt       # 赛前设置页
│   │       ├── EditModeOverlay.kt   # 编辑模式覆盖层
│   │       └── theme/
│   │           ├── Color.kt
│   │           └── Theme.kt
│   └── cpp/
│       ├── CMakeLists.txt
│       ├── score_fsm.h              # C++ FSM 头文件
│       ├── score_fsm.cpp            # C++ FSM 实现
│       └── score_jni.cpp            # JNI 桥接实现
└── build.gradle.kts
```

## 构建与运行

1. 打开项目到 Android Studio (Hedgehog+)
2. 同步 Gradle
3. 选择 Wear OS 模拟器或真实手表
4. Run `app`

```bash
./gradlew assembleDebug
```

## 计分规则

- **三局两胜**：先赢两局者获胜
- **计分上限**：可选 11 / 15 / 21 分
- **领先两分**：必须领先对手 2 分才能赢局
- **封顶 30 分**：先到 30 分者赢该局（无领先要求）
- **换边**：决胜局任意一方达到半场上限时触发换边提醒
- **发球权**：总分偶数为左方发球，奇数为右方发球

## 手势映射

| 操作 | 手势 | 反馈 |
|---|---|---|
| 己方加分 | 右滑 | 短促单次震动 |
| 对方加分 | 左滑 | 连续两次震动 |
| 撤销 | 下滑 | 长连续震动 |
| 编辑模式 | 长按 2s | 高频短震 |
| 局点/赛点 | 自动 | 强效警示震动 |
