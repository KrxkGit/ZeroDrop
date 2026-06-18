<template>
  <div v-if="renderError" class="empty-state">
    <div class="empty-icon">⚠️</div>
    <h2 class="empty-title">解析出错</h2>
    <p class="empty-subtitle">数据格式有误，请检查</p>
    <div class="error-detail">{{ renderError }}</div>
    <button class="back-button" @click="renderError = ''; matchData = ''">返回</button>
  </div>
  <Review v-else-if="matchData" :data="matchData" :is-wechat="isWechat" />
  <div v-else class="empty-state" @drop.prevent="handleDrop" @dragover.prevent>
    <div class="empty-icon">🏸</div>
    <h2 class="empty-title">ZeroDrop 赛后复盘</h2>
    <p class="empty-subtitle">上传二维码截图或扫码查看比赛数据</p>

    <div class="action-grid">
      <label class="action-card upload-card">
        <input
          type="file"
          accept="image/*"
          capture="environment"
          class="file-input"
          @change="handleFileSelect"
        />
        <span class="action-icon">📷</span>
        <span class="action-text">上传图片</span>
      </label>

      <label class="action-card manual-card">
        <span class="action-icon">✏️</span>
        <span class="action-text">手动输入</span>
        <input
          v-model="manualInput"
          placeholder="粘贴数据字符串..."
          class="manual-input"
          @keyup.enter="submitManual"
        />
      </label>
    </div>

    <p class="empty-hint">
      请使用手机相机扫描手表上的二维码，或在微信中<span class="hint-highlight">复制链接</span>后在浏览器中打开
    </p>

    <div v-if="error" class="error-msg">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, onErrorCaptured } from 'vue'
import Review from './views/Review.vue'
import jsQR from 'jsqr'

const matchData = ref('')
const manualInput = ref('')
const error = ref('')
const renderError = ref('')

// WeChat browser detection
const isWechat = ref(false)

function detectWechat(): boolean {
  const ua = navigator.userAgent.toLowerCase()
  return ua.includes('micromessenger')
}

onErrorCaptured((err) => {
  renderError.value = String(err)
  return false
})

onMounted(() => {
  isWechat.value = detectWechat()

  const params = new URLSearchParams(window.location.search)
  let data = params.get('m')

  if (data) {
    // Persist to sessionStorage for same-browser recovery
    try { sessionStorage.setItem('zerodrop_m', data) } catch { /* ignore */ }

    // In WeChat, store data in URL hash — this survives "在浏览器打开"
    // because WeChat preserves the hash fragment when switching to system browser
    if (isWechat.value) {
      location.hash = encodeURIComponent(data)
    }
  } else {
    // No query param — try hash fallback (survives WeChat → Browser transition)
    if (location.hash && location.hash.length > 1) {
      try {
        const decoded = decodeURIComponent(location.hash.slice(1))
        if (decoded) {
          data = decoded
          location.hash = '' // clean up
        }
      } catch { /* ignore */ }
    }
    // Try sessionStorage as last resort
    if (!data) {
      try {
        const stored = sessionStorage.getItem('zerodrop_m')
        if (stored) data = stored
      } catch { /* ignore */ }
    }
  }

  if (data) {
    matchData.value = data
    // Clean URL without losing navigation ability
    window.history.replaceState({}, '', window.location.pathname)
  }
})

function readQrFromImage(imageData: ImageData): string | null {
  const code = jsQR(imageData.data, imageData.width, imageData.height)
  if (!code || !code.data) return null

  // 尝试从 URL 中提取 m 参数
  try {
    const url = new URL(code.data)
    const m = url.searchParams.get('m')
    if (m) return m
  } catch { /* 不是 URL，可能是纯数据 */ }

  // 直接返回扫描到的数据
  return code.data
}

function handleFileSelect(e: Event) {
  error.value = ''
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  decodeImage(file)
}

function handleDrop(e: DragEvent) {
  error.value = ''
  const file = e.dataTransfer?.files?.[0]
  if (!file) return
  decodeImage(file)
}

function decodeImage(file: File) {
  if (!file.type.startsWith('image/')) {
    error.value = '请选择图片文件'
    return
  }

  const img = new Image()
  const url = URL.createObjectURL(file)

  img.onload = () => {
    const canvas = document.createElement('canvas')
    const maxSize = 1024
    let w = img.naturalWidth
    let h = img.naturalHeight
    if (w > maxSize || h > maxSize) {
      const ratio = Math.min(maxSize / w, maxSize / h)
      w = Math.round(w * ratio)
      h = Math.round(h * ratio)
    }
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      error.value = '无法读取图片'
      URL.revokeObjectURL(url)
      return
    }
    ctx.drawImage(img, 0, 0, w, h)
    const imageData = ctx.getImageData(0, 0, w, h)

    const result = readQrFromImage(imageData)
    URL.revokeObjectURL(url)

    if (result) {
      matchData.value = result
    } else {
      error.value = '未识别到二维码，请尝试上传更清晰的图片'
    }
  }

  img.onerror = () => {
    error.value = '图片加载失败'
    URL.revokeObjectURL(url)
  }

  img.src = url
}

function submitManual() {
  error.value = ''
  const text = manualInput.value.trim()
  if (!text) return
  if (text.length < 2) {
    error.value = '数据格式不正确'
    return
  }
  matchData.value = text
}
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 32px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 22px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px 0;
}

.empty-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 32px 0;
}

.action-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 320px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.action-card:hover {
  border-color: #3b82f6;
  background: #222;
}

.action-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.action-text {
  font-size: 15px;
  font-weight: 500;
  color: #fff;
  flex-shrink: 0;
}

.file-input {
  display: none;
}

.manual-card {
  flex-wrap: wrap;
}

.manual-input {
  flex: 1;
  min-width: 100%;
  margin-top: 8px;
  background: #0f0f0f;
  border: 1px solid #444;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: #fff;
  outline: none;
  font-family: monospace;
}

.manual-input:focus {
  border-color: #3b82f6;
}

.manual-input::placeholder {
  color: #666;
}

.error-msg {
  margin-top: 16px;
  padding: 10px 16px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  color: #ef4444;
  font-size: 13px;
  max-width: 320px;
}

.error-detail {
  margin-top: 8px;
  padding: 10px 16px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  color: #ef4444;
  font-size: 12px;
  max-width: 320px;
  word-break: break-all;
  font-family: monospace;
}

.back-button {
  margin-top: 16px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 24px;
  font-size: 14px;
  cursor: pointer;
}

.empty-hint {
  margin-top: 28px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
  max-width: 280px;
  line-height: 1.6;
}

.hint-highlight {
  color: rgba(255, 255, 255, 0.5);
  font-weight: 500;
}
</style>