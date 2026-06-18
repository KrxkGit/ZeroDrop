import { computed } from 'vue'
import type { MatchState } from '../utils/matchParser'

/**
 * 计算最大连续得分
 */
function calculateMaxConsecutive(states: MatchState[], side: 'left' | 'right'): number {
  let max = 0
  let current = 0

  for (let i = 1; i < states.length; i++) {
    const prev = states[i - 1].score
    const curr = states[i].score

    if (curr[side] > prev[side]) {
      current++
      max = Math.max(max, current)
    } else {
      current = 0
    }
  }

  return max
}

/**
 * 计算领先交替次数
 */
function calculateLeadChanges(states: MatchState[]): number {
  let count = 0
  let lastLeader: 'left' | 'right' | 'tie' = 'tie'

  for (const state of states) {
    const currentLeader = state.diff > 0 ? 'left' : state.diff < 0 ? 'right' : 'tie'

    if (currentLeader !== 'tie' && currentLeader !== lastLeader) {
      count++
      lastLeader = currentLeader
    }
  }

  return count
}

/**
 * 识别关键转折点
 */
function identifyKeyPoints(states: MatchState[]): string[] {
  const points: string[] = []
  let streakStart: { side: 'left' | 'right', score: string } | null = null
  let streakCount = 0

  for (let i = 1; i < states.length; i++) {
    const prev = states[i - 1].score
    const curr = states[i].score

    if (curr.left > prev.left) {
      // 己方得分
      if (streakStart?.side === 'left') {
        streakCount++
      } else {
        streakStart = { side: 'left', score: `${prev.left}:${prev.right}` }
        streakCount = 1
      }
    } else if (curr.right > prev.right) {
      // 对方得分
      if (streakStart?.side === 'right') {
        streakCount++
      } else {
        streakStart = { side: 'right', score: `${prev.left}:${prev.right}` }
        streakCount = 1
      }
    }

    // 检查 streak 是否被中断
    if (streakStart && streakCount >= 3) {
      const winner = streakStart.side === 'left' ? '我方' : '对方'
      points.push(`在 ${streakStart.score} 后，${winner}连得 ${streakCount} 分`)
      streakStart = null
      streakCount = 0
    }
  }

  return points
}

/**
 * 战术分析 Composable
 */
export function useAnalysis(statesRef: import('vue').ComputedRef<MatchState[]>) {
  const maxConsecutive = computed(() => ({
    left: calculateMaxConsecutive(statesRef.value, 'left'),
    right: calculateMaxConsecutive(statesRef.value, 'right')
  }))

  const leadChangeCount = computed(() => calculateLeadChanges(statesRef.value))

  const keyPoints = computed(() => identifyKeyPoints(statesRef.value))

  return { maxConsecutive, leadChangeCount, keyPoints }
}