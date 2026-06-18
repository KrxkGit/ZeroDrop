<template>
  <div>
    <!-- WeChat 浏览器提示 -->
    <div v-if="isWechat" class="wechat-banner">
      <span class="wechat-icon">💡</span>
      <span class="wechat-text">微信内无法保存截图，请点击右上角 <strong>「在浏览器打开」</strong> 或长按保存</span>
    </div>

    <!-- 顶部操作栏 -->
    <div class="action-bar">
      <span class="action-bar-title">赛后复盘</span>
      <div class="action-bar-buttons">
        <button v-if="isWechat" class="copy-btn" @click="copyLink">复制链接</button>
        <button v-if="!isWechat" class="save-btn" @click="saveScreenshot">保存截图</button>
      </div>
    </div>
    <div ref="captureRef" class="review-container">
      <!-- 截图标题区 -->
      <div class="screenshot-header">
        <div class="header-brand">ZeroDrop</div>
        <div class="header-subtitle">赛后复盘</div>
        <div class="header-score" v-if="finalScore.left > 0 || finalScore.right > 0">
          {{ finalScore.left }} : {{ finalScore.right }}
        </div>
      </div>

      <!-- 模块 A：全局赛果卡片 -->
      <div class="card result-card">
        <div class="set-results" v-if="setScores.length > 1">
          <div v-for="(set, index) in setScores" :key="index" class="set-item">
            <span class="set-number">第{{ index + 1 }}局</span>
            <span class="set-score">{{ set.left }} : {{ set.right }}</span>
            <span class="set-result" :class="{ 'win': set.left > set.right }">
              {{ set.left > set.right ? '胜' : '负' }}
            </span>
          </div>
        </div>

        <div class="score-display">
          <span class="score-number">{{ finalScore.left }}</span>
          <span class="score-separator">:</span>
          <span class="score-number">{{ finalScore.right }}</span>
        </div>
        <div :class="['status-badge', winStatus]">
          {{ winStatusText }}
        </div>
      </div>

      <!-- 模块 B：气势走势图 -->
      <div class="card chart-card" v-if="states.length > 0">
        <h3 class="card-title">气势走势</h3>
        <v-chart :option="chartOption" style="height: 280px" autoresize />
      </div>

      <!-- 模块 C：分局统计 -->
      <div v-if="setStats.length > 1" class="card set-stats-card">
        <h3 class="card-title">分局统计</h3>
        <div class="set-stats-grid">
          <div v-for="(stat, index) in setStats" :key="index" class="set-stat-item">
            <div class="set-header">
              <span class="set-number">第{{ index + 1 }}局</span>
              <span class="set-score">{{ stat.setScore.left }} : {{ stat.setScore.right }}</span>
            </div>
            <div class="set-details">
              <span>总得分: {{ stat.totalPoints }}</span>
              <span>最大分差: {{ stat.maxDiff }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 模块 D：深度战术分析 -->
      <div class="card analysis-card">
        <h3 class="card-title">战术分析</h3>

        <div class="stats-grid">
          <div class="stat-item">
            <span class="stat-label">我方最高连得</span>
            <span class="stat-value">{{ maxConsecutive.left }}分</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">对方最高连得</span>
            <span class="stat-value">{{ maxConsecutive.right }}分</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">领先交替</span>
            <span class="stat-value">{{ leadChangeCount }}次</span>
          </div>
        </div>

        <div v-if="keyPoints.length > 0" class="key-points">
          <h4 class="key-points-title">关键转折点</h4>
          <div v-for="(point, i) in keyPoints" :key="i" class="key-point">
            {{ point }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, GridSimpleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { MatchState } from '../utils/matchParser'
import { parseMultiSetData, parseMatchData } from '../utils/matchParser'
import { useAnalysis } from '../composables/useAnalysis'
import html2canvas from 'html2canvas'

use([CanvasRenderer, LineChart, TooltipComponent, GridComponent, GridSimpleComponent])

const props = defineProps<{
  data: string
  isWechat?: boolean
}>()

const captureRef = ref<HTMLElement | null>(null)

/** 复制完整链接（微信内使用，方便用户在浏览器打开） */
async function copyLink() {
  const url = `${window.location.origin}${window.location.pathname}?m=${encodeURIComponent(props.data)}`
  try {
    await navigator.clipboard.writeText(url)
    // 简单提示：短暂修改按钮文字
    const btn = document.querySelector('.copy-btn') as HTMLButtonElement
    if (btn) {
      const original = btn.textContent
      btn.textContent = '已复制 ✓'
      setTimeout(() => { btn.textContent = original }, 2000)
    }
  } catch {
    // 降级：选中文本手动复制
    const input = document.createElement('textarea')
    input.value = url
    input.style.cssText = 'position:fixed;left:-9999px'
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    const btn = document.querySelector('.copy-btn') as HTMLButtonElement
    if (btn) {
      const original = btn.textContent
      btn.textContent = '已复制 ✓'
      setTimeout(() => { btn.textContent = original }, 2000)
    }
  }
}

async function saveScreenshot() {
  const el = captureRef.value
  if (!el) return
  try {
    const canvas = await html2canvas(el, {
      backgroundColor: '#0f0f0f',
      scale: 2,
      useCORS: true,
      logging: false
    })
    const link = document.createElement('a')
    link.download = `zerodrop-复盘-${new Date().toISOString().slice(0, 10)}.png`
    link.href = canvas.toDataURL()
    link.click()
  } catch (e) {
    console.error('截图失败', e)
  }
}

// 解析比赛数据（自动检测单局/多局）
const states = computed(() => {
  if (!props.data) return []
  return parseMatchData(props.data)
})

// 计算各局比分
const setScores = computed(() => {
  const sets: Array<{ left: number; right: number }> = []
  let currentSetLeft = 0
  let currentSetRight = 0

  for (const state of states.value) {
    // 检测新局开始
    if (state.setScore.left === 0 && state.setScore.right === 0 &&
        (currentSetLeft > 0 || currentSetRight > 0)) {
      sets.push({ left: currentSetLeft, right: currentSetRight })
      currentSetLeft = 0
      currentSetRight = 0
    }
    currentSetLeft = state.setScore.left
    currentSetRight = state.setScore.right
  }

  // 添加最后一局
  if (currentSetLeft > 0 || currentSetRight > 0) {
    sets.push({ left: currentSetLeft, right: currentSetRight })
  }

  return sets
})

// 各局统计
const setStats = computed(() => {
  const stats: Array<{
    setScore: { left: number; right: number }
    totalPoints: number
    maxDiff: number
  }> = []
  for (const state of states.value) {
    if (state.setScore.left === 0 && state.setScore.right === 0) continue
    const setIndex = state.setNumber - 1
    if (!stats[setIndex]) {
      stats[setIndex] = {
        setScore: { left: 0, right: 0 },
        totalPoints: 0,
        maxDiff: 0
      }
    }
    const setStat = stats[setIndex]
    setStat.setScore.left = state.setScore.left
    setStat.setScore.right = state.setScore.right
    setStat.totalPoints = state.setScore.left + state.setScore.right
    setStat.maxDiff = Math.max(setStat.maxDiff, Math.abs(state.diff))
  }
  return stats
})

// 最终比分
const finalScore = computed(() => {
  if (states.value.length === 0) return { left: 0, right: 0 }
  const last = states.value[states.value.length - 1]
  return last.score
})

// 胜负状态
const winStatus = computed(() => {
  return finalScore.value.left > finalScore.value.right ? 'win' : 'lose'
})

const winStatusText = computed(() => {
  return winStatus.value === 'win' ? 'WIN' : 'LOSE'
})

// 战术分析
const { maxConsecutive, leadChangeCount, keyPoints } = useAnalysis(states)

// 图表配置
const chartOption = computed(() => {
  if (states.value.length === 0) return {}
  return {
    xAxis: {
      type: 'category',
      data: states.value.map((_, i) => i),
      show: false
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: 'rgba(255, 255, 255, 0.1)'
        }
      }
    },
    series: [{
      type: 'line',
      data: states.value.map(s => s.diff),
      smooth: true,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(59, 130, 246, 0.4)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ]
        }
      },
      lineStyle: { color: '#3b82f6', width: 3 },
      itemStyle: { color: '#3b82f6' },
      symbol: 'none',
      markLine: {
        silent: true,
        data: [{ yAxis: 0 }],
        lineStyle: { color: 'rgba(255, 255, 255, 0.3)', type: 'dashed' }
      }
    }],
    grid: { top: 20, bottom: 20, left: 30, right: 10 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        const index = data.dataIndex
        const state = states.value[index]
        if (!state) return ''
        const setInfo = setScores.value[state.setNumber - 1]
        return `第${state.setNumber}局: ${state.score.left} : ${state.score.right}${setInfo ? ` (局比分: ${setInfo.left} : ${setInfo.right})` : ''}`
      }
    }
  }
})
</script>

