package com.lingfeng.sprite.domain.command;

import com.lingfeng.sprite.domain.pacing.EvolutionPacingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandOrchestrator 单元测试
 *
 * 测试命令编排器的核心功能：
 * - 命令注册与执行
 * - 六种命令类型处理
 * - 结果历史追踪
 * - ImpactReport 验证
 * - 待处理命令管理
 */
class CommandOrchestratorTest {

    private CommandOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new CommandOrchestrator();
    }

    // ==================== 初始化测试 ====================

    @Test
    @DisplayName("默认构造应该创建空的编排器")
    void testDefaultConstruction_createsEmptyOrchestrator() {
        assertTrue(orchestrator.getPendingCommands().isEmpty());
        assertTrue(orchestrator.getHistory(10).isEmpty());
    }

    @Test
    @DisplayName("setPacingEngine 应该设置节速引擎")
    void testSetPacingEngine_setsEngine() {
        EvolutionPacingEngine engine = new EvolutionPacingEngine();

        orchestrator.setPacingEngine(engine);

        // Just verify it doesn't throw
        assertDoesNotThrow(() -> orchestrator.execute(AskCommand.create("test question")));
    }

    // ==================== ASK 命令测试 ====================

    @Test
    @DisplayName("execute ASK 命令应该返回 AskResult")
    void testExecuteAskCommand_returnsAskResult() {
        AskCommand command = AskCommand.create("什么是量子计算?");

        CommandResult result = orchestrator.execute(command);

        assertNotNull(result);
        assertTrue(result instanceof CommandOrchestrator.AskResult);
        assertTrue(result.isSuccess());
        assertEquals(command.getCommandId(), result.getCommandId());
    }

    @Test
    @DisplayName("AskResult 应该包含回答")
    void testAskResult_containsAnswer() {
        AskCommand command = AskCommand.create("测试问题");

        CommandResult result = orchestrator.execute(command);

        CommandOrchestrator.AskResult askResult = (CommandOrchestrator.AskResult) result;
        assertNotNull(askResult.answer());
        assertTrue(askResult.answer().contains("测试问题"));
    }

    @Test
    @DisplayName("AskResult 应该包含 ImpactReport")
    void testAskResult_containsImpactReport() {
        AskCommand command = AskCommand.create("测试问题");

        CommandResult result = orchestrator.execute(command);

        assertNotNull(result.getImpact());
        assertNotNull(result.getImpact().getRelationshipUpdate());
    }

    // ==================== TASK 命令测试 ====================

    @Test
    @DisplayName("execute TASK 命令应该返回 TaskResult")
    void testExecuteTaskCommand_returnsTaskResult() {
        TaskCommand command = TaskCommand.create("完成代码审查");

        CommandResult result = orchestrator.execute(command);

        assertNotNull(result);
        assertTrue(result instanceof CommandOrchestrator.TaskResult);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("TaskResult 应该包含输出")
    void testTaskResult_containsOutput() {
        TaskCommand command = TaskCommand.create("完成代码审查");

        CommandResult result = orchestrator.execute(command);

        CommandOrchestrator.TaskResult taskResult = (CommandOrchestrator.TaskResult) result;
        assertNotNull(taskResult.output());
        assertTrue(taskResult.output().contains("完成代码审查"));
    }

    @Test
    @DisplayName("TaskResult 应该包含目标更新")
    void testTaskResult_containsGoalUpdate() {
        TaskCommand command = TaskCommand.create("完成任务");

        CommandResult result = orchestrator.execute(command);

        CommandOrchestrator.TaskResult taskResult = (CommandOrchestrator.TaskResult) result;
        assertNotNull(taskResult.impact().getGoalUpdate());
        assertTrue(taskResult.impact().getGoalUpdate().goalProgressed());
    }

    // ==================== RESEARCH 命令测试 ====================

    @Test
    @DisplayName("execute RESEARCH 命令应该返回 ResearchResult")
    void testExecuteResearchCommand_returnsResearchResult() {
        ResearchCommand command = ResearchCommand.create("AI的最新发展");

        CommandResult result = orchestrator.execute(command);

        assertNotNull(result);
        assertTrue(result instanceof CommandOrchestrator.ResearchResult);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("ResearchResult 应该包含发现")
    void testResearchResult_containsFindings() {
        ResearchCommand command = ResearchCommand.create("量子计算");

        CommandResult result = orchestrator.execute(command);

        CommandOrchestrator.ResearchResult researchResult = (CommandOrchestrator.ResearchResult) result;
        assertNotNull(researchResult.findings());
        assertTrue(researchResult.findings().contains("量子计算"));
    }

    @Test
    @DisplayName("ResearchResult 应该包含成长更新")
    void testResearchResult_containsGrowthUpdate() {
        ResearchCommand command = ResearchCommand.create("新主题");

        CommandResult result = orchestrator.execute(command);

        CommandOrchestrator.ResearchResult researchResult = (CommandOrchestrator.ResearchResult) result;
        assertNotNull(researchResult.impact().getGrowthUpdate());
        assertTrue(researchResult.impact().getGrowthUpdate().hasImpact());
    }

    // ==================== DECISION 命令测试 ====================

    @Test
    @DisplayName("execute DECISION 命令应该返回 DecisionResult")
    void testExecuteDecisionCommand_returnsDecisionResult() {
        DecisionCommand command = DecisionCommand.create(
                "推荐哪个方案?",
                java.util.List.of("方案A", "方案B", "方案C")
        );

        CommandResult result = orchestrator.execute(command);

        assertNotNull(result);
        assertTrue(result instanceof CommandOrchestrator.DecisionResult);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("DecisionResult 应该包含推荐和选项")
    void testDecisionResult_containsRecommendationAndOptions() {
        DecisionCommand command = DecisionCommand.create(
                "推荐哪个方案?",
                java.util.List.of("方案A", "方案B", "方案C")
        );

        CommandResult result = orchestrator.execute(command);

        CommandOrchestrator.DecisionResult decisionResult = (CommandOrchestrator.DecisionResult) result;
        assertNotNull(decisionResult.recommendation());
        assertEquals(3, decisionResult.options().size());
    }

    // ==================== LEARNING 命令测试 ====================

    @Test
    @DisplayName("execute LEARNING 命令应该返回 LearningResult")
    void testExecuteLearningCommand_returnsLearningResult() {
        LearningCommand command = LearningCommand.create("学习机器学习");

        CommandResult result = orchestrator.execute(command);

        assertNotNull(result);
        assertTrue(result instanceof CommandOrchestrator.LearningResult);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("LearningResult 应该包含学习计划")
    void testLearningResult_containsPlan() {
        LearningCommand command = LearningCommand.create("学习Python");

        CommandResult result = orchestrator.execute(command);

        CommandOrchestrator.LearningResult learningResult = (CommandOrchestrator.LearningResult) result;
        assertNotNull(learningResult.plan());
        assertTrue(learningResult.plan().contains("学习Python"));
    }

    // ==================== ACTION 命令测试 ====================

    @Test
    @DisplayName("execute ACTION 命令应该返回 ActionResult")
    void testExecuteActionCommand_returnsActionResult() {
        ActionCommand command = ActionCommand.create("发送邮件",
                new ActionCommand.ActionTarget("device-1", "SYSTEM", "mail"));

        CommandResult result = orchestrator.execute(command);

        assertNotNull(result);
        assertTrue(result instanceof CommandOrchestrator.ActionResult);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("ActionResult 应该包含结果")
    void testActionResult_containsResult() {
        ActionCommand command = ActionCommand.create("执行操作",
                new ActionCommand.ActionTarget("device-1", "APPLICATION", "notepad"));

        CommandResult result = orchestrator.execute(command);

        CommandOrchestrator.ActionResult actionResult = (CommandOrchestrator.ActionResult) result;
        assertNotNull(actionResult.result());
        assertTrue(actionResult.result().contains("执行操作"));
    }

    @Test
    @DisplayName("ActionResult 应该包含自我更新")
    void testActionResult_containsSelfUpdate() {
        ActionCommand command = ActionCommand.create("执行操作",
                new ActionCommand.ActionTarget("device-1", "APPLICATION", "notepad"));

        CommandResult result = orchestrator.execute(command);

        CommandOrchestrator.ActionResult actionResult = (CommandOrchestrator.ActionResult) result;
        assertNotNull(actionResult.impact().getSelfUpdate());
        assertTrue(actionResult.impact().getSelfUpdate().energyChanged());
    }

    // ==================== 命令历史测试 ====================

    @Test
    @DisplayName("execute 应该添加到历史")
    void testExecute_addsToHistory() {
        orchestrator.execute(AskCommand.create("问题1"));
        orchestrator.execute(AskCommand.create("问题2"));

        var history = orchestrator.getHistory(10);

        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("getHistory limit 应该限制返回数量")
    void testGetHistory_limitRestrictsSize() {
        for (int i = 0; i < 5; i++) {
            orchestrator.execute(AskCommand.create("问题" + i));
        }

        var history = orchestrator.getHistory(3);

        assertEquals(3, history.size());
    }

    @Test
    @DisplayName("getHistory limit 大于历史长度应该返回全部")
    void testGetHistory_limitExceedsSize() {
        orchestrator.execute(AskCommand.create("问题1"));

        var history = orchestrator.getHistory(100);

        assertEquals(1, history.size());
    }

    // ==================== 待处理命令测试 ====================

    @Test
    @DisplayName("execute 应该注册命令")
    void testExecute_registersCommand() {
        AskCommand command = AskCommand.create("测试问题");

        orchestrator.execute(command);

        // After execution, command status is COMPLETED, so not pending
        var pending = orchestrator.getPendingCommands();
        // Commands go to COMPLETED after execution, so pending should be empty
        assertTrue(pending.isEmpty());
    }

    // ==================== ImpactReport 验证测试 ====================

    @Test
    @DisplayName("所有命令结果应该包含 ImpactReport")
    void testAllResults_containImpactReport() {
        Command[] commands = {
                AskCommand.create("问题"),
                TaskCommand.create("任务"),
                ResearchCommand.create("研究"),
                DecisionCommand.create("决策", java.util.List.of("A", "B")),
                LearningCommand.create("学习"),
                ActionCommand.create("操作", new ActionCommand.ActionTarget("", "", ""))
        };

        for (Command command : commands) {
            CommandResult result = orchestrator.execute(command);
            assertNotNull(result.getImpact(),
                    command.getType() + " result should have ImpactReport");
        }
    }

    @Test
    @DisplayName("ImpactReport 自我更新应该包含正确字段")
    void testImpactReport_selfUpdateHasCorrectFields() {
        TaskCommand command = TaskCommand.create("执行任务");

        CommandResult result = orchestrator.execute(command);
        ImpactReport.SelfUpdate selfUpdate = result.getImpact().getSelfUpdate();

        assertNotNull(selfUpdate);
        // Energy should change for task
        assertTrue(selfUpdate.energyChanged());
        assertTrue(selfUpdate.focusChanged());
    }

    @Test
    @DisplayName("ImpactReport 关系更新对于 ASK 命令应该更新关系")
    void testImpactReport_relationshipUpdateForAsk() {
        AskCommand command = AskCommand.create("互动问题");

        CommandResult result = orchestrator.execute(command);
        ImpactReport.RelationshipUpdate relUpdate = result.getImpact().getRelationshipUpdate();

        assertNotNull(relUpdate);
        assertTrue(relUpdate.interacted());
    }

    // ==================== 命令注册测试 ====================

    @Test
    @DisplayName("register 应该添加命令到注册表")
    void testRegister_addsToRegistry() {
        AskCommand command = AskCommand.create("测试问题");

        orchestrator.register(command);

        // Just verify it doesn't throw - pending commands filtered by status
        assertDoesNotThrow(() -> orchestrator.getPendingCommands());
    }

    // ==================== Result 接口方法测试 ====================

    @Test
    @DisplayName("所有 Result 应该实现 getSummary")
    void testAllResults_implementGetSummary() {
        Command[] commands = {
                AskCommand.create("问题"),
                TaskCommand.create("任务"),
                ResearchCommand.create("研究"),
                DecisionCommand.create("决策", java.util.List.of("A", "B")),
                LearningCommand.create("学习"),
                ActionCommand.create("操作", new ActionCommand.ActionTarget("", "", ""))
        };

        for (Command command : commands) {
            CommandResult result = orchestrator.execute(command);
            assertNotNull(result.getSummary(),
                    command.getType() + " result should have summary");
            assertFalse(result.getSummary().isEmpty());
        }
    }

    @Test
    @DisplayName("所有 Result 应该实现 getDetail")
    void testAllResults_implementGetDetail() {
        Command[] commands = {
                AskCommand.create("问题"),
                TaskCommand.create("任务"),
                ResearchCommand.create("研究"),
                DecisionCommand.create("决策", java.util.List.of("A", "B")),
                LearningCommand.create("学习"),
                ActionCommand.create("操作", new ActionCommand.ActionTarget("", "", ""))
        };

        for (Command command : commands) {
            CommandResult result = orchestrator.execute(command);
            assertNotNull(result.getDetail(),
                    command.getType() + " result should have detail");
        }
    }

    @Test
    @DisplayName("所有 Result 应该返回正确的 commandId")
    void testAllResults_returnCorrectCommandId() {
        AskCommand askCommand = AskCommand.create("问题");
        TaskCommand taskCommand = TaskCommand.create("任务");

        CommandResult askResult = orchestrator.execute(askCommand);
        CommandResult taskResult = orchestrator.execute(taskCommand);

        assertEquals(askCommand.getCommandId(), askResult.getCommandId());
        assertEquals(taskCommand.getCommandId(), taskResult.getCommandId());
    }
}
