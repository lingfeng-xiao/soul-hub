package com.lingfeng.sprite.life;

import com.lingfeng.sprite.controller.dto.AutonomyStatusResponse;
import com.lingfeng.sprite.life.persistence.AutonomyPolicyEntity;
import com.lingfeng.sprite.life.persistence.AutonomyPolicyRepository;
import com.lingfeng.sprite.service.AutonomousConsciousness;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AutonomyPolicyService {

    private static final long POLICY_ID = 1L;

    private final AutonomyPolicyRepository repository;
    private final AutonomousConsciousness consciousness;
    private final LifeJournalService lifeJournalService;

    public AutonomyPolicyService(
            AutonomyPolicyRepository repository,
            AutonomousConsciousness consciousness,
            LifeJournalService lifeJournalService
    ) {
        this.repository = repository;
        this.consciousness = consciousness;
        this.lifeJournalService = lifeJournalService;
    }

    public AutonomyStatusResponse getStatus() {
        AutonomyPolicyEntity entity = getOrCreate();
        AutonomousConsciousness.ConsciousnessState state = consciousness.getConsciousnessState();
        return new AutonomyStatusResponse(
                entity.getMode(),
                entity.isPaused(),
                entity.isAllowInternal(),
                entity.isAllowReadonly(),
                entity.isAllowMutating(),
                state.autonomyFactor(),
                state.level().name(),
                state.totalDecisions(),
                state.autonomousDecisions(),
                state.recentAutonomousActions(),
                entity.getUpdatedAt()
        );
    }

    public AutonomyStatusResponse pause() {
        AutonomyPolicyEntity entity = getOrCreate();
        entity.setPaused(true);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
        consciousness.setAutonomyThreshold(1.0f);
        lifeJournalService.record("AUTONOMY", "Autonomy paused", "Autonomous actions are now paused.", entity);
        return getStatus();
    }

    public AutonomyStatusResponse resume() {
        AutonomyPolicyEntity entity = getOrCreate();
        entity.setPaused(false);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
        consciousness.setAutonomyThreshold(0.45f);
        lifeJournalService.record("AUTONOMY", "Autonomy resumed", "Autonomous actions are active again.", entity);
        return getStatus();
    }

    public void reset() {
        AutonomyPolicyEntity entity = AutonomyPolicyEntity.defaults();
        entity.setId(POLICY_ID);
        repository.save(entity);
        consciousness.resetCounters();
        consciousness.setAutonomyThreshold(0.45f);
    }

    private AutonomyPolicyEntity getOrCreate() {
        return repository.findById(POLICY_ID).orElseGet(() -> {
            AutonomyPolicyEntity entity = AutonomyPolicyEntity.defaults();
            entity.setId(POLICY_ID);
            return repository.save(entity);
        });
    }
}
