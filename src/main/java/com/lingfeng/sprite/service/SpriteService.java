package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.Sprite;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.llm.MinMaxConfig;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;
import com.lingfeng.sprite.sensor.RealEnvironmentSensor;
import com.lingfeng.sprite.sensor.RealPlatformSensor;
import com.lingfeng.sprite.sensor.RealUserSensor;
import com.lingfeng.sprite.action.ActionResult;
import com.lingfeng.sprite.action.QuickReactionHandler;
import com.lingfeng.sprite.config.AppConfig;
import com.lingfeng.sprite.llm.MinMaxConfig;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;

/**
 * Sprite 核心服务
 *
 * 封装 Sprite 实例的管理，提供认知循环触发、状态获取、反馈记录等功能
 */
@Service
public class SpriteService {

    private static final Logger logger = LoggerFactory.getLogger(SpriteService.class);

    private final Sprite sprite;
    private final MemorySystem.Memory memory;
    private final EvolutionEngine.Engine evolutionEngine;
    private final MemoryConsolidationService memoryConsolidationService;
    private final EvolutionService evolutionService;
    private final ActionExecutor actionExecutor;
    private final UnifiedContextService unifiedContextService;
    private final AvatarService avatarService;
    private final WebhookService webhookService;
    private final MultiDeviceCoordinationService multiDeviceCoordinationService;
    private final QuickReactionHandler quickReactionHandler;

    public SpriteService(
            AppConfig appConfig,
            MinMaxConfig minMaxConfig,
            MinMaxLlmReasoner minMaxLlmReasoner,
            MemoryConsolidationService memoryConsolidationService,
            EvolutionService evolutionService,
            ActionExecutor actionExecutor,
            MemorySystem.Memory memory,
            UnifiedContextService unifiedContextService,
            AvatarService avatarService,
            WebhookService webhookService,
            MultiDeviceCoordinationService multiDeviceCoordinationService,
            QuickReactionHandler quickReactionHandler
    ) {
        this.memoryConsolidationService = memoryConsolidationService;
        this.evolutionService = evolutionService;
        this.actionExecutor = actionExecutor;
        this.memory = memory;
        this.unifiedContextService = unifiedContextService;
        this.avatarService = avatarService;
        this.webhookService = webhookService;
        this.multiDeviceCoordinationService = multiDeviceCoordinationService;
        this.quickReactionHandler = quickReactionHandler;

        // 加载已保存的长期记忆
        this.memory.load();

        // 创建感知系统（使用真实传感器）
        PerceptionSystem.DeviceType deviceType = PerceptionSystem.DeviceType.PC;
        PerceptionSystem.System perceptionSystem = new PerceptionSystem.System(
                java.util.List.of(
                        new RealPlatformSensor("pc-1", deviceType),
                        new RealUserSensor(),
                        new RealEnvironmentSensor()
                )
        );

        // 创建自我模型
        SelfModel.Self selfModel = SelfModel.Self.createDefault();

        // 注册当前设备分身
        avatarService.registerCurrentDevice();

        // 将分身列表注入到自我模型
        SelfModel.Avatars avatars = new SelfModel.Avatars(avatarService.getAllAvatars());
        selfModel = new SelfModel.Self(
            selfModel.identity(),
            selfModel.personality(),
            selfModel.capabilities(),
            avatars,
            selfModel.metacognition(),
            selfModel.growthHistory(),
            selfModel.evolutionLevel(),
            selfModel.evolutionCount(),
            selfModel.learnedSkills(),
            selfModel.selfGoals(),
            selfModel.learningMetrics(),
            selfModel.autonomousState()
        );

        // 创建世界模型（使用配置的owner信息）
        OwnerModel.Owner configuredOwner = new OwnerModel.Owner(
            new OwnerModel.OwnerIdentity(
                appConfig.getOwner().getName() != null ? appConfig.getOwner().getName() : "灵锋",
                appConfig.getOwner().getOccupation(),
                List.of()
            ),
            new OwnerModel.LifeContext(
                appConfig.getOwner().getWorkplace(),
                appConfig.getOwner().getHome(),
                new OwnerModel.Family(List.of()),
                List.of()
            ),
            List.of(), List.of(), List.of(),
            null,
            List.of(), List.of(),
            new OwnerModel.TrustLevel(0.5f),
            null, null,
            new OwnerModel.DigitalFootprint(List.of(), List.of(), List.of()),
            List.of(),
            Instant.now()
        );
        WorldModel.World worldModel = new WorldModel.World(configuredOwner);

        // 创建推理引擎（使用真实 LLM）
        com.lingfeng.sprite.cognition.ReasoningEngine reasoningEngine = null;
        if (appConfig.getLlm().isEnabled()) {
            reasoningEngine = new com.lingfeng.sprite.cognition.ReasoningEngine(
                    minMaxLlmReasoner
            );
        }

        // 创建认知控制器
        CognitionController cognitionController = new CognitionController(
                perceptionSystem,
                memory,
                selfModel,
                worldModel,
                reasoningEngine
        );

        // 创建进化引擎
        this.evolutionEngine = EvolutionEngine.Factory.create();

        // 创建 Sprite 实例
        this.sprite = Sprite.createWithComponents(
                selfModel,
                Sprite.Platform.PC,
                cognitionController,
                this.memory,
                evolutionEngine,
                reasoningEngine
        );

        logger.info("SpriteService initialized with LLM support: {}",
                appConfig.getLlm().isEnabled());
    }