<style scoped>
.review-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 16px;
  padding-bottom: 100px;
}

.screenshot-header {
  text-align: center;
  padding: 24px 16px 16px;
  margin-bottom: 8px;
}

.header-brand {
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1px;
}

.header-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 4px;
}

.header-score {
  font-size: 36px;
  font-weight: 700;
  margin-top: 12px;
  color: #ffffff;
  font-variant-numeric: tabular-nums;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 600px;
  margin: 0 auto;
  padding: 12px 16px;
}

.action-bar-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.action-bar-buttons {
  display: flex;
  gap: 8px;
}

.save-btn {
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.save-btn:hover {
  background: #2563eb;
}

.save-btn:active {
  background: #1d4ed8;
}

/* WeChat 提示横幅 */
.wechat-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 600px;
  margin: 0 auto;
  padding: 12px 16px;
  background: rgba(251, 191, 36, 0.12);
  border-bottom: 1px solid rgba(251, 191, 36, 0.25);
}

.wechat-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.wechat-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.5;
}

.wechat-text strong {
  color: #fbbf24;
  font-weight: 600;
}

.copy-btn {
  background: transparent;
  color: #fbbf24;
  border: 1px solid rgba(251, 191, 36, 0.4);
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.copy-btn:hover {
  background: rgba(251, 191, 36, 0.1);
  border-color: rgba(251, 191, 36, 0.6);
}

.copy-btn:active {
  background: rgba(251, 191, 36, 0.2);
}

.card {
  background: #1a1a1a;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 16px 0;
  font-weight: 500;
}

/* 模块 A：赛果卡片 */
.result-card {
  text-align: center;
  padding: 32px 20px;
}

.set-results {
  margin-bottom: 20px;
}

.set-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #252525;
  border-radius: 8px;
  font-size: 14px;
}

