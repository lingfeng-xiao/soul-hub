package com.lingfeng.sprite.domain.relationship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RelationshipService 单元测试
 *
 * 测试关系领域服务的核心功能：
 * - 关系初始化
 * - 信任管理（增加/减少/等级检查）
 * - 共享项目管理
 * - 关怀优先级
 * - 交互记录
 * - 完整性检查
 */
class RelationshipServiceTest {

    private RelationshipService service;

    @BeforeEach
    void setUp() {
        service = new RelationshipService();
        service.initialize("owner-123");
    }

    // ==================== 初始化测试 ====================

    @Test
    @DisplayName("initialize 应该设置所有字段")
    void testInitialize_setsAllFields() {
        assertEquals("owner-123", service.getOwnerId());
        assertNotNull(service.getProfile());
        assertNotNull(service.getTrustState());
        assertFalse(service.getCarePriorities().isEmpty());
    }

    @Test
    @DisplayName("initialize 应该创建默认关系配置")
    void testInitialize_createsDefaultProfile() {
        assertEquals(RelationshipProfile.RelationshipType.FRIEND, service.getProfile().getType());
        assertEquals(0.5f, service.getProfile().getStrength(), 0.001f);
        assertEquals(0, service.getProfile().getInteractionCount());
    }

    @Test
    @DisplayName("initialize 应该创建默认信任状态")
    void testInitialize_createsDefaultTrustState() {
        assertEquals(TrustState.TrustLevel.MEDIUM, service.getTrustState().getLevel());
        assertEquals(0.5f, service.getTrustState().getScore(), 0.001f);
    }

    @Test
    @DisplayName("initialize 应该创建默认关怀优先级")
    void testInitialize_createsDefaultCarePriorities() {
        assertEquals(4, service.getCarePriorities().size());
    }

    @Test
    @DisplayName("isComplete 应该在初始化后返回 true")
    void testIsComplete_afterInitialization_returnsTrue() {
        assertTrue(service.isComplete());
    }

    // ==================== 交互记录测试 ====================

    @Test
    @DisplayName("recordInteraction 应该增加交互次数")
    void testRecordInteraction_incrementsCount() {
        int originalCount = service.getProfile().getInteractionCount();

        service.recordInteraction();

        assertEquals(originalCount + 1, service.getProfile().getInteractionCount());
    }

    @Test
    @DisplayName("recordInteraction 应该增加关系强度")
    void testRecordInteraction_increasesStrength() {
        float originalStrength = service.getProfile().getStrength();

        service.recordInteraction();

        assertTrue(service.getProfile().getStrength() > originalStrength);
    }

    // ==================== 关系强度测试 ====================

    @Test
    @DisplayName("updateStrength 应该更新关系强度")
    void testUpdateStrength_updatesStrength() {
        service.updateStrength(0.8f);

        assertEquals(0.8f, service.getProfile().getStrength(), 0.001f);
    }

    @Test
    @DisplayName("updateStrength 应该 clamps 到范围 [0, 1]")
    void testUpdateStrength_clampsToRange() {
        service.updateStrength(1.5f);
        assertEquals(1.0f, service.getProfile().getStrength(), 0.001f);

        service.updateStrength(-0.5f);
        assertEquals(0.0f, service.getProfile().getStrength(), 0.001f);
    }

    // ==================== 关系类型测试 ====================

    @Test
    @DisplayName("updateRelationshipType 应该更新关系类型")
    void testUpdateRelationshipType_updatesType() {
        service.updateRelationshipType(RelationshipProfile.RelationshipType.FAMILY);

        assertEquals(RelationshipProfile.RelationshipType.FAMILY, service.getProfile().getType());
    }

    // ==================== 信任管理测试 ====================

    @Test
    @DisplayName("increaseTrust 应该增加信任分数")
    void testIncreaseTrust_increasesScore() {
        float originalScore = service.getTrustState().getScore();

        service.increaseTrust(0.2f);

        assertTrue(service.getTrustState().getScore() > originalScore);
    }