    /**
     * 执行一轮完整认知闭环
     */
    public CognitionController.CognitionResult cognitionCycle() {
        logger.debug("Starting cognition cycle");

        // 更新当前设备的心跳
        avatarService.updateLastSeen(avatarService.getCurrentDeviceId());

        // S14-3: 广播状态到其他设备
        Sprite.State currentState = sprite.getState();
        multiDeviceCoordinationService.broadcast(
            MultiDeviceCoordinationService.MessageType.STATE_SYNC,
            "state:" + currentState.identity().identity().displayName() + ",running:" + currentState.isRunning()
        );

        // 执行认知闭环
        CognitionController.CognitionResult result = sprite.cognitionCycle();

        // 记忆整合
        memoryConsolidationService.consolidateIfNeeded(memory);
        // S13-1: 触发记忆整合事件
        webhookService.triggerEvent(WebhookService.EventType.MEMORY_CONSOLIDATED,
            Map.of("timestamp", Instant.now().toString()));

        // 每轮保存一次记忆（避免丢失）
        memory.save();

        // 应用进化结果
        evolutionService.applyEvolution(sprite);
        // S13-1: 触发进化事件
        webhookService.triggerEvent(WebhookService.EventType.EVOLUTION_TRIGGERED,
            Map.of("timestamp", Instant.now().toString()));

        // 执行推荐动作（优先使用决策引擎生成的可执行动作）
        if (result.decisionResult() != null && result.decisionResult().hasActions()) {
            // 使用 DecisionEngine 生成的可执行 ToolCall
            for (var toolCall : result.decisionResult().actions()) {
                ActionResult execResult = actionExecutor.executeTool(
                    toolCall.tool(),
                    buildActionContext(toolCall.params(), result)
                );
                logger.info("Executed tool '{}': success={}, message={}",
                    toolCall.tool(), execResult.success(), execResult.message());

                // S13-1: 触发动作执行事件
                webhookService.triggerEvent(WebhookService.EventType.ACTION_EXECUTED,
                    Map.of(
                        "timestamp", Instant.now().toString(),
                        "tool", toolCall.tool(),
                        "success", execResult.success()
                    ));

                // 收集动作执行反馈到进化引擎
                recordActionFeedback(toolCall.tool(), execResult);
            }
        } else if (result.actionRecommendation() != null
                && !result.actionRecommendation().recommendations().isEmpty()) {
            // 降级：使用旧的字符串格式
            for (String action : result.actionRecommendation().recommendations()) {
                ActionResult execResult = actionExecutor.execute(action, buildActionContext(null, result));
                // S13-1: 触发动作执行事件
                webhookService.triggerEvent(WebhookService.EventType.ACTION_EXECUTED,
                    Map.of(
                        "timestamp", Instant.now().toString(),
                        "tool", action,
                        "success", execResult.success()
                    ));
                // 收集动作执行反馈到进化引擎
                recordActionFeedback(action, execResult);
            }
        }

        // 更新统一上下文（供 ConversationService 使用）
        unifiedContextService.updateContext(
                result.perception(),
                result.selfModel(),
                result.worldModel(),
                memory,
                sprite.getEvolutionEngine()
        );

        logger.debug("Cognition cycle completed");

        // S13-1: 触发决策事件
        if (result.decisionResult() != null && result.decisionResult().hasActions()) {
            String actionName = result.decisionResult().actions().isEmpty() ? "none" :
                result.decisionResult().actions().get(0).tool().name();
            webhookService.triggerEvent(WebhookService.EventType.DECISION_MADE,
                Map.of(
                    "timestamp", Instant.now().toString(),
                    "action", actionName
                ));
        }

        return result;
    }

