import express from 'express'
import cors from 'cors'
import {
  initBrowser,
  checkLogin,
  login,
  loadSession,
  getChatList,
  getChatMessages,
  sendMessage,
  closeBrowser
} from './browser.mjs'
import { handleWebhook } from './feishu-integration.mjs'

const app = express()
app.use(cors())
app.use(express.json())

// 状态管理
let lastChatList = []
let messageCallback = null

/**
 * 设置消息回调 - 当有新消息时调用
 */
export function onNewMessage(callback) {
  messageCallback = callback
}

/**
 * 轮询检查新消息
 */
export async function pollForNewMessages() {
  try {
    const chatList = await getChatList()
    
    if (!lastChatList.length) {
      lastChatList = chatList
      return []
    }
    
    // 找出新消息（新出现的或未读数增加的）
    const newMessages = []
    
    for (const chat of chatList) {
      const lastChat = lastChatList.find(c => c.id === chat.id)
      
      // 新聊天
      if (!lastChat && chat.unread) {
        newMessages.push({
          ...chat,
          isNew: true
        })
      }
      // 已存在的聊天但新增了未读
      else if (lastChat && chat.unread && !lastChat.unread) {
        newMessages.push({
          ...chat,
          isNew: false
        })
      }
    }
    
    lastChatList = chatList
    
    // 触发回调
    if (newMessages.length > 0 && messageCallback) {
      for (const msg of newMessages) {
        messageCallback(msg)
      }
    }
    
    return newMessages
  } catch (error) {
    console.error('检查新消息失败:', error.message)
    return []
  }
}

// ============ API 接口 ============

// 健康检查
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() })
})

// 检查登录状态
app.get('/api/login/status', async (req, res) => {
  try {
    const result = await checkLogin()
    res.json(result)
  } catch (error) {
    res.status(500).json({ error: error.message })
  }
})

// 扫码登录
app.post('/api/login', async (req, res) => {
  try {
    await login()
    res.json({ success: true })
  } catch (error) {
    res.status(500).json({ error: error.message })
  }
})

// 获取聊天列表
app.get('/api/chat/list', async (req, res) => {
  try {
    const chatList = await getChatList()
    lastChatList = chatList
    res.json({ success: true, data: chatList })
  } catch (error) {
    res.status(500).json({ error: error.message })
  }
})

// 获取聊天消息
app.get('/api/chat/messages/:chatId', async (req, res) => {
  try {
    const { chatId } = req.params
    const messages = await getChatMessages(chatId)
    res.json({ success: true, data: messages })
  } catch (error) {
    res.status(500).json({ error: error.message })
  }
})

// 发送消息
app.post('/api/chat/send', async (req, res) => {
  try {
    const { chatId, content } = req.body
    
    if (!chatId || !content) {
      return res.status(400).json({ error: '缺少必要参数' })
    }
    
    const result = await sendMessage(chatId, content)
    res.json(result)
  } catch (error) {
    res.status(500).json({ error: error.message })
  }
})

// 启动轮询
let pollInterval = null
app.post('/api/poll/start', (req, res) => {
  const { interval = 30000 } = req.body
  
  if (pollInterval) {
    return res.json({ message: '轮询已在运行中' })
  }
  
  pollInterval = setInterval(async () => {
    await pollForNewMessages()
  }, interval)
  
  res.json({ success: true, interval })
})

// 停止轮询
app.post('/api/poll/stop', (req, res) => {
  if (pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
  res.json({ success: true })
})

// 关闭浏览器
app.post('/api/browser/close', async (req, res) => {
  try {
    await closeBrowser()
    res.json({ success: true })
  } catch (error) {
    res.status(500).json({ error: error.message })
  }
})

// 飞书 Webhook（用于接收飞书消息事件）
app.post('/api/feishu/webhook', async (req, res) => {
  try {
    // 飞书 verification 验证
    const challenge = req.body?.challenge
    if (challenge) {
      return res.json({ challenge })
    }
    
    // 处理消息
    await handleWebhook(req.body)
    res.json({ success: true })
  } catch (error) {
    console.error('飞书 webhook 处理失败:', error)
    res.status(500).json({ error: error.message })
  }
})

// 启动服务器
const PORT = process.env.PORT || 3000

export function startServer(port = PORT) {
  return new Promise((resolve) => {
    app.listen(port, () => {
      console.log(`🚀 Boss Chat Bot API 服务已启动: http://localhost:${port}`)
      console.log(`
可用接口:
  GET  /health                    - 健康检查
  GET  /api/login/status          - 检查登录状态
  POST /api/login                 - 扫码登录
  GET  /api/chat/list            - 获取聊天列表
  GET  /api/chat/messages/:chatId - 获取聊天消息
  POST /api/chat/send            - 发送消息
  POST /api/poll/start            - 启动轮询
  POST /api/poll/stop            - 停止轮询
      `)
      resolve(port)
    })
  })
}

export default app
