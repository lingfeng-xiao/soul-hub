import puppeteer from 'puppeteer-extra'
import StealthPlugin from 'puppeteer-extra-plugin-stealth'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

// 配置
const CONFIG = {
  bossUrl: 'https://www.zhipin.com/web/geek/chat',
  loginUrl: 'https://www.zhipin.com/web/user/login',
  cookieFile: path.join(__dirname, '../data/cookies.json'),
  sessionFile: path.join(__dirname, '../data/session.json')
}

// 初始化 Puppeteer
puppeteer.use(StealthPlugin())

let browser = null
let page = null

/**
 * 初始化浏览器
 */
export async function initBrowser() {
  if (browser) return { browser, page }
  
  const exePath = process.env.PUPPETEER_EXECUTABLE_PATH
  
  browser = await puppeteer.launch({
    executablePath: exePath,
    headless: false,
    defaultViewport: { width: 1280, height: 800 },
    args: [
      '--disable-blink-features=AutomationControlled',
      '--no-sandbox',
      '--disable-setuid-sandbox'
    ]
  })
  
  page = await browser.newPage()
  
  // 设置用户代理
  await page.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
  
  return { browser, page }
}

/**
 * 检查是否已登录
 */
export async function checkLogin() {
  const { page } = await initBrowser()
  
  await page.goto(CONFIG.bossUrl, { waitUntil: 'networkidle0' })
  
  // 检查是否跳转到了登录页
  const isLoginPage = page.url().includes('/login')
  
  if (isLoginPage) {
    return { loggedIn: false, message: '需要登录' }
  }
  
  // 检查是否登录成功
  const userInfo = await page.evaluate(() => {
    const avatar = document.querySelector('.user-avatar')
    return avatar ? true : false
  })
  
  return { loggedIn: userInfo, message: userInfo ? '已登录' : '需要登录' }
}

/**
 * 扫码登录
 */
export async function login() {
  const { browser, page } = await initBrowser()
  
  await page.goto(CONFIG.loginUrl, { waitUntil: 'networkidle0' })
  
  console.log('='.repeat(50))
  console.log('请扫码登录 Boss 直聘')
  console.log('登录成功后脚本会自动保存登录状态')
  console.log('='.repeat(50))
  
  // 等待登录成功
  await page.waitForFunction(() => {
    return !window.location.href.includes('/login')
  }, { timeout: 120000 })
  
  // 保存 Cookie
  const cookies = await page.cookies()
  fs.writeFileSync(CONFIG.cookieFile, JSON.stringify(cookies, null, 2))
  console.log('登录成功，Cookie 已保存')
  
  return { success: true }
}

/**
 * 加载登录状态
 */
export async function loadSession() {
  const { page } = await initBrowser()
  
  if (fs.existsSync(CONFIG.cookieFile)) {
    const cookies = JSON.parse(fs.readFileSync(CONFIG.cookieFile, 'utf-8'))
    await page.setCookie(...cookies)
    console.log('已加载登录状态')
    return true
  }
  
  return false
}

/**
 * 获取聊天列表
 */
export async function getChatList() {
  const { page } = await initBrowser()
  
  await page.goto(CONFIG.bossUrl, { waitUntil: 'networkidle0' })
  await page.waitForSelector('.chat-item', { timeout: 10000 }).catch(() => null)
  
  const chatList = await page.evaluate(() => {
    const items = document.querySelectorAll('.chat-item')
    return Array.from(items).map(item => ({
      id: item.getAttribute('data-id') || item.getAttribute('data-chatid'),
      bossName: item.querySelector('.name')?.textContent?.trim() || 
                item.querySelector('.boss-name')?.textContent?.trim(),
      company: item.querySelector('.company')?.textContent?.trim() || 
               item.querySelector('.company-name')?.textContent?.trim(),
      position: item.querySelector('.position')?.textContent?.trim() || 
                item.querySelector('.job-name')?.textContent?.trim(),
      lastMessage: item.querySelector('.msg-text')?.textContent?.trim() || 
                   item.querySelector('.last-msg')?.textContent?.trim(),
      time: item.querySelector('.time')?.textContent?.trim(),
      unread: !!item.querySelector('.unread') || 
              !!item.querySelector('.red-point') ||
              item.classList.contains('unread')
    }))
  })
  
  return chatList
}