    /**
     * 构建动作执行上下文
     */
    private Map<String, Object> buildActionContext(
            Map<String, Object> toolParams,
            CognitionController.CognitionResult result
    ) {
        Map<String, Object> context = new java.util.concurrent.ConcurrentHashMap<>();
        context.put("perception", result.perception());
        context.put("reflection", result.reflection());
        context.put("timestamp", Instant.now());

        // 如果有工具参数，合并到上下文中
        if (toolParams != null) {
            context.putAll(toolParams);
        }

        return context;
    }

    /**
     * 记录动作执行反馈到进化引擎
     */
    private void recordActionFeedback(String actionType, ActionResult execResult) {
        try {
            EvolutionEngine.Feedback feedback = new EvolutionEngine.Feedback.OutcomeFeedback(
                Instant.now(),
                actionType,
                execResult.success(),
                execResult.message(),
                calculateImpact(execResult)
            );
            sprite.recordFeedback(
                EvolutionEngine.Feedback.FeedbackSource.OUTCOME_SUCCESS,
                actionType + ": " + execResult.message(),
                execResult.message(),
                execResult.success(),
                calculateImpact(execResult)
            );
            logger.debug("Recorded action feedback: action={}, success={}",
                actionType, execResult.success());
        } catch (Exception e) {
            logger.warn("Failed to record action feedback: {}", e.getMessage());
        }
    }

    /**
     * 根据执行结果计算影响度
     */
    private EvolutionEngine.Impact calculateImpact(ActionResult execResult) {
        if (execResult.success()) {
            // 成功的动作根据消息内容判断影响度
            String msg = execResult.message() != null ? execResult.message().toLowerCase() : "";
            if (msg.contains("找到") || msg.contains("成功")) {
                return EvolutionEngine.Impact.HIGH;
            } else if (msg.contains("失败") || msg.contains("错误")) {
                return EvolutionEngine.Impact.LOW;
            }
            return EvolutionEngine.Impact.MEDIUM;
        } else {
            return EvolutionEngine.Impact.LOW;  // 失败时影响度为低
        }
    }

    /**
     * 获取当前状态
     */
    public Sprite.State getState() {
        return sprite.getState();
    }

    /**
     * 记录反馈
     */
    public void recordFeedback(EvolutionEngine.Feedback.FeedbackSource type, String content,
            String outcome, boolean success, EvolutionEngine.Impact impact) {
        sprite.recordFeedback(type, content, outcome, success, impact);
    }

    /**
     * 获取记忆状态
     */
    public MemorySystem.MemoryStatus getMemoryStatus() {
        return sprite.getMemoryStatus();
    }

    /**
     * S12-1: 获取Memory对象供Dashboard可视化使用
     */
    public MemorySystem.Memory getMemory() {
        return memory;
    }

    /**
     * S12-3: 获取EvolutionEngine供Dashboard使用
     */
    public EvolutionEngine.Engine getEvolutionEngine() {
        return evolutionEngine;
    }

    /**
     * 获取进化状态
     */
    public EvolutionEngine.EvolutionStatus getEvolutionStatus() {
        return sprite.getEvolutionStatus();
    }

