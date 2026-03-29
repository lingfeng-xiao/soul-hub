package com.lingfeng.sprite.domain.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lingfeng.sprite.domain.goal.ActiveIntention;
import com.lingfeng.sprite.domain.goal.GoalService;
import com.lingfeng.sprite.domain.identity.IdentityProfile;
import com.lingfeng.sprite.domain.identity.IdentityService;
import com.lingfeng.sprite.domain.relationship.RelationshipService;
import com.lingfeng.sprite.domain.self.AttentionFocus;
import com.lingfeng.sprite.domain.self.SelfService;
import com.lingfeng.sprite.domain.self.SelfState;
import com.lingfeng.sprite.life.AutonomyPolicyService;
import com.lingfeng.sprite.life.LifeJournalService;
import com.lingfeng.sprite.life.persistence.AutonomyPolicyEntity;
import com.lingfeng.sprite.life.persistence.AutonomyPolicyRepository;
import com.lingfeng.sprite.life.persistence.LifeJournalEntryEntity;
import com.lingfeng.sprite.life.persistence.LifeJournalEntryRepository;
import com.lingfeng.sprite.service.AutonomousConsciousness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifeSnapshotServiceTest {

    private LifeSnapshotService service;
    private IdentityService identityService;
    private SelfService selfService;
    private RelationshipService relationshipService;
    private GoalService goalService;

    @BeforeEach
    void setUp() {
        identityService = new IdentityService();
        identityService.updateProfile(
                IdentityProfile.builder()
                        .displayName("Sprite")
                        .essence("a learning digital being")
                        .emoji("AI")
                        .vibe("focused")
                        .build(),
                "test"
        );

        selfService = new SelfService();
        relationshipService = new RelationshipService();
        relationshipService.initialize("owner-123");
        goalService = new GoalService();

        LifeJournalEntryRepository journalRepository = mock(LifeJournalEntryRepository.class);
        when(journalRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(journalRepository.save(any(LifeJournalEntryEntity.class))).thenAnswer(invocation -> {
            LifeJournalEntryEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        AutonomyPolicyRepository policyRepository = mock(AutonomyPolicyRepository.class);
        AutonomyPolicyEntity policy = AutonomyPolicyEntity.defaults();
        policy.setId(1L);
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(AutonomyPolicyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LifeJournalService journalService = new LifeJournalService(
                journalRepository,
                new ObjectMapper().registerModule(new JavaTimeModule())
        );
        AutonomyPolicyService autonomyPolicyService = new AutonomyPolicyService(
                policyRepository,
                new AutonomousConsciousness(),
                journalService
        );

        service = new LifeSnapshotService(
                identityService,
                selfService,
                relationshipService,
                goalService,
                journalService,
                autonomyPolicyService
        );
    }

    @Test
    @DisplayName("generateSnapshot should produce a valid life snapshot")
    void generateSnapshot_producesSnapshot() {
        LifeSnapshot snapshot = service.generateSnapshot();

        assertNotNull(snapshot);
        assertEquals("Sprite", snapshot.getDisplayName());
        assertNotNull(snapshot.getCurrentState());
        assertNotNull(snapshot.getAttentionFocus());
        assertNotNull(snapshot.getPacingState());
    }

    @Test
    @DisplayName("generateSnapshot should include active intentions")
    void generateSnapshot_includesIntentions() {
        goalService.createIntention("intent-1", "Work on the current task", ActiveIntention.Urgency.HIGH);

        LifeSnapshot snapshot = service.generateSnapshot();

        assertEquals(1, snapshot.getActiveIntentions().size());
        assertEquals("intent-1", snapshot.getActiveIntentions().get(0).getIntentionId());
    }

    @Test
    @DisplayName("generateSnapshot should include relationship summary")
    void generateSnapshot_includesRelationshipSummary() {
        LifeSnapshot snapshot = service.generateSnapshot();

        assertNotNull(snapshot.getRelationshipSummary());
        assertEquals("FRIEND", snapshot.getRelationshipSummary().getRelationshipType());
        assertEquals("MEDIUM", snapshot.getRelationshipSummary().getTrustLevel());
    }

    @Test
    @DisplayName("generateSnapshot coherence should match the self state")
    void generateSnapshot_coherenceMatchesSelfState() {
        selfService.updateState(SelfState.builder()
                .energyLevel(0.6f)
                .coherenceScore(0.82f)
                .build());
        selfService.updateFocus(AttentionFocus.conversation("conv-1", "Planning"));

        LifeSnapshot snapshot = service.generateSnapshot();

        assertEquals(0.82f, snapshot.getCoherenceScore(), 0.001f);
        assertEquals("Planning", snapshot.getAttentionFocus().getDescription());
    }

    @Test
    @DisplayName("isReady should return true when the required services are wired")
    void isReady_returnsTrue() {
        assertTrue(service.isReady());
    }

    @Test
    @DisplayName("createDefault should produce a baseline snapshot")
    void createDefault_returnsValidSnapshot() {
        LifeSnapshot snapshot = LifeSnapshot.createDefault();

        assertNotNull(snapshot);
        assertFalse(snapshot.getVersion().isBlank());
        assertNotNull(snapshot.getGeneratedAt());
    }
}
