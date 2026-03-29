package com.lingfeng.sprite.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Multi-agent system configuration
 */
@Component
@ConfigurationProperties(prefix = "sprite.agent")
public class AgentConfig {

    private boolean enabled = true;
    private LeaderConfig leader = new LeaderConfig();
    private Map<String, WorkerConfig> workers;
    private MailboxConfig mailbox = new MailboxConfig();
    private long intervalMs = 1000; // cognition cycle interval

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LeaderConfig getLeader() {
        return leader;
    }

    public void setLeader(LeaderConfig leader) {
        this.leader = leader;
    }

    public Map<String, WorkerConfig> getWorkers() {
        return workers;
    }

    public void setWorkers(Map<String, WorkerConfig> workers) {
        this.workers = workers;
    }

    public MailboxConfig getMailbox() {
        return mailbox;
    }

    public void setMailbox(MailboxConfig mailbox) {
        this.mailbox = mailbox;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public static class LeaderConfig {
        private String id = "sprite-leader";
        private String workspace = System.getProperty("user.home") + "/.sprites";

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getWorkspace() {
            return workspace;
        }

        public void setWorkspace(String workspace) {
            this.workspace = workspace;
        }
    }

    public static class WorkerConfig {
        private int count = 1;
        private boolean enabled = true;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class MailboxConfig {
        private String basePath = System.getProperty("user.home") + "/.sprites";
        private long pollIntervalMs = 100;

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }
    }
}
