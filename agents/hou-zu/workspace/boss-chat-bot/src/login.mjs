/**
 * 登录脚本 - 独立运行用于扫码登录
 */
import { login, loadSession } from './browser.mjs'

async function main() {
  console.log('='.repeat(50))
  console.log('Boss 直聘登录工具')
  console.log('='.repeat(50))
  
  // 尝试加载已有会话
  const hasSession = await loadSession()
  
  if (hasSession) {
    console.log('已加载之前的登录状态')
    console.log('如果需要重新登录，请删除 data/cookies.json 文件')
  }
  
  // 执行登录
  await login()
  
  console.log('登录完成！')
  process.exit(0)
}

main().catch(err => {
  console.error('登录失败:', err)
  process.exit(1)
})