    /**
     * 获取认知统计
     */
    public CognitionController.CognitionStats getCognitionStats() {
        return sprite.getCognitionStats();
    }

    /**
     * 启动 Sprite
     */
    public void start() {
        sprite.start();
        logger.info("Sprite started");
        // S14-2: 注册当前设备到多设备协同服务
        multiDeviceCoordinationService.registerCurrentDevice();
        // S13-1: 触发Sprite启动事件
        webhookService.triggerEvent(WebhookService.EventType.SPRITE_STARTED,
            Map.of("timestamp", Instant.now().toString()));
    }

    /**
     * 停止 Sprite
     */
    public void stop() {
        // S13-1: 触发Sprite停止事件（在实际停止前）
        webhookService.triggerEvent(WebhookService.EventType.SPRITE_STOPPED,
            Map.of("timestamp", Instant.now().toString()));
        sprite.stop();
        logger.info("Sprite stopped");
    }

    /**
     * S16-1: 处理快速反应
     *
     * 检查输入是否可以通过快速通道处理
     *
     * @param input 用户输入
     * @return 快速反应结果，如果不能绕过则返回null
     */
    public CognitionController.QuickReactionResult handleQuickReaction(String input) {
        if (quickReactionHandler == null) {
            logger.warn("QuickReactionHandler not available");
            return null;
        }

        // 获取认知控制器
        CognitionController cognitionController = sprite.getCognitionController();
        if (cognitionController == null) {
            logger.warn("CognitionController not available");
            return null;
        }

        return cognitionController.handleQuickReaction(input, quickReactionHandler);
    }

    /**
     * S16-2: 处理紧急事件队列
     *
     * 在每轮认知循环中调用，处理积压的紧急事件
     */
    public void processUrgentEvents() {
        if (quickReactionHandler == null) {
            return;
        }

        QuickReactionHandler.UrgentEvent event;
        int processed = 0;
        int maxProcess = 10; // 每轮最多处理10个紧急事件

        while (processed < maxProcess && (event = quickReactionHandler.pollUrgentEvent()) != null) {
            logger.info("Processing urgent event: id={}, priority={}, content={}",
                event.eventId(), event.priority(), event.content());

            // 紧急事件触发通知动作
            ActionResult result = actionExecutor.executeTool("NotifyAction",
                Map.of(
                    "actionParam", "紧急事件: " + event.content(),
                    "priority", event.priority(),
                    "timestamp", Instant.now()
                )
            );

            // 记录反馈
            if (result != null) {
                recordActionFeedback("URGENT:" + event.eventId(), result);
            }

            // S13-1: 触发紧急事件处理事件
            webhookService.triggerEvent(WebhookService.EventType.ACTION_EXECUTED,
                Map.of(
                    "timestamp", Instant.now().toString(),
                    "tool", "NotifyAction",
                    "success", result != null && result.success(),
                    "urgent", true,
                    "eventId", event.eventId()
                ));

            processed++;
        }

        if (processed > 0) {
            logger.info("Processed {} urgent events", processed);
        }
    }

    /**
     * S16-4: 异步执行动作并注册回调
     *
     * @param action 动作名称
     * @param context 执行上下文
     * @param actionId 动作ID用于追踪
     * @return 异步结果
     */
    public CompletableFuture<ActionResult> executeAsyncAction(String action, Map<String, Object> context, String actionId) {
        logger.debug("Registering async action: id={}, action={}", actionId, action);

        // 注册到快速反应处理器
        if (quickReactionHandler != null) {
            quickReactionHandler.registerAsyncAction(actionId, new QuickReactionHandler.SimpleFuture<>());
        }

        // 异步执行
        return actionExecutor.executeAsync(action, context, actionId);
    }

    /**
     * S16-4: 完成异步动作
     */
    public void completeAsyncAction(String actionId, ActionResult result) {
        if (quickReactionHandler != null) {
            quickReactionHandler.completeAsyncAction(actionId, result);
        }
    }

    /**
     * S16-2: 获取紧急事件队列大小
     */
    public int getUrgentQueueSize() {
        return quickReactionHandler != null ? quickReactionHandler.getUrgentQueueSize() : 0;
    }
}
