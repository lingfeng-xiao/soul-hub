/**
 * 飞书消息集成
 * 
 * 让用户可以通过飞书消息控制 Boss 直聘助手
 */

import fetch from 'node-fetch'
import {
  handleUserMessage
} from './handler.mjs'

// 飞书配置
const FEISHU_CONFIG = {
  appId: process.env.FEISHU_APP_ID,
  appSecret: process.env.FEISHU_APP_SECRET,
  // 机器人 webhook（用于接收消息）
  webhookUrl: process.env.FEISHU_WEBHOOK_URL
}

// 获取飞书 access_token
async function getFeishuAccessToken() {
  const response = await fetch('https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      app_id: FEISHU_CONFIG.appId,
      app_secret: FEISHU_CONFIG.appSecret
    })
  })
  
  const data = await response.json()
  return data.tenant_access_token
}

/**
 * 发送飞书消息
 */
export async function sendFeishuMessage(receiveId, content, msgType = 'text') {
  const token = await getFeishuAccessToken()
  
  const response = await fetch('https://open.feishu.cn/open-apis/im/v1/messages', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      receive_id: receiveId,
      msg_type: msgType,
      content: JSON.stringify({ text: content })
    })
  })
  
  return await response.json()
}

/**
 * 回复飞书消息
 */
export async function replyFeishuMessage(messageId, content, msgType = 'text') {
  const token = await getFeishuAccessToken()
  
  const response = await fetch(`https://open.feishu.cn/open-apis/im/v1/messages/${messageId}/reply`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      msg_type: msgType,
      content: JSON.stringify({ text: content })
    })
  })
  
  return await response.json()
}

/**
 * 处理收到的飞书消息
 */
export async function handleFeishuMessage(event) {
  const { message, sender } = event
  
  // 只处理文本消息
  if (message?.msg_type !== 'text') {
    return { success: true }
  }
  
  const userId = sender?.sender_id?.user_id
  const content = JSON.parse(message.content).text
  
  console.log(`收到飞书消息 from ${userId}: ${content}`)
  
  // 处理消息
  const result = await handleUserMessage(content, {
    platform: 'feishu',
    userId,
    messageId: message.message_id
  })
  
  // 发送回复
  if (result.type === 'text') {
    await replyFeishuMessage(message.message_id, result.content)
  } else if (result.type === 'reply_request') {
    // 需要用户确认回复内容
    await replyFeishuMessage(message.message_id, 
      result.content + '\n\n请确认是否发送，或输入新的回复内容'
    )
  }
  
  return { success: true }
}

/**
 * 飞书消息 webhook 入口（供外部调用）
 */
export async function handleWebhook(requestBody) {
  // 验证签名（生产环境需要）
  // const signature = requestBody.headers['x-feishu-signature']
  
  const event = requestBody.event
  
  if (event?.message) {
    await handleFeishuMessage(event)
  }
  
  return { success: true }
}

export default {
  sendFeishuMessage,
  replyFeishuMessage,
  handleFeishuMessage,
  handleWebhook
}
