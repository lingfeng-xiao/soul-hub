package com.lingfeng.sprite.agent;

/**
 * Worker specialization types
 */
public enum WorkerType {
    PERCEPTION("perception"),
    COGNITION("cognition"),
    ACTION("action");

    private final String name;

    WorkerType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static WorkerType fromName(String name) {
        for (WorkerType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown worker type: " + name);
    }
}