.set-number {
  color: rgba(255, 255, 255, 0.6);
}

.set-score {
  font-weight: 600;
  color: #ffffff;
}

.set-result {
  padding: 4px 12px;
  border-radius: 6px;
  background: #374151;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.set-result.win {
  background: #3b82f6;
  color: white;
}

.score-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}

.score-number {
  font-size: 64px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.score-separator {
  font-size: 48px;
  color: rgba(255, 255, 255, 0.3);
}

.status-badge {
  display: inline-block;
  padding: 8px 24px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 2px;
}

.status-badge.win {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: white;
}

.status-badge.lose {
  background: linear-gradient(135deg, #ef4444, #b91c1c);
  color: white;
}

/* 模块 B：图表卡片 */
.chart-card {
  padding: 16px;
}

/* 模块 C：分局统计 */
.set-stats-card {
  padding: 16px;
}

.set-stats-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.set-stat-item {
  background: #252525;
  border-radius: 8px;
  padding: 12px;
}

.set-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.set-details {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

/* 模块 D：战术分析 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-item {
  background: #252525;
  border-radius: 8px;
  padding: 12px 8px;
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
}

.key-points {
  background: #252525;
  border-radius: 8px;
  padding: 16px;
}

.key-points-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 12px 0;
  font-weight: 500;
}

.key-point {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.key-point:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
</style>