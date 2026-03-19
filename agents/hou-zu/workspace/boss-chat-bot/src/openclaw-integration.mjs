/**
 * OpenClaw 集成模块
 * 
 * 两种接入方式:
 * 1. HTTP 轮询 - OpenClaw 定时调用 API 检查新消息
 * 2. 飞书通知 - 有新消息时通过飞书发送给用户，用户回复后再发送
 */

import fetch from 'node-fetch'

// 配置
const CONFIG = {
  // Boss API 服务地址
  bossApiUrl: process.env.BOSS_API_URL || 'http://localhost:3000',
  // OpenClaw 回调地址（用于发送回复）
  openclawCallbackUrl: process.env.OPENCLAW_CALLBACK_URL,
  // 飞书配置（可选）
  feishu: {
    webhookUrl: process.env.FEISHU_WEBHOOK_URL,
    botId: process.env.FEISHU_BOT_ID
  }
}

/**
 * 从 Boss API 获取聊天列表
 */
export async function getChatList() {
  const response = await fetch(`${CONFIG.bossApiUrl}/api/chat/list`)
  const data = await response.json()
  return data.data || []
}

/**
 * 从 Boss API 获取聊天消息
 */
export async function getChatMessages(chatId) {
  const response = await fetch(`${CONFIG.bossApiUrl}/api/chat/messages/${chatId}`)
  const data = await response.json()
  return data.data || []
}

/**
 * 发送消息到 Boss
 */
export async function sendBossMessage(chatId, content) {
  const response = await fetch(`${CONFIG.bossApiUrl}/api/chat/send`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ chatId, content })
  })
  const data = await response.json()
  return data
}

/**
 * 检查登录状态
 */
export async function checkLoginStatus() {
  const response = await fetch(`${CONFIG.bossApiUrl}/api/login/status`)
  const data = await response.json()
  return data
}

/**
 * 获取未处理的聊天列表
 */
export async function getUnreadChats() {
  const chatList = await getChatList()
  return chatList.filter(chat => chat.unread)
}

/**
 * 格式化聊天信息用于展示
 */
export function formatChatInfo(chat) {
  return `
💬 新消息来自: ${chat.bossName}
🏢 公司: ${chat.company}
📋 职位: ${chat.position}
📝 最新消息: ${chat.lastMessage}
🕐 时间: ${chat.time}
🔗 ChatID: ${chat.id}
  `.trim()
}

/**
 * 格式化消息列表用于上下文
 */
export function formatMessagesForContext(messages) {
  if (!messages || messages.length === 0) {
    return '暂无消息历史'
  }
  
  return messages
    .slice(-10)  // 最近10条
    .map(msg => {
      const prefix = msg.type === 'sent' ? '👤 你' : '💼 BOSS'
      return `${prefix}: ${msg.content}`
    })
    .join('\n')
}

export default {
  getChatList,
  getChatMessages,
  sendBossMessage,
  checkLoginStatus,
  getUnreadChats,
  formatChatInfo,
  formatMessagesForContext
}
