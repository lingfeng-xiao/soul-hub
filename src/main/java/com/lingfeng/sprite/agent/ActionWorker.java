package com.lingfeng.sprite.agent;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.action.ActionResult;
import com.lingfeng.sprite.mcp.McpClient;
import com.lingfeng.sprite.service.ActionExecutor;
import com.lingfeng.sprite.agent.SkillExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Action Worker - handles action execution including skills and MCP tools
 */
public class ActionWorker extends WorkerAgent {
    private static final Logger logger = LoggerFactory.getLogger(ActionWorker.class);

    private final MemorySystem.Memory memory;
    private final SelfModel.Self selfModel;
    private final WorldModel.World worldModel;
    private ActionExecutor actionExecutor;
    private SkillExecutor skillExecutor;
    private McpClient mcpClient;

    public ActionWorker(String workerId, Mailbox mailbox, AgentRegistry registry,
                        MemorySystem.Memory memory, SelfModel.Self selfModel,
                        WorldModel.World worldModel) {
        super(workerId, WorkerType.ACTION, mailbox, registry);
        this.memory = memory;
        this.selfModel = selfModel;
        this.worldModel = worldModel;
    }

    @Override
    protected void doInitialize() {
        logger.info("ActionWorker {} initialized", workerId);
        actionExecutor = new ActionExecutor(memory);
    }

    /**
     * Set skill executor (called by LeaderAgent after construction)
     */
    public void setSkillExecutor(SkillExecutor skillExecutor) {
        this.skillExecutor = skillExecutor;
    }

    /**
     * Set MCP client (called by LeaderAgent after construction)
     */
    public void setMcpClient(McpClient mcpClient) {
        this.mcpClient = mcpClient;
    }

    @Override
    protected Task doProcessTask(Task task) {
        logger.debug("ActionWorker {} processing {} task", workerId, task.type());

        if ("EXECUTE_ACTION".equals(task.type())) {
            return executeAction(task);
        } else if ("EXECUTE_TOOL".equals(task.type())) {
            return executeTool(task);
        } else if ("EXECUTE_SKILL".equals(task.type())) {
            return executeSkill(task);
        } else if ("EXECUTE_MCP_TOOL".equals(task.type())) {
            return executeMcpTool(task);
        }

        return task.withError("Unknown task type: " + task.type());
    }

    private Task executeAction(Task task) {
        try {
            String actionString = (String) task.parameters().get("actionString");
            if (actionString == null) {
                return task.withError("Missing actionString parameter");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> context = (Map<String, Object>) task.parameters().getOrDefault("context", Map.of());

            ActionResult result = actionExecutor.execute(actionString, context);

            return task.withResult(Map.of(
                "success", result.success(),
                "message", result.message(),
                "data", result.data()
            ));
        } catch (Exception e) {
            logger.error("ActionWorker {} failed to execute action", workerId, e);
            return task.withError(e.getMessage());
        }
    }

    private Task executeTool(Task task) {
        try {
            String toolName = (String) task.parameters().get("toolName");
            if (toolName == null) {
                return task.withError("Missing toolName parameter");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) task.parameters().getOrDefault("params", Map.of());

            ActionResult result = actionExecutor.executeTool(toolName, params);

            return task.withResult(Map.of(
                "success", result.success(),
                "message", result.message(),
                "data", result.data()
            ));
        } catch (Exception e) {
            logger.error("ActionWorker {} failed to execute tool", workerId, e);
            return task.withError(e.getMessage());
        }
    }

    private Task executeSkill(Task task) {
        if (skillExecutor == null) {
            return task.withError("SkillExecutor not available");
        }

        try {
            String skillId = (String) task.parameters().get("skillId");
            if (skillId == null) {
                return task.withError("Missing skillId parameter");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) task.parameters().getOrDefault("parameters", Map.of());

            var result = skillExecutor.executeSkill(skillId, task.taskId(), params, selfModel, worldModel, memory);

            return task.withResult(Map.of(
                "success", result.success(),
                "message", result.message(),
                "data", result.data(),
                "durationMs", result.durationMs()
            ));
        } catch (Exception e) {
            logger.error("ActionWorker {} failed to execute skill", workerId, e);
            return task.withError(e.getMessage());
        }
    }

    private Task executeMcpTool(Task task) {
        if (mcpClient == null) {
            return task.withError("MCP client not available");
        }

        try {
            String serverName = (String) task.parameters().get("serverName");
            String toolName = (String) task.parameters().get("toolName");
            if (serverName == null || toolName == null) {
                return task.withError("Missing serverName or toolName parameter");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> args = (Map<String, Object>) task.parameters().getOrDefault("arguments", Map.of());

            String result = mcpClient.callTool(serverName, toolName, args).join();

            return task.withResult(Map.of(
                "success", true,
                "message", "MCP tool executed successfully",
                "data", result
            ));
        } catch (Exception e) {
            logger.error("ActionWorker {} failed to execute MCP tool", workerId, e);
            return task.withError(e.getMessage());
        }
    }
}