    @Test
    @DisplayName("increaseTrust 应该 clamps 到最大值 1.0")
    void testIncreaseTrust_clampsToMaximum() {
        service.increaseTrust(1.0f);

        assertEquals(1.0f, service.getTrustState().getScore(), 0.001f);
        assertEquals(TrustState.TrustLevel.FULL, service.getTrustState().getLevel());
    }

    @Test
    @DisplayName("increaseTrust 应该更新信任等级")
    void testIncreaseTrust_updatesLevel() {
        service.increaseTrust(0.3f);

        assertEquals(TrustState.TrustLevel.HIGH, service.getTrustState().getLevel());
    }

    @Test
    @DisplayName("decreaseTrust 应该减少信任分数")
    void testDecreaseTrust_decreasesScore() {
        float originalScore = service.getTrustState().getScore();

        service.decreaseTrust(0.2f);

        assertTrue(service.getTrustState().getScore() < originalScore);
    }

    @Test
    @DisplayName("decreaseTrust 应该 clamps 到最小值 0.0")
    void testDecreaseTrust_clampsToMinimum() {
        service.decreaseTrust(1.0f);

        assertEquals(0.0f, service.getTrustState().getScore(), 0.001f);
    }

    @Test
    @DisplayName("decreaseTrust 应该更新信任等级")
    void testDecreaseTrust_updatesLevel() {
        service.decreaseTrust(0.3f);

        assertEquals(TrustState.TrustLevel.LOW, service.getTrustState().getLevel());
    }

    @Test
    @DisplayName("isTrustAtLeast 应该正确比较信任等级")
    void testIsTrustAtLeast_comparesLevels() {
        // 默认是 MEDIUM
        assertTrue(service.isTrustAtLeast(TrustState.TrustLevel.LOW));
        assertTrue(service.isTrustAtLeast(TrustState.TrustLevel.MEDIUM));
        assertFalse(service.isTrustAtLeast(TrustState.TrustLevel.HIGH));
        assertFalse(service.isTrustAtLeast(TrustState.TrustLevel.FULL));
    }

    @Test
    @DisplayName("isTrustAtLeast 当等级足够时应该返回 true")
    void testIsTrustAtLeast_returnsTrueWhenLevelSufficient() {
        service.increaseTrust(0.5f); // 提升到 FULL (0.5 + 0.5 = 1.0)

        assertTrue(service.isTrustAtLeast(TrustState.TrustLevel.MEDIUM));
        assertTrue(service.isTrustAtLeast(TrustState.TrustLevel.HIGH));
        assertTrue(service.isTrustAtLeast(TrustState.TrustLevel.FULL));
    }

    // ==================== 共享项目测试 ====================

    @Test
    @DisplayName("addSharedProject 应该添加项目")
    void testAddSharedProject_addsProject() {
        service.addSharedProject("proj-1", "测试项目", "这是一个测试项目");

        assertEquals(1, service.getSharedProjects().size());
    }

    @Test
    @DisplayName("addSharedProject 应该设置项目为活跃状态")
    void testAddSharedProject_setsActiveStatus() {
        service.addSharedProject("proj-1", "测试项目", "描述");

        assertEquals(SharedProjectLink.ProjectStatus.ACTIVE,
                service.getSharedProjects().get(0).getStatus());
    }

    @Test
    @DisplayName("updateProjectEngagement 应该更新参与度")
    void testUpdateProjectEngagement_updatesEngagement() {
        service.addSharedProject("proj-1", "测试项目", "描述");

        service.updateProjectEngagement("proj-1", 0.9f);

        assertEquals(0.9f, service.getSharedProjects().get(0).getEngagement(), 0.001f);
    }

    @Test
    @DisplayName("updateProjectEngagement 应该忽略不存在的项目")
    void testUpdateProjectEngagement_ignoresNonExistent() {
        // 不应该抛出异常
        assertDoesNotThrow(() -> service.updateProjectEngagement("non-existent", 0.5f));
    }

