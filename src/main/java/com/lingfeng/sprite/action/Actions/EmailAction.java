package com.lingfeng.sprite.action.Actions;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;

/**
 * S7-1: 邮件动作插件
 *
 * 通过SMTP发送邮件给主人
 *
 * 参数:
 * - to: 收件人邮箱 (默认使用配置的主邮箱)
 * - subject: 邮件主题
 * - content: 邮件内容
 * - smtpHost: SMTP服务器地址
 * - smtpPort: SMTP端口
 * - username: 用户名
 * - password: 密码
 */
public class EmailAction implements ActionPlugin {

    private static final Logger logger = LoggerFactory.getLogger(EmailAction.class);

    // 默认SMTP配置 (可通过参数覆盖)
    private static String defaultSmtpHost = "smtp.gmail.com";
    private static int defaultSmtpPort = 587;
    private static String defaultUsername = "";
    private static String defaultPassword = "";
    private static String defaultFrom = "";

    @Override
    public String getName() {
        return "EmailAction";
    }

    /**
     * 设置默认SMTP配置 (全局配置)
     */
    public static void configure(String smtpHost, int smtpPort, String username, String password, String from) {
        defaultSmtpHost = smtpHost;
        defaultSmtpPort = smtpPort;
        defaultUsername = username;
        defaultPassword = password;
        defaultFrom = from;
    }

    @Override
    public ActionResult execute(Map<String, Object> params) {
        try {
            String actionParam = (String) params.get("actionParam");
            Instant timestamp = (Instant) params.get("timestamp");

            // 解析参数
            String to = getString(params, "to", getOwnerEmail(params));
            String subject = getString(params, "subject", "Sprite 通知");
            String content = actionParam != null ? actionParam : getString(params, "content", "");

            // 获取SMTP配置 (优先使用参数，其次使用默认配置)
            String smtpHost = getString(params, "smtpHost", defaultSmtpHost);
            int smtpPort = getInt(params, "smtpPort", defaultSmtpPort);
            String username = getString(params, "username", defaultUsername);
            String password = getString(params, "password", defaultPassword);
            String from = getString(params, "from", defaultFrom);

            // 验证必要配置
            if (smtpHost == null || smtpHost.isEmpty()) {
                return ActionResult.failure("SMTP host not configured");
            }
            if (to == null || to.isEmpty()) {
                return ActionResult.failure("Recipient email not configured");
            }

            // 记录邮件信息
            logger.info("=== Email Action ===");
            logger.info("Time: {}", timestamp);
            logger.info("From: {}", from);
            logger.info("To: {}", to);
            logger.info("Subject: {}", subject);
            logger.info("Content: {}", content);
            logger.info("SMTP: {}:{}", smtpHost, smtpPort);
            logger.info("==================");

            // 如果没有配置认证信息，只记录不发送
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                logger.warn("Email credentials not configured, only logging the email");
                return ActionResult.success("Email logged (credentials not configured): to=" + to + ", subject=" + subject);
            }

            // 发送邮件
            boolean sent = sendEmail(smtpHost, smtpPort, username, password, from, to, subject, content);

            if (sent) {
                return ActionResult.success("Email sent successfully: to=" + to + ", subject=" + subject);
            } else {
                return ActionResult.failure("Failed to send email");
            }

        } catch (Exception e) {
            logger.error("EmailAction failed: {}", e.getMessage());
            return ActionResult.failure("EmailAction failed: " + e.getMessage());
        }
    }

    /**
     * 发送邮件
     */
    private boolean sendEmail(String smtpHost, int smtpPort, String username, String password,
                            String from, String to, String subject, String content) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);
            message.setSentDate(new java.util.Date());

            Transport.send(message);
            logger.info("Email sent successfully to {}", to);
            return true;

        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从参数中获取字符串
     */
    private String getString(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * 从参数中获取整数
     */
    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取主人的邮箱地址
     */
    private String getOwnerEmail(Map<String, Object> params) {
        // 可以从worldModel或配置中获取
        // 这里返回空字符串，实际使用时需要配置
        return "";
    }
}