/**
 * 获取聊天详情和消息
 */
export async function getChatMessages(chatId) {
  const { page } = await initBrowser()
  
  // 点击进入聊天窗口 - 尝试多种选择器
  const selectors = [
    `.chat-item[data-id="${chatId}"]`,
    `.chat-item[data-chatid="${chatId}"]`,
    `.chat-item:has-text("${chatId}")`
  ]
  
  for (const selector of selectors) {
    try {
      await page.click(selector, { timeout: 3000 })
      break
    } catch (e) {
      continue
    }
  }
  
  await page.waitForLoadState('networkidle0')
  await new Promise(r => setTimeout(r, 1000))
  
  // 滚动到顶部加载更多
  await page.evaluate(() => {
    const msgList = document.querySelector('.msg-list') || 
                    document.querySelector('.chat-msg-list')
    if (msgList) msgList.scrollTop = 0
  })
  
  await new Promise(r => setTimeout(r, 500))
  
  // 解析消息
  const messages = await page.evaluate(() => {
    const msgItems = document.querySelectorAll('.msg-item, .chat-msg-item')
    return Array.from(msgItems).map(item => ({
      type: item.classList.contains('mine') || 
            item.classList.contains('sent') ? 'sent' : 'received',
      content: item.querySelector('.msg-content, .text')?.textContent?.trim(),
      time: item.querySelector('.time, .msg-time')?.textContent?.trim(),
      isRead: !!item.querySelector('.read') || 
              !!item.querySelector('.readed')
    }))
  })
  
  // 返回聊天列表页
  await page.click('.back-btn, .chat-back, [class*="back"]').catch(() => {})
  await page.waitForLoadState('networkidle0')
  
  return messages
}

/**
 * 发送消息
 */
export async function sendMessage(chatId, content) {
  const { page } = await initBrowser()
  
  // 进入聊天窗口
  const selectors = [
    `.chat-item[data-id="${chatId}"]`,
    `.chat-item[data-chatid="${chatId}"]`
  ]
  
  for (const selector of selectors) {
    try {
      await page.click(selector, { timeout: 3000 })
      break
    } catch (e) {
      continue
    }
  }
  
  await page.waitForLoadState('networkidle0')
  await new Promise(r => setTimeout(r, 500))
  
  // 输入消息 - 尝试多种输入方式
  const inputSelectors = [
    '.msg-input textarea',
    '.msg-input input',
    '.chat-input textarea',
    'input[class*="msg"]',
    'textarea[class*="msg"]'
  ]
  
  let inputEl = null
  for (const selector of inputSelectors) {
    inputEl = await page.$(selector)
    if (inputEl) break
  }
  
  if (!inputEl) {
    throw new Error('找不到消息输入框')
  }
  
  await inputEl.click()
  await new Promise(r => setTimeout(r, 200))
  
  // 清空输入框
  await inputEl.click({ clickCount: 3 })
  await page.keyboard.press('Backspace')
  
  // 输入内容
  await page.keyboard.type(content, { delay: 30 })
  
  // 点击发送按钮
  const sendBtnSelectors = [
    '.send-btn',
    '.msg-send-btn',
    'button[class*="send"]',
    '[ka="send_msg"]'
  ]
  
  for (const selector of sendBtnSelectors) {
    try {
      await page.click(selector, { timeout: 2000 })
      break
    } catch (e) {
      continue
    }
  }
  
  // 等待发送完成
  await new Promise(r => setTimeout(r, 1000))
  
  console.log(`消息已发送: ${content}`)
  
  // 返回列表
  await page.click('.back-btn, .chat-back').catch(() => {})
  
  return { success: true, content }
}

/**
 * 关闭浏览器
 */
export async function closeBrowser() {
  if (browser) {
    await browser.close()
    browser = null
    page = null
  }
}

export default {
  initBrowser,
  checkLogin,
  login,
  loadSession,
  getChatList,
  getChatMessages,
  sendMessage,
  closeBrowser
}
