# ZeroDrop Web 赛后复盘

ZeroDrop 羽毛球计分手表端的配套 Web 应用，用于展示比赛复盘数据。

## 功能特性

- **赛果卡片**：显示最终比分和胜负状态
- **气势走势图**：使用 ECharts 绘制比分变化曲线
- **战术分析**：
  - 最大连续得分
  - 领先交替次数
  - 关键转折点识别

## 本地开发

```bash
cd web
npm install
npm run dev
```

访问 http://localhost:5173 查看效果。

## 构建

```bash
npm run build
```

构建产物输出到 `dist/` 目录。

## 部署

推送到 `main` 分支后，GitHub Actions 会自动部署到 GitHub Pages。

访问地址：`https://your-username.github.io/ZeroDrop/zerodrop-web/`

## 数据格式

URL 参数 `?m=` 包含比赛数据，格式为：

```
Config(2位) + History(变长0/1字符串)
```

- Config 第1位：首球发球方（1=己方，0=对方）
- Config 第2位：初始位置（1=右区，0=左区）
- History：1=己方得分，0=对方得分

示例：`11100110` 表示：
- 己方发球，右区起始
- 得分顺序：己方-己方-己方-对方-对方-己方-己方-对方