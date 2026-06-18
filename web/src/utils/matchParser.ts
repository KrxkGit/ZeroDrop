/**
 * 单局比赛数据接口
 */
export interface SetData {
  serveSelf: boolean
  initialRight: boolean
  points: string
}

/**
 * 比赛状态接口
 */
export interface MatchState {
  score: { left: number; right: number }
  diff: number
  serveSide: 'left' | 'right'
  wearerHalf: 'left' | 'right'
  setNumber: number
  setScore: { left: number; right: number }
}

/**
 * 解析多局比赛数据
 * 数据格式：set1_data|set2_data|...
 *   每局数据：[首球发球方1=己方,0=对方][初始位置1=右区,0=左区][得分序列]
 *   得分序列：1=己方得分, 0=对方得分
 */
export function parseMultiSetData(data: string): MatchState[] {
  if (!data) return []

  const sets = data.split('|')
  const allStates: MatchState[] = []
  let totalLeftScore = 0
  let totalRightScore = 0
  let globalPointIndex = 0

  for (let setIndex = 0; setIndex < sets.length; setIndex++) {
    const setData = sets[setIndex]
    if (setData.length < 2) continue

    const setNumber = setIndex + 1
    const serveSelf = setData[0] === '1'
    const initialRight = setData[1] === '1'
    const history = setData.slice(2)

    let leftScore = 0
    let rightScore = 0

    // 添加当前局的初始状态
    if (setIndex === 0) {
      allStates.push({
        score: { left: 0, right: 0 },
        diff: 0,
        serveSide: serveSelf ? 'left' : 'right',
        wearerHalf: initialRight ? 'right' : 'left',
        setNumber,
        setScore: { left: 0, right: 0 }
      })
    }

    for (const point of history) {
      if (point === '1') {
        leftScore++
      } else if (point === '0') {
        rightScore++
      }

      const totalPoints = leftScore + rightScore
      const serveSide = (totalPoints % 2 === 0)
        ? (serveSelf ? 'left' : 'right')
        : (serveSelf ? 'right' : 'left')

      allStates.push({
        score: { left: totalLeftScore + leftScore, right: totalRightScore + rightScore },
        diff: leftScore - rightScore,
        serveSide,
        wearerHalf: initialRight ? 'right' : 'left',
        setNumber,
        setScore: { left: leftScore, right: rightScore }
      })
    }

    // 更新总分
    totalLeftScore += leftScore
    totalRightScore += rightScore

    // 如果不是最后一局，添加局间分隔
    if (setIndex < sets.length - 1) {
      allStates.push({
        score: { left: totalLeftScore, right: totalRightScore },
        diff: totalLeftScore - totalRightScore,
        serveSide: serveSelf ? 'left' : 'right',
        wearerHalf: initialRight ? 'right' : 'left',
        setNumber: setNumber + 1,
        setScore: { left: 0, right: 0 }
      })
    }
  }

  return allStates
}

/**
 * 向后兼容：解析单局比赛数据
 * 数据格式：Config(2位) + History(变长0/1字符串)
 *   Config: [首球发球方1=己方,0=对方][初始位置1=右区,0=左区]
 *   History: 1=己方得分, 0=对方得分
 */
export function parseMatchData(data: string): MatchState[] {
  // 检查是否是多局数据（包含|分隔符）
  if (data.includes('|')) {
    return parseMultiSetData(data)
  }

  // 否则作为单局数据处理
  if (!data || data.length < 2) return []

  const serveSelf = data[0] === '1'
  const initialRight = data[1] === '1'
  const history = data.slice(2)

  const states: MatchState[] = []
  let leftScore = 0
  let rightScore = 0

  // 初始状态
  states.push({
    score: { left: 0, right: 0 },
    diff: 0,
    serveSide: serveSelf ? 'left' : 'right',
    wearerHalf: initialRight ? 'right' : 'left',
    setNumber: 1,
    setScore: { left: 0, right: 0 }
  })

  for (const point of history) {
    if (point === '1') {
      leftScore++
    } else if (point === '0') {
      rightScore++
    }

    const totalPoints = leftScore + rightScore
    const serveSide = (totalPoints % 2 === 0)
      ? (serveSelf ? 'left' : 'right')
      : (serveSelf ? 'right' : 'left')

    states.push({
      score: { left: leftScore, right: rightScore },
      diff: leftScore - rightScore,
      serveSide,
      wearerHalf: initialRight ? 'right' : 'left',
      setNumber: 1,
      setScore: { left: leftScore, right: rightScore }
    })
  }

  return states
}