package com.lingfeng.sprite.life.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "autonomy_policy")
public class AutonomyPolicyEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String mode;

    @Column(nullable = false)
    private boolean paused;

    @Column(name = "allow_internal", nullable = false)
    private boolean allowInternal;

    @Column(name = "allow_readonly", nullable = false)
    private boolean allowReadonly;

    @Column(name = "allow_mutating", nullable = false)
    private boolean allowMutating;

    @Lob
    @Column(name = "whitelist_json")
    private String whitelistJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AutonomyPolicyEntity defaults() {
        AutonomyPolicyEntity entity = new AutonomyPolicyEntity();
        entity.id = 1L;
        entity.mode = "HIGH_AUTONOMY";
        entity.paused = false;
        entity.allowInternal = true;
        entity.allowReadonly = true;
        entity.allowMutating = false;
        entity.whitelistJson = "[]";
        entity.updatedAt = Instant.now();
        return entity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public boolean isAllowInternal() { return allowInternal; }
    public void setAllowInternal(boolean allowInternal) { this.allowInternal = allowInternal; }
    public boolean isAllowReadonly() { return allowReadonly; }
    public void setAllowReadonly(boolean allowReadonly) { this.allowReadonly = allowReadonly; }
    public boolean isAllowMutating() { return allowMutating; }
    public void setAllowMutating(boolean allowMutating) { this.allowMutating = allowMutating; }
    public String getWhitelistJson() { return whitelistJson; }
    public void setWhitelistJson(String whitelistJson) { this.whitelistJson = whitelistJson; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
