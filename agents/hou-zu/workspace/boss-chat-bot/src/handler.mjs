/**
 * Boss 直聘消息助手 - 对话式交互入口
 * 
 * 整合 OpenClaw Skill，用于处理用户指令
 */

import {
  getChatList,
  getChatMessages,
  sendBossMessage,
  checkLoginStatus,
  getUnreadChats,
  formatChatInfo,
  formatMessagesForContext
} from './openclaw-integration.mjs'

/**
 * 处理用户消息
 * @param {string} userMessage - 用户的消息
 * @param {Object} context - 上下文信息
 * @returns {Object} - 响应结果
 */
export async function handleUserMessage(userMessage, context = {}) {
  const message = userMessage.toLowerCase().trim()
  
  // 1. 查看未读消息
  if (message.includes('查看消息') || message.includes('看消息') || message === '消息') {
    return await showUnreadMessages()
  }
  
  // 2. 查看特定聊天
  if (message.includes('查看和') || message.includes('查看 ') || message.includes('聊天记录')) {
    const nameMatch = userMessage.match(/查看和(.+?)的|查看(.+?)的|聊天记录/)
    const targetName = nameMatch?.[1]?.trim()
    if (targetName) {
      return await showChatMessages(targetName)
    }
  }
  
  // 3. AI 自动回复
  if (message.includes('回复') || message.includes('自动回复') || message.includes('回复这个')) {
    return await autoReply()
  }
  
  // 4. 手动发送消息
  const sendMatch = userMessage.match(/给(.+?)发送[:：](.+)/)
  if (sendMatch) {
    const targetName = sendMatch[1].trim()
    const content = sendMatch[2].trim()
    return await manualReply(targetName, content)
  }
  
  // 5. 检查状态
  if (message.includes('状态') || message.includes('登录')) {
    return await checkStatus()
  }
  
  // 默认 - 显示帮助
  return {
    type: 'text',
    content: `
🤖 Boss 直聘消息助手

可用命令：
• "查看消息" - 查看未读消息列表
• "查看和 XXX 的聊天" - 查看与某个 Boss 的聊天记录
• "回复这个" - AI 自动回复最后一条未读消息
• "给 XXX 发送: 内容" - 手动发送消息
• "状态" - 检查登录状态

💡 提示：需要先运行 API 服务 (npm start)
    `.trim()
  }
}

/**
 * 显示未读消息列表
 */
async function showUnreadMessages() {
  try {
    const unreadChats = await getUnreadChats()
    
    if (!unreadChats || unreadChats.length === 0) {
      return {
        type: 'text',
        content: '✅ 暂无未读消息'
      }
    }
    
    const message = unreadChats
      .map((chat, index) => `${index + 1}. ${formatChatInfo(chat)}`)
      .join('\n\n')
    
    return {
      type: 'text',
      content: `📬 未读消息 (${unreadChats.length} 条):\n\n${message}`,
      context: {
        unreadChats,
        lastChat: unreadChats[0]
      }
    }
  } catch (error) {
    return {
      type: 'text',
      content: `❌ 获取消息失败: ${error.message}\n\n请确保 API 服务正在运行 (npm start)`
    }
  }
}

/**
 * 查看特定聊天记录
 */
async function showChatMessages(targetName) {
  try {
    const chatList = await getChatList()
    const targetChat = chatList.find(c => 
      c.bossName?.includes(targetName) || 
      c.company?.includes(targetName)
    )
    
    if (!targetChat) {
      return {
        type: 'text',
        content: `❌ 未找到与 "${targetName}" 的聊天`
      }
    }
    
    const messages = await getChatMessages(targetChat.id)
    
    if (!messages || messages.length === 0) {
      return {
        type: 'text',
        content: `与 ${targetChat.bossName} 暂无消息`
      }
    }
    
    const chatInfo = formatChatInfo(targetChat)
    const msgHistory = formatMessagesForContext(messages)
    
    return {
      type: 'text',
      content: `${chatInfo}\n\n💬 聊天记录:\n${msgHistory}`,
      context: {
        currentChat: targetChat,
        messages
      }
    }
  } catch (error) {
    return {
      type: 'text',
      content: `❌ 获取聊天记录失败: ${error.message}`
    }
  }
}

/**
 * AI 自动回复最后一条未读消息
 */
async function autoReply() {
  try {
    const unreadChats = await getUnreadChats()
    
    if (!unreadChats || unreadChats.length === 0) {
      return {
        type: 'text',
        content: '✅ 暂无未读消息需要回复'
      }
    }
    
    // 取最后一条未读消息
    const targetChat = unreadChats[unreadChats.length - 1]
    const messages = await getChatMessages(targetChat.id)
    
    // 生成回复（这里返回提示让 OpenClaw 生成智能回复）
    return {
      type: 'reply_request',
      content: `请为以下情况生成回复:\n\n${formatChatInfo(targetChat)}\n\n最近消息:\n${formatMessagesForContext(messages)}`,
      context: {
        chatId: targetChat.id,
        bossName: targetChat.bossName,
        messages
      }
    }
  } catch (error) {
    return {
      type: 'text',
      content: `❌ 自动回复失败: ${error.message}`
    }
  }
}

/**
 * 手动发送消息
 */
async function manualReply(targetName, content) {
  try {
    const chatList = await getChatList()
    const targetChat = chatList.find(c => 
      c.bossName?.includes(targetName) || 
      c.company?.includes(targetName)
    )
    
    if (!targetChat) {
      return {
        type: 'text',
        content: `❌ 未找到与 "${targetName}" 的聊天`
      }
    }
    
    await sendBossMessage(targetChat.id, content)
    
    return {
      type: 'text',
      content: `✅ 消息已发送给 ${targetChat.bossName}:\n\n${content}`
    }
  } catch (error) {
    return {
      type: 'text',
      content: `❌ 发送失败: ${error.message}`
    }
  }
}

/**
 * 检查登录状态
 */
async function checkStatus() {
  try {
    const status = await checkLoginStatus()
    
    if (status.loggedIn) {
      return {
        type: 'text',
        content: '✅ 已登录 Boss 直聘'
      }
    } else {
      return {
        type: 'text',
        content: '❌ 未登录，需要扫码登录\n\n运行 npm run login 进行登录'
      }
    }
  } catch (error) {
    return {
      type: 'text',
      content: `❌ 检查状态失败: ${error.message}\n\n请确保 API 服务正在运行`
    }
  }
}

export default {
  handleUserMessage,
  showUnreadMessages,
  showChatMessages,
  autoReply,
  manualReply,
  checkStatus
}