    @Test
    @DisplayName("completeProject 应该标记项目为已完成")
    void testCompleteProject_marksCompleted() {
        service.addSharedProject("proj-1", "测试项目", "描述");

        service.completeProject("proj-1");

        assertEquals(SharedProjectLink.ProjectStatus.COMPLETED,
                service.getSharedProjects().get(0).getStatus());
    }

    @Test
    @DisplayName("getActiveProjects 应该只返回活跃项目")
    void testGetActiveProjects_filtersCorrectly() {
        service.addSharedProject("proj-1", "项目1", "描述");
        service.addSharedProject("proj-2", "项目2", "描述");
        service.completeProject("proj-1");

        assertEquals(1, service.getActiveProjects().size());
        assertEquals("proj-2", service.getActiveProjects().get(0).getProjectId());
    }

    // ==================== 关怀优先级测试 ====================

    @Test
    @DisplayName("getTopCarePriority 应该返回最高分关怀")
    void testGetTopCarePriority_returnsHighestScore() {
        CarePriority top = service.getTopCarePriority();

        assertNotNull(top);
        assertEquals(CarePriority.CareType.SAFETY, top.getCareType()); // SAFETY 有最高分
    }

    @Test
    @DisplayName("updateCarePriority 应该更新关怀级别")
    void testUpdateCarePriority_updatesLevel() {
        service.updateCarePriority(CarePriority.CareType.EMOTIONAL,
                CarePriority.PriorityLevel.CRITICAL);

        // 验证更新（需要找到对应的关怀类型）
        CarePriority emotional = service.getCarePriorities().stream()
                .filter(c -> c.getCareType() == CarePriority.CareType.EMOTIONAL)
                .findFirst()
                .orElse(null);

        assertNotNull(emotional);
        assertEquals(CarePriority.PriorityLevel.CRITICAL, emotional.getLevel());
    }

    @Test
    @DisplayName("triggerCare 应该更新最后触发时间")
    void testTriggerCare_updatesTriggeredTime() {
        CarePriority before = service.getTopCarePriority();
        assertNull(before.getLastTriggeredAt());

        service.triggerCare(before.getCareType());

        CarePriority after = service.getCarePriorities().stream()
                .filter(c -> c.getCareType() == before.getCareType())
                .findFirst()
                .orElse(null);

        assertNotNull(after);
        assertNotNull(after.getLastTriggeredAt());
    }

    // ==================== 关系摘要测试 ====================

    @Test
    @DisplayName("getRelationshipSummary 应该返回格式化的摘要")
    void testGetRelationshipSummary_returnsFormattedSummary() {
        String summary = service.getRelationshipSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("关系:"));
        assertTrue(summary.contains("信任:"));
        assertTrue(summary.contains("项目:"));
        assertTrue(summary.contains("关怀:"));
    }

    // ==================== Builder 测试 ====================

    @Test
    @DisplayName("Builder 应该创建完整服务")
    void testBuilder_createsCompleteService() {
        RelationshipService builtService = RelationshipService.builder()
                .ownerId("built-owner")
                .build();

        assertEquals("built-owner", builtService.getOwnerId());
        assertTrue(builtService.isComplete());
    }

    @Test
    @DisplayName("Builder 应该支持自定义配置")
    void testBuilder_supportsCustomConfig() {
        RelationshipProfile customProfile = RelationshipProfile.builder()
                .relationshipId("custom-id")
                .type(RelationshipProfile.RelationshipType.PARTNER)
                .strength(0.9f)
                .build();

        RelationshipService builtService = RelationshipService.builder()
                .ownerId("custom-owner")
                .profile(customProfile)
                .build();

        assertEquals(RelationshipProfile.RelationshipType.PARTNER,
                builtService.getProfile().getType());
        assertEquals(0.9f, builtService.getProfile().getStrength(), 0.001f);
    }
}
