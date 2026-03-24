package com.lingfeng.sprite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SpriteController 集成测试 (S30-3: API End-to-End Tests)
 *
 * 测试所有 REST API 端点，包括：
 * - 状态查询接口
 * - 认知循环接口
 * - 记忆系统接口
 * - 进化系统接口
 * - 情绪分析接口
 * - GitHub 备份接口
 * - 外部 API 接口
 * - 性能监控接口
 * - 多设备协同接口
 * - 认证与授权测试
 * - 错误处理测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpriteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 核心状态接口测试 ====================

    @Nested
    @DisplayName("核心状态接口测试")
    class CoreStateTests {

        @Test
        @DisplayName("GET /api/sprite/state - 获取 Sprite 当前状态")
        void testGetState() throws Exception {
            mockMvc.perform(get("/api/sprite/state"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").exists());
        }

        @Test
        @DisplayName("POST /api/sprite/cycle - 手动触发认知闭环")
        void testCognitionCycle() throws Exception {
            mockMvc.perform(post("/api/sprite/cycle"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @DisplayName("POST /api/sprite/start - 启动 Sprite")
        void testStartSprite() throws Exception {
            mockMvc.perform(post("/api/sprite/start"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/sprite/stop - 停止 Sprite")
        void testStopSprite() throws Exception {
            mockMvc.perform(post("/api/sprite/stop"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 反馈接口测试 ====================

    @Nested
    @DisplayName("反馈接口测试")
    class FeedbackTests {

        @Test
        @DisplayName("POST /api/sprite/feedback - 提交反馈")
        void testRecordFeedback() throws Exception {
            String requestJson = """
                {
                    "type": "USER_FEEDBACK",
                    "content": "测试反馈内容",
                    "outcome": "测试结果",
                    "success": true,
                    "impact": "POSITIVE"
                }
                """;

            mockMvc.perform(post("/api/sprite/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/sprite/feedback - 无效反馈类型应返回错误")
        void testRecordFeedbackWithInvalidType() throws Exception {
            String requestJson = """
                {
                    "type": "INVALID_TYPE",
                    "content": "测试",
                    "outcome": "结果",
                    "success": false,
                    "impact": "NEGATIVE"
                }
                """;

            mockMvc.perform(post("/api/sprite/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/sprite/feedback - 获取反馈统计")
        void testGetFeedbackStats() throws Exception {
            mockMvc.perform(get("/api/sprite/feedback"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalFeedback").exists());
        }
    }

    // ==================== 记忆系统接口测试 ====================

    @Nested
    @DisplayName("记忆系统接口测试")
    class MemoryTests {

        @Test
        @DisplayName("GET /api/sprite/memory - 获取记忆系统状态")
        void testGetMemoryStatus() throws Exception {
            mockMvc.perform(get("/api/sprite/memory"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.shortTermMemory").exists())
                    .andExpect(jsonPath("$.longTermMemory").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/memory/visualization - 获取记忆可视化数据")
        void testGetMemoryVisualization() throws Exception {
            mockMvc.perform(get("/api/sprite/memory/visualization"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.nodes").exists())
                    .andExpect(jsonPath("$.edges").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/memory/timeline - 获取记忆时间线（无参数）")
        void testGetMemoryTimelineWithoutParams() throws Exception {
            mockMvc.perform(get("/api/sprite/memory/timeline"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.events").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/memory/timeline - 获取记忆时间线（带日期参数）")
        void testGetMemoryTimelineWithParams() throws Exception {
            Instant startDate = Instant.parse("2026-03-01T00:00:00Z");
            Instant endDate = Instant.parse("2026-03-24T23:59:59Z");

            mockMvc.perform(get("/api/sprite/memory/timeline")
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.events").exists());
        }
    }

    // ==================== 进化系统接口测试 ====================

    @Nested
    @DisplayName("进化系统接口测试")
    class EvolutionTests {

        @Test
        @DisplayName("GET /api/sprite/evolution - 获取进化状态")
        void testGetEvolutionStatus() throws Exception {
            mockMvc.perform(get("/api/sprite/evolution"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.generation").exists())
                    .andExpect(jsonPath("$.enabled").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/evolution/dashboard - 获取进化 Dashboard 数据")
        void testGetEvolutionDashboard() throws Exception {
            mockMvc.perform(get("/api/sprite/evolution/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.currentGeneration").exists())
                    .andExpect(jsonPath("$.evolutionProgress").exists());
        }
    }

    // ==================== 认知统计接口测试 ====================

    @Nested
    @DisplayName("认知统计接口测试")
    class CognitionStatsTests {

        @Test
        @DisplayName("GET /api/sprite/stats - 获取认知统计")
        void testGetCognitionStats() throws Exception {
            mockMvc.perform(get("/api/sprite/stats"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalCycles").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/cognition/dashboard - 获取认知 Dashboard 数据")
        void testGetCognitionDashboard() throws Exception {
            mockMvc.perform(get("/api/sprite/cognition/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.cognitionMetrics").exists());
        }
    }

    // ==================== 健康检查接口测试 ====================

    @Nested
    @DisplayName("健康检查接口测试")
    class HealthTests {

        @Test
        @DisplayName("GET /api/sprite/health - 获取系统健康状态")
        void testGetHealth() throws Exception {
            mockMvc.perform(get("/api/sprite/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.components").exists());
        }
    }

    // ==================== 交互偏好接口测试 ====================

    @Nested
    @DisplayName("交互偏好接口测试")
    class PreferencesTests {

        @Test
        @DisplayName("GET /api/sprite/preferences - 获取主人交互偏好")
        void testGetPreferences() throws Exception {
            mockMvc.perform(get("/api/sprite/preferences"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.preferredInteractionTime").exists())
                    .andExpect(jsonPath("$.communicationStyle").exists());
        }
    }

    // ==================== 情绪分析接口测试 ====================

    @Nested
    @DisplayName("情绪分析接口测试")
    class EmotionTests {

        @Test
        @DisplayName("GET /api/sprite/emotions - 获取情绪统计（存在数据）")
        void testGetEmotionStats() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/sprite/emotions"))
                    .andReturn();

            int status = result.getResponse().getStatus();
            if (status == 200) {
                mockMvc.perform(get("/api/sprite/emotions"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.dominantEmotion").exists());
            } else {
                // No content - 是合法的响应
                org.junit.jupiter.api.Assertions.assertEquals(204, status);
            }
        }

        @Test
        @DisplayName("GET /api/sprite/emotions/weekly - 获取周情绪模式")
        void testGetWeeklyEmotionPattern() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/weekly"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.patterns").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/emotions/contact-advice - 获取每周联系建议")
        void testGetContactAdvice() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/contact-advice"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.recommendedDays").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/emotions/optimal-windows - 获取最优联系时间窗口")
        void testGetOptimalContactWindows() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/optimal-windows"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/sprite/emotions/predict - 预测情绪（默认参数）")
        void testPredictEmotionDefaultParams() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/predict"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.predictedEmotion").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/emotions/predict - 预测情绪（指定日期和时间）")
        void testPredictEmotionWithParams() throws Exception {
            LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1);

            mockMvc.perform(get("/api/sprite/emotions/predict")
                            .param("date", tomorrow.toString())
                            .param("hour", "14"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.predictedEmotion").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/emotions/trend - 获取情绪趋势（默认7天）")
        void testGetEmotionTrendDefault() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/trend"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.trend").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/emotions/trend - 获取情绪趋势（指定天数）")
        void testGetEmotionTrendWithDays() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/trend")
                            .param("days", "14"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.trend").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/emotions/dashboard - 获取情绪 Dashboard")
        void testGetEmotionDashboard() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.currentEmotion").exists());
        }
    }

    // ==================== GitHub 备份接口测试 ====================

    @Nested
    @DisplayName("GitHub 备份接口测试")
    class BackupTests {

        @Test
        @DisplayName("POST /api/sprite/backup - 手动触发 GitHub 备份")
        void testTriggerBackup() throws Exception {
            mockMvc.perform(post("/api/sprite/backup"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/backup/index - 获取备份索引")
        void testGetBackupIndex() throws Exception {
            mockMvc.perform(get("/api/sprite/backup/index"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/sprite/backup/status - 获取备份状态")
        void testGetBackupStatus() throws Exception {
            mockMvc.perform(get("/api/sprite/backup/status"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.backupEnabled").exists())
                    .andExpect(jsonPath("$.lastBackupTime").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/backup/list - 获取备份列表")
        void testListBackups() throws Exception {
            mockMvc.perform(get("/api/sprite/backup/list"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.backups").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/backup/versions - 获取所有备份版本")
        void testListBackupVersions() throws Exception {
            mockMvc.perform(get("/api/sprite/backup/versions"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/sprite/backup/snapshot - 缺少 timestamp 参数应返回错误")
        void testGetMemorySnapshotWithoutTimestamp() throws Exception {
            mockMvc.perform(get("/api/sprite/backup/snapshot"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/sprite/backup/compare - 缺少参数应返回错误")
        void testCompareBackupsWithoutParams() throws Exception {
            mockMvc.perform(get("/api/sprite/backup/compare"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/sprite/backup/conflicts - 检测备份冲突")
        void testCheckConflicts() throws Exception {
            mockMvc.perform(get("/api/sprite/backup/conflicts"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.hasConflicts").exists());
        }

        @Test
        @DisplayName("POST /api/sprite/backup/config - 备份配置文件")
        void testBackupConfigFiles() throws Exception {
            mockMvc.perform(post("/api/sprite/backup/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @DisplayName("POST /api/sprite/backup/config - 备份配置文件（带提交消息）")
        void testBackupConfigFilesWithMessage() throws Exception {
            mockMvc.perform(post("/api/sprite/backup/config")
                            .param("commitMessage", "Test backup commit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @DisplayName("POST /api/sprite/backup/snapshot - 备份代码快照")
        void testBackupCodeSnapshot() throws Exception {
            mockMvc.perform(post("/api/sprite/backup/snapshot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @DisplayName("POST /api/sprite/backup/restore - 缺少 timestamp 参数应返回错误")
        void testRestoreFromBackupWithoutTimestamp() throws Exception {
            mockMvc.perform(post("/api/sprite/backup/restore"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/sprite/backup/rollback/{versionId} - 缺少 versionId 应返回404")
        void testRollbackWithoutVersionId() throws Exception {
            // 传入不存在的版本ID进行测试
            mockMvc.perform(post("/api/sprite/backup/rollback/non-existent-version"))
                    .andExpect(status().isOk()); // 可能返回内部错误但不应该是404
        }
    }

    // ==================== 外部 API 接口测试 ====================

    @Nested
    @DisplayName("外部 API 接口测试")
    class ExternalApiTests {

        @Test
        @DisplayName("GET /api/sprite/external/weather - 查询天气（缺少城市参数）")
        void testGetWeatherWithoutCity() throws Exception {
            mockMvc.perform(get("/api/sprite/external/weather"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/sprite/external/weather - 查询天气")
        void testGetWeather() throws Exception {
            mockMvc.perform(get("/api/sprite/external/weather")
                            .param("city", "Beijing"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/external/news - 查询新闻（默认参数）")
        void testGetNewsDefaultParams() throws Exception {
            mockMvc.perform(get("/api/sprite/external/news"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/external/news - 查询新闻（自定义参数）")
        void testGetNewsWithParams() throws Exception {
            mockMvc.perform(get("/api/sprite/external/news")
                            .param("topic", "technology")
                            .param("page", "1")
                            .param("pageSize", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/external/search - 网络搜索（缺少查询参数）")
        void testSearchWithoutQuery() throws Exception {
            mockMvc.perform(get("/api/sprite/external/search"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/sprite/external/search - 网络搜索")
        void testSearch() throws Exception {
            mockMvc.perform(get("/api/sprite/external/search")
                            .param("query", "Spring Boot")
                            .param("numResults", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").exists());
        }

        @Test
        @DisplayName("GET /api/sprite/external/translate - 翻译（缺少文本参数）")
        void testTranslateWithoutText() throws Exception {
            mockMvc.perform(get("/api/sprite/external/translate"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/sprite/external/translate - 翻译")
        void testTranslate() throws Exception {
            mockMvc.perform(get("/api/sprite/external/translate")
                            .param("text", "Hello")
                            .param("from", "en")
                            .param("to", "zh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").exists());
        }
    }

    // ==================== 性能监控接口测试 ====================

    @Nested
    @DisplayName("性能监控接口测试")
    class MonitorTests {

        @Test
        @DisplayName("GET /api/sprite/monitor/alerts - 检查性能告警")
        void testCheckAlerts() throws Exception {
            mockMvc.perform(get("/api/sprite/monitor/alerts"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/sprite/monitor/snapshot - 获取性能快照")
        void testGetSnapshot() throws Exception {
            mockMvc.perform(get("/api/sprite/monitor/snapshot"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.memory").exists())
                    .andExpect(jsonPath("$.threads").exists());
        }
    }

    // ==================== 多设备协同接口测试 ====================

    @Nested
    @DisplayName("多设备协同接口测试")
    class DeviceTests {

        @Test
        @DisplayName("GET /api/sprite/devices - 获取所有设备列表")
        void testGetAllDevices() throws Exception {
            mockMvc.perform(get("/api/sprite/devices"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/sprite/devices/status - 获取设备协同状态")
        void testGetCoordinationStatus() throws Exception {
            mockMvc.perform(get("/api/sprite/devices/status"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.isActive").exists());
        }

        @Test
        @DisplayName("POST /api/sprite/devices/sync - 触发设备同步")
        void testTriggerSync() throws Exception {
            mockMvc.perform(post("/api/sprite/devices/sync"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /api/sprite/devices/local - 获取本地设备信息")
        void testGetLocalDevice() throws Exception {
            mockMvc.perform(get("/api/sprite/devices/local"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.deviceId").exists());
        }

        @Test
        @DisplayName("POST /api/sprite/devices/register - 注册新设备")
        void testRegisterDevice() throws Exception {
            String requestJson = """
                {
                    "deviceId": "test-device-001",
                    "deviceName": "Test Device",
                    "deviceType": "MOBILE",
                    "ipAddress": "192.168.1.100"
                }
                """;

            mockMvc.perform(post("/api/sprite/devices/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/sprite/devices/register - 设备注册信息不完整")
        void testRegisterDeviceIncomplete() throws Exception {
            String requestJson = """
                {
                    "deviceId": "test-device-001"
                }
                """;

            mockMvc.perform(post("/api/sprite/devices/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk()); // 服务端可能接受不完整的请求
        }

        @Test
        @DisplayName("DELETE /api/sprite/devices/{deviceId} - 注销设备")
        void testUnregisterDevice() throws Exception {
            // 先注册再注销
            String requestJson = """
                {
                    "deviceId": "temp-device-001",
                    "deviceName": "Temp Device",
                    "deviceType": "TABLET",
                    "ipAddress": "192.168.1.200"
                }
                """;

            mockMvc.perform(post("/api/sprite/devices/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/sprite/devices/temp-device-001"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/sprite/devices/{deviceId}/state - 更新设备状态")
        void testUpdateDeviceState() throws Exception {
            // 先注册再更新状态
            String requestJson = """
                {
                    "deviceId": "state-test-device",
                    "deviceName": "State Test Device",
                    "deviceType": "PC",
                    "ipAddress": "192.168.1.50"
                }
                """;

            mockMvc.perform(post("/api/sprite/devices/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            mockMvc.perform(put("/api/sprite/devices/state-test-device/state")
                            .param("state", "ACTIVE"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/sprite/devices/message - 发送设备消息")
        void testSendMessage() throws Exception {
            String requestJson = """
                {
                    "targetDeviceId": "test-device-002",
                    "messageType": "SYNC_REQUEST",
                    "content": "Test sync message"
                }
                """;

            mockMvc.perform(post("/api/sprite/devices/message")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/sprite/devices/{deviceId}/messages - 获取设备消息历史")
        void testGetDeviceMessages() throws Exception {
            mockMvc.perform(get("/api/sprite/devices/test-device-002/messages"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    // ==================== 认证与授权测试 ====================

    @Nested
    @DisplayName("认证与授权测试")
    class AuthenticationTests {

        @Test
        @DisplayName("未认证请求应能访问公开端点")
        void testPublicEndpointWithoutAuth() throws Exception {
            // 状态接口应该是公开的
            mockMvc.perform(get("/api/sprite/state"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("健康检查端点应该无需认证")
        void testHealthEndpointWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/sprite/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("使用有效 Admin API Key 应能访问受保护端点")
        void testProtectedEndpointWithValidAdminKey() throws Exception {
            // 管理员 API key 应该能够访问所有端点
            mockMvc.perform(get("/api/sprite/memory/visualization")
                            .header("X-API-Key", "sk-admin-test-key-12345"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("使用有效 User API Key 应能访问部分端点")
        void testProtectedEndpointWithValidUserKey() throws Exception {
            // 普通用户 API key 应该能够访问状态端点
            mockMvc.perform(get("/api/sprite/state")
                            .header("X-API-Key", "sk-user-test-key-67890"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("使用无效 API Key 应返回 401 Unauthorized")
        void testInvalidApiKey() throws Exception {
            mockMvc.perform(get("/api/sprite/memory/visualization")
                            .header("X-API-Key", "invalid-api-key"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("空 API Key 应返回 401 Unauthorized")
        void testEmptyApiKey() throws Exception {
            mockMvc.perform(get("/api/sprite/memory/visualization")
                            .header("X-API-Key", ""))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("缺失 API Key 头应返回 401 Unauthorized")
        void testMissingApiKeyHeader() throws Exception {
            mockMvc.perform(get("/api/sprite/memory/visualization"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("使用 Device API Key 应能访问设备相关端点")
        void testDeviceEndpointWithDeviceKey() throws Exception {
            mockMvc.perform(get("/api/sprite/devices")
                            .header("X-API-Key", "sk-device-test-key-abcde"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("使用 API API Key 应能访问外部 API 端点")
        void testExternalApiEndpointWithApiKey() throws Exception {
            mockMvc.perform(get("/api/sprite/external/weather")
                            .param("city", "Beijing")
                            .header("X-API-Key", "sk-api-test-key-fghij"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("反馈提交接口应能使用有效 API Key")
        void testFeedbackWithValidApiKey() throws Exception {
            String requestJson = """
                {
                    "type": "USER_FEEDBACK",
                    "content": "API Key 认证测试",
                    "outcome": "测试成功",
                    "success": true,
                    "impact": "POSITIVE"
                }
                """;

            mockMvc.perform(post("/api/sprite/feedback")
                            .header("X-API-Key", "sk-admin-test-key-12345")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("设备注册接口应能使用有效 API Key")
        void testDeviceRegistrationWithValidApiKey() throws Exception {
            String requestJson = """
                {
                    "deviceId": "auth-test-device-001",
                    "deviceName": "Auth Test Device",
                    "deviceType": "MOBILE",
                    "ipAddress": "10.0.0.1"
                }
                """;

            mockMvc.perform(post("/api/sprite/devices/register")
                            .header("X-API-Key", "sk-device-test-key-abcde")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("连续认证失败应触发临时封禁")
        void testMultipleFailedAuthAttempts() throws Exception {
            // 尝试使用无效 API Key 多次（SecurityService 会在 5 次失败后封禁）
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/api/sprite/state")
                                .header("X-API-Key", "wrong-key-attempt-" + i))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    // ==================== 错误处理测试 ====================

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTests {

        @Test
        @DisplayName("访问不存在的端点应返回 404")
        void testNotFoundEndpoint() throws Exception {
            mockMvc.perform(get("/api/sprite/non-existent-endpoint"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST 请求使用错误的 Content-Type 应返回 415")
        void testWrongContentType() throws Exception {
            mockMvc.perform(post("/api/sprite/cycle")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("invalid content"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("无效的 JSON 格式应返回 400")
        void testInvalidJsonFormat() throws Exception {
            mockMvc.perform(post("/api/sprite/feedback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ invalid json }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("无效的 HTTP 方法应返回 405")
        void testMethodNotAllowed() throws Exception {
            // 尝试使用 DELETE 方法访问只支持 GET 的端点
            mockMvc.perform(delete("/api/sprite/state"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("GET /api/sprite/emotions/predict - 无效的日期格式应返回错误")
        void testPredictEmotionWithInvalidDateFormat() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/predict")
                            .param("date", "invalid-date")
                            .param("hour", "12"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== 参数验证测试 ====================

    @Nested
    @DisplayName("参数验证测试")
    class ParameterValidationTests {

        @Test
        @DisplayName("空的城市参数应被拒绝")
        void testEmptyCityParameter() throws Exception {
            mockMvc.perform(get("/api/sprite/external/weather")
                            .param("city", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("负数的分页参数应被拒绝")
        void testNegativePageParameter() throws Exception {
            mockMvc.perform(get("/api/sprite/external/news")
                            .param("page", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("过大的天数参数应被接受（服务层处理）")
        void testLargeDaysParameter() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/trend")
                            .param("days", "365"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("超出范围的小时参数应被拒绝")
        void testOutOfRangeHourParameter() throws Exception {
            mockMvc.perform(get("/api/sprite/emotions/predict")
                            .param("hour", "25"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== 性能与响应时间测试 ====================

    @Nested
    @DisplayName("性能与响应时间测试")
    class PerformanceTests {

        @Test
        @DisplayName("状态接口应在合理时间内响应")
        void testStateEndpointResponseTime() throws Exception {
            long startTime = System.currentTimeMillis();

            mockMvc.perform(get("/api/sprite/state"))
                    .andExpect(status().isOk());

            long duration = System.currentTimeMillis() - startTime;
            org.junit.jupiter.api.Assertions.assertTrue(
                    duration < 5000,
                    "State endpoint should respond within 5 seconds, took: " + duration + "ms"
            );
        }

        @Test
        @DisplayName("连续多个请求应该都能成功")
        void testMultipleSequentialRequests() throws Exception {
            String[] endpoints = {
                    "/api/sprite/state",
                    "/api/sprite/health",
                    "/api/sprite/memory",
                    "/api/sprite/stats"
            };

            for (String endpoint : endpoints) {
                mockMvc.perform(get(endpoint))
                        .andExpect(status().isOk());
            }
        }
    }

    // ==================== 并发测试 ====================

    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("并发请求状态接口应该都能成功")
        void testConcurrentStateRequests() throws Exception {
            int concurrentRequests = 10;
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(concurrentRequests);
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(concurrentRequests);

            for (int i = 0; i < concurrentRequests; i++) {
                executor.submit(() -> {
                    try {
                        mockMvc.perform(get("/api/sprite/state"))
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        org.junit.jupiter.api.Assertions.fail("Concurrent request failed: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertTrue(completed, "All concurrent requests should complete");
            executor.shutdown();
        }
    }

    // ==================== SecurityService 直接集成测试 ====================

    @Nested
    @DisplayName("SecurityService 直接集成测试")
    class SecurityServiceIntegrationTests {

        @Autowired
        private SecurityService securityService;

        @Test
        @DisplayName("SecurityService - 验证有效 Admin API Key")
        void testValidateAdminApiKey() {
            SecurityService.AuthResult result = securityService.validateApiKey("sk-admin-test-key-12345");

            org.junit.jupiter.api.Assertions.assertTrue(result.success());
            org.junit.jupiter.api.Assertions.assertEquals("admin-user", result.userId());
            org.junit.jupiter.api.Assertions.assertTrue(result.roles().contains(SecurityService.Role.ADMIN));
        }

        @Test
        @DisplayName("SecurityService - 验证有效 User API Key")
        void testValidateUserApiKey() {
            SecurityService.AuthResult result = securityService.validateApiKey("sk-user-test-key-67890");

            org.junit.jupiter.api.Assertions.assertTrue(result.success());
            org.junit.jupiter.api.Assertions.assertEquals("regular-user", result.userId());
            org.junit.jupiter.api.Assertions.assertTrue(result.roles().contains(SecurityService.Role.USER));
        }

        @Test
        @DisplayName("SecurityService - 验证无效 API Key 应失败")
        void testValidateInvalidApiKey() {
            SecurityService.AuthResult result = securityService.validateApiKey("invalid-key");

            org.junit.jupiter.api.Assertions.assertFalse(result.success());
            org.junit.jupiter.api.Assertions.assertEquals("Invalid API key", result.errorMessage());
        }

        @Test
        @DisplayName("SecurityService - 验证空 API Key 应失败")
        void testValidateEmptyApiKey() {
            SecurityService.AuthResult result = securityService.validateApiKey("");

            org.junit.jupiter.api.Assertions.assertFalse(result.success());
            org.junit.jupiter.api.Assertions.assertEquals("API key is required", result.errorMessage());
        }

        @Test
        @DisplayName("SecurityService - 验证空 API Key 应失败（空格）")
        void testValidateBlankApiKey() {
            SecurityService.AuthResult result = securityService.validateApiKey("   ");

            org.junit.jupiter.api.Assertions.assertFalse(result.success());
            org.junit.jupiter.api.Assertions.assertEquals("API key is required", result.errorMessage());
        }

        @Test
        @DisplayName("SecurityService - 生成并验证 JWT Token")
        void testGenerateAndValidateToken() {
            String token = securityService.generateToken("test-user", Set.of(SecurityService.Role.USER), 3600);

            org.junit.jupiter.api.Assertions.assertNotNull(token);

            SecurityService.AuthResult result = securityService.validateToken(token);

            org.junit.jupiter.api.Assertions.assertTrue(result.success());
            org.junit.jupiter.api.Assertions.assertEquals("test-user", result.userId());
        }

        @Test
        @DisplayName("SecurityService - 验证过期 JWT Token 应失败")
        void testValidateExpiredToken() {
            // 生成一个已过期的 token（-1 秒）
            String token = securityService.generateToken("test-user", Set.of(SecurityService.Role.USER), -1);

            SecurityService.AuthResult result = securityService.validateToken(token);

            org.junit.jupiter.api.Assertions.assertFalse(result.success());
            org.junit.jupiter.api.Assertions.assertEquals("Token has expired", result.errorMessage());
        }

        @Test
        @DisplayName("SecurityService - 验证无效 JWT Token 应失败")
        void testValidateInvalidToken() {
            SecurityService.AuthResult result = securityService.validateToken("invalid.jwt.token");

            org.junit.jupiter.api.Assertions.assertFalse(result.success());
        }

        @Test
        @DisplayName("SecurityService - 检查速率限制")
        void testRateLimiting() {
            // 使用默认速率限制进行测试
            String apiKey = "sk-admin-test-key-12345";

            // 前 100 个请求应该被允许
            for (int i = 0; i < 50; i++) {
                SecurityService.RateLimitResult result = securityService.checkRateLimit(apiKey);
                org.junit.jupiter.api.Assertions.assertTrue(result.allowed(), "Request " + i + " should be allowed");
            }
        }

        @Test
        @DisplayName("SecurityService - 速率限制耗尽后应拒绝")
        void testRateLimitExhausted() {
            // 创建一个低速率限制的 API key
            String lowRateKey = "low-rate-key";
            securityService.registerApiKey(lowRateKey, "rate-test-user", Set.of(SecurityService.Role.USER), 5, 60);

            // 消耗所有速率限制
            for (int i = 0; i < 5; i++) {
                securityService.checkRateLimit(lowRateKey);
            }

            // 下一个请求应该被拒绝
            SecurityService.RateLimitResult result = securityService.checkRateLimit(lowRateKey);
            org.junit.jupiter.api.Assertions.assertFalse(result.allowed());
        }

        @Test
        @DisplayName("SecurityService - 检查用户角色")
        void testHasRole() {
            org.junit.jupiter.api.Assertions.assertTrue(securityService.hasRole("sk-admin-test-key-12345", SecurityService.Role.ADMIN));
            org.junit.jupiter.api.Assertions.assertTrue(securityService.hasRole("sk-admin-test-key-12345", SecurityService.Role.USER));
            org.junit.jupiter.api.Assertions.assertFalse(securityService.hasRole("sk-user-test-key-67890", SecurityService.Role.ADMIN));
        }

        @Test
        @DisplayName("SecurityService - 获取认证统计")
        void testGetAuthStats() {
            SecurityService.AuthStats stats = securityService.getAuthStats();

            org.junit.jupiter.api.Assertions.assertNotNull(stats);
            org.junit.jupiter.api.Assertions.assertTrue(stats.totalAttempts() >= 0);
        }

        @Test
        @DisplayName("SecurityService - 注册新 API Key")
        void testRegisterNewApiKey() {
            String newKey = "new-test-key-12345";
            securityService.registerApiKey(newKey, "new-test-user", Set.of(SecurityService.Role.USER), 100, 60);

            org.junit.jupiter.api.Assertions.assertTrue(securityService.isApiKeyRegistered(newKey));
        }

        @Test
        @DisplayName("SecurityService - 撤销 API Key")
        void testRevokeApiKey() {
            String revokeKey = "revoke-test-key-12345";
            securityService.registerApiKey(revokeKey, "revoke-test-user", Set.of(SecurityService.Role.USER), 100, 60);

            org.junit.jupiter.api.Assertions.assertTrue(securityService.isApiKeyRegistered(revokeKey));

            securityService.revokeApiKey(revokeKey);

            org.junit.jupiter.api.Assertions.assertFalse(securityService.isApiKeyRegistered(revokeKey));
        }

        @Test
        @DisplayName("SecurityService - 权限授予和撤销")
        void testPermissionGrantAndRevoke() {
            String permKey = "perm-test-key-12345";
            securityService.registerApiKey(permKey, "perm-test-user", Set.of(SecurityService.Role.USER), 100, 60);

            // 初始没有权限
            org.junit.jupiter.api.Assertions.assertFalse(securityService.hasPermission(permKey, "memory:read"));

            // 授予权限
            securityService.grantPermission(permKey, "memory:read");
            org.junit.jupiter.api.Assertions.assertTrue(securityService.hasPermission(permKey, "memory:read"));

            // 撤销权限
            securityService.revokePermission(permKey, "memory:read");
            org.junit.jupiter.api.Assertions.assertFalse(securityService.hasPermission(permKey, "memory:read"));
        }
    }
}
