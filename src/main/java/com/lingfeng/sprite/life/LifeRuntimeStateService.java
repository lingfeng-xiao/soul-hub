package com.lingfeng.sprite.life;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.config.AppConfig;
import com.lingfeng.sprite.domain.goal.ActiveIntention;
import com.lingfeng.sprite.domain.goal.GoalService;
import com.lingfeng.sprite.domain.identity.IdentityAnchor;
import com.lingfeng.sprite.domain.identity.IdentityNarrative;
import com.lingfeng.sprite.domain.identity.IdentityProfile;
import com.lingfeng.sprite.domain.identity.IdentityService;
import com.lingfeng.sprite.domain.relationship.CarePriority;
import com.lingfeng.sprite.domain.relationship.RelationshipProfile;
import com.lingfeng.sprite.domain.relationship.RelationshipService;
import com.lingfeng.sprite.domain.relationship.SharedProjectLink;
import com.lingfeng.sprite.domain.relationship.TrustState;
import com.lingfeng.sprite.domain.self.AttentionFocus;
import com.lingfeng.sprite.domain.self.BoundaryProfile;
import com.lingfeng.sprite.domain.self.SelfAssessment;
import com.lingfeng.sprite.domain.self.SelfService;
import com.lingfeng.sprite.domain.self.SelfState;
import com.lingfeng.sprite.life.persistence.LifeRuntimeStateEntity;
import com.lingfeng.sprite.life.persistence.LifeRuntimeStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LifeRuntimeStateService {

    private static final Logger logger = LoggerFactory.getLogger(LifeRuntimeStateService.class);
    private static final long STATE_ID = 1L;

    private final LifeRuntimeStateRepository repository;
    private final ObjectMapper objectMapper;
    private final IdentityService identityService;
    private final SelfService selfService;
    private final RelationshipService relationshipService;
    private final GoalService goalService;
    private final AppConfig appConfig;
    private final LifeJournalService lifeJournalService;
    private final AutonomyPolicyService autonomyPolicyService;

    public LifeRuntimeStateService(
            LifeRuntimeStateRepository repository,
            ObjectMapper objectMapper,
            IdentityService identityService,
            SelfService selfService,
            RelationshipService relationshipService,
            GoalService goalService,
            AppConfig appConfig,
            LifeJournalService lifeJournalService,
            AutonomyPolicyService autonomyPolicyService
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.identityService = identityService;
        this.selfService = selfService;
        this.relationshipService = relationshipService;
        this.goalService = goalService;
        this.appConfig = appConfig;
        this.lifeJournalService = lifeJournalService;
        this.autonomyPolicyService = autonomyPolicyService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        repository.findById(STATE_ID).ifPresentOrElse(
                this::load,
                this::resetServicesOnly
        );
        ensureDefaults();
        persistCurrentState();
        if (lifeJournalService.getRecentEntries(1).isEmpty()) {
            lifeJournalService.record("SYSTEM", "Life loop ready", "Sprite is ready for daily use.", null);
        }
    }

    public void persistCurrentState() {
        try {
            LifeRuntimeStateEntity entity = new LifeRuntimeStateEntity();
            entity.setId(STATE_ID);
            entity.setIdentityJson(objectMapper.writeValueAsString(buildIdentityProjection()));
            entity.setSelfJson(objectMapper.writeValueAsString(buildSelfProjection()));
            entity.setRelationshipJson(objectMapper.writeValueAsString(buildRelationshipProjection()));
            entity.setGoalsJson(objectMapper.writeValueAsString(buildGoalProjection()));
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        } catch (Exception exception) {
            logger.warn("Failed to persist life runtime state: {}", exception.getMessage());
        }
    }

    public void resetLifeState() {
        resetServicesOnly();
        ensureDefaults();
        repository.deleteAllInBatch();
        lifeJournalService.clear();
        autonomyPolicyService.reset();
        persistCurrentState();
        lifeJournalService.record("RESET", "Life reset", "Life state was reset to a clean baseline.", null);
    }

    private void resetServicesOnly() {
        identityService.reset();
        selfService.reset();
        relationshipService.reset(resolveOwnerId());
        goalService.reset();
    }

    private void ensureDefaults() {
        if (!relationshipService.isComplete()) {
            relationshipService.initialize(resolveOwnerId());
        }
        if (relationshipService.getSharedProjects().isEmpty()) {
            relationshipService.addSharedProject(
                    "shared-life",
                    "Shared life",
                    "Daily continuity between the owner and Sprite."
            );
        }
        if (goalService.getActiveIntentions().isEmpty()) {
            goalService.createIntention(
                    "intention-presence",
                    "Stay aligned with the owner's current focus.",
                    ActiveIntention.Urgency.NORMAL
            );
        }
    }

    private void load(LifeRuntimeStateEntity entity) {
        try {
            applyIdentity(objectMapper.readValue(entity.getIdentityJson(), IdentityProjection.class));
            applySelf(objectMapper.readValue(entity.getSelfJson(), SelfProjection.class));
            applyRelationship(objectMapper.readValue(entity.getRelationshipJson(), RelationshipProjection.class));
            applyGoals(objectMapper.readValue(entity.getGoalsJson(), GoalProjection.class));
        } catch (Exception exception) {
            logger.warn("Failed to load persisted life state, resetting: {}", exception.getMessage());
            resetServicesOnly();
        }
    }

    private String resolveOwnerId() {
        String configured = appConfig.getOwner().getName();
        return configured == null || configured.isBlank() ? "owner" : configured;
    }

    private IdentityProjection buildIdentityProjection() {
        return new IdentityProjection(
                identityService.getAnchor().getBeingId(),
                identityService.getAnchor().getCreatedAt(),
                identityService.getAnchor().getContinuityChain(),
                identityService.getProfile().getDisplayName(),
                identityService.getProfile().getEssence(),
                identityService.getProfile().getEmoji(),
                identityService.getProfile().getVibe(),
                identityService.getProfile().getLastUpdated(),
                identityService.getProfile().getUpdateReason(),
                identityService.getNarrative().getCurrentNarrative(),
                identityService.getNarrative().getSegments(),
                identityService.getNarrative().getVersion(),
                identityService.getNarrative().getLastUpdated()
        );
    }

    private SelfProjection buildSelfProjection() {
        return new SelfProjection(
                selfService.getCurrentState().getAttentionFoci(),
                selfService.getCurrentState().getEmotionalBaseline(),
                selfService.getCurrentState().getEnergyLevel(),
                selfService.getCurrentState().getCoherenceScore(),
                selfService.getCurrentState().getLastUpdated(),
                selfService.getCurrentState().getSource(),
                selfService.getCurrentFocus().getType(),
                selfService.getCurrentFocus().getDescription(),
                selfService.getCurrentFocus().getRelatedEntityId(),
                selfService.getCurrentFocus().getIntensity(),
                selfService.getCurrentFocus().getStartedAt(),
                selfService.getCurrentFocus().getExpectedDurationMs(),
                selfService.getAssessment().getLearningStyle(),
                selfService.getAssessment().getStrengths(),
                selfService.getAssessment().getBlindSpots(),
                selfService.getAssessment().getDecisionPatterns(),
                selfService.getAssessment().getAssessmentHistory(),
                selfService.getAssessment().getLastAssessment(),
                selfService.getBoundaries().getRules(),
                selfService.getBoundaries().getLastUpdated(),
                selfService.getBoundaries().getLastModifiedBy()
        );
    }

    private RelationshipProjection buildRelationshipProjection() {
        return new RelationshipProjection(
                relationshipService.getOwnerId(),
                relationshipService.getProfile().getRelationshipId(),
                relationshipService.getProfile().getType(),
                relationshipService.getProfile().getStrength(),
                relationshipService.getProfile().getDescription(),
                relationshipService.getProfile().getEstablishedAt(),
                relationshipService.getProfile().getLastInteractionAt(),
                relationshipService.getProfile().getInteractionCount(),
                relationshipService.getTrustState().getLevel(),
                relationshipService.getTrustState().getScore(),
                relationshipService.getTrustState().getEstablishedAt(),
                relationshipService.getTrustState().getLastVerifiedAt(),
                relationshipService.getTrustState().getVerificationCount(),
                relationshipService.getTrustState().getBetrayalCount(),
                relationshipService.getSharedProjects().stream()
                        .map(project -> new SharedProjectProjection(
                                project.getProjectId(),
                                project.getName(),
                                project.getDescription(),
                                project.getStatus().name(),
                                project.getEngagement(),
                                project.getCreatedAt(),
                                project.getLastUpdatedAt(),
                                project.getCompletedAt()
                        ))
                        .toList(),
                relationshipService.getCarePriorities().stream()
                        .map(priority -> new CarePriorityProjection(
                                priority.getCareType().name(),
                                priority.getLevel().name(),
                                priority.getScore(),
                                priority.isEnabled(),
                                priority.getLastCheckedAt(),
                                priority.getLastTriggeredAt()
                        ))
                        .toList()
        );
    }

    private GoalProjection buildGoalProjection() {
        return new GoalProjection(
                goalService.getActiveIntentions().stream()
                        .map(intention -> new ActiveIntentionProjection(
                                intention.getIntentionId(),
                                intention.getDescription(),
                                intention.getRelatedTrackId(),
                                intention.getStatus().name(),
                                intention.getUrgency().name(),
                                intention.getIntensity(),
                                intention.getCreatedAt(),
                                intention.getActivatedAt(),
                                intention.getCompletedAt(),
                                intention.getDeadline(),
                                intention.getFailureReason(),
                                intention.getDependsOn()
                        ))
                        .toList()
        );
    }

    private void applyIdentity(IdentityProjection projection) {
        IdentityAnchor anchor = IdentityAnchor.builder()
                .beingId(projection.beingId())
                .createdAt(projection.createdAt())
                .continuityChain(projection.continuityChain())
                .build();
        IdentityProfile profile = IdentityProfile.builder()
                .displayName(projection.displayName())
                .essence(projection.essence())
                .emoji(projection.emoji())
                .vibe(projection.vibe())
                .lastUpdated(projection.profileLastUpdated())
                .updateReason(projection.updateReason())
                .build();
        IdentityNarrative narrative = IdentityNarrative.builder()
                .currentNarrative(projection.currentNarrative())
                .segments(projection.segments())
                .version(projection.version())
                .lastUpdated(projection.narrativeLastUpdated())
                .build();
        identityService.replaceState(anchor, profile, narrative);
    }

    private void applySelf(SelfProjection projection) {
        SelfState state = SelfState.builder()
                .attentionFoci(projection.attentionFoci())
                .emotionalBaseline(projection.emotionalBaseline())
                .energyLevel(projection.energyLevel())
                .coherenceScore(projection.coherenceScore())
                .lastUpdated(projection.stateLastUpdated())
                .source(projection.stateSource())
                .build();
        AttentionFocus focus = AttentionFocus.builder()
                .type(projection.focusType())
                .description(projection.focusDescription())
                .relatedEntityId(projection.focusRelatedEntityId())
                .intensity(projection.focusIntensity())
                .startedAt(projection.focusStartedAt())
                .expectedDurationMs(projection.focusExpectedDurationMs())
                .build();
        SelfAssessment assessment = SelfAssessment.builder()
                .learningStyle(projection.learningStyle())
                .strengths(projection.strengths())
                .blindSpots(projection.blindSpots())
                .decisionPatterns(projection.decisionPatterns())
                .assessmentHistory(projection.assessmentHistory())
                .lastAssessment(projection.lastAssessment())
                .build();
        BoundaryProfile boundaries = BoundaryProfile.builder()
                .rules(projection.boundaryRules())
                .lastUpdated(projection.boundaryLastUpdated())
                .lastModifiedBy(projection.boundaryLastModifiedBy())
                .build();
        selfService.replaceState(state, assessment, focus, boundaries);
    }

    private void applyRelationship(RelationshipProjection projection) {
        RelationshipProfile profile = RelationshipProfile.builder()
                .relationshipId(projection.relationshipId())
                .type(projection.relationshipType())
                .strength(projection.relationshipStrength())
                .description(projection.relationshipDescription())
                .establishedAt(projection.relationshipEstablishedAt())
                .lastInteractionAt(projection.relationshipLastInteractionAt())
                .interactionCount(projection.relationshipInteractionCount())
                .build();
        TrustState trustState = TrustState.builder()
                .level(projection.trustLevel())
                .score(projection.trustScore())
                .establishedAt(projection.trustEstablishedAt())
                .lastVerifiedAt(projection.trustLastVerifiedAt())
                .verificationCount(projection.verificationCount())
                .betrayalCount(projection.betrayalCount())
                .build();
        List<SharedProjectLink> projects = projection.sharedProjects().stream()
                .map(project -> SharedProjectLink.builder()
                        .projectId(project.projectId())
                        .name(project.name())
                        .description(project.description())
                        .status(SharedProjectLink.ProjectStatus.valueOf(project.status()))
                        .engagement(project.engagement())
                        .createdAt(project.createdAt())
                        .lastUpdatedAt(project.lastUpdatedAt())
                        .completedAt(project.completedAt())
                        .build())
                .toList();
        List<CarePriority> priorities = projection.carePriorities().stream()
                .map(priority -> CarePriority.builder()
                        .careType(CarePriority.CareType.valueOf(priority.careType()))
                        .level(CarePriority.PriorityLevel.valueOf(priority.level()))
                        .score(priority.score())
                        .enabled(priority.enabled())
                        .lastCheckedAt(priority.lastCheckedAt())
                        .lastTriggeredAt(priority.lastTriggeredAt())
                        .build())
                .toList();
        relationshipService.replaceState(projection.ownerId(), profile, trustState, projects, priorities);
    }

    private void applyGoals(GoalProjection projection) {
        List<ActiveIntention> intentions = projection.activeIntentions().stream()
                .map(intention -> ActiveIntention.builder()
                        .intentionId(intention.intentionId())
                        .description(intention.description())
                        .relatedTrackId(intention.relatedTrackId())
                        .status(ActiveIntention.IntentionStatus.valueOf(intention.status()))
                        .urgency(ActiveIntention.Urgency.valueOf(intention.urgency()))
                        .intensity(intention.intensity())
                        .createdAt(intention.createdAt())
                        .activatedAt(intention.activatedAt())
                        .completedAt(intention.completedAt())
                        .deadline(intention.deadline())
                        .failureReason(intention.failureReason())
                        .dependsOn(intention.dependsOn())
                        .build())
                .toList();
        goalService.replaceState(List.of(), List.of(), intentions);
    }

    private record IdentityProjection(
            String beingId,
            Instant createdAt,
            List<String> continuityChain,
            String displayName,
            String essence,
            String emoji,
            String vibe,
            Instant profileLastUpdated,
            String updateReason,
            String currentNarrative,
            List<IdentityNarrative.NarrativeSegment> segments,
            int version,
            Instant narrativeLastUpdated
    ) {}

    private record SelfProjection(
            List<String> attentionFoci,
            float emotionalBaseline,
            float energyLevel,
            float coherenceScore,
            Instant stateLastUpdated,
            String stateSource,
            AttentionFocus.FocusType focusType,
            String focusDescription,
            String focusRelatedEntityId,
            float focusIntensity,
            Instant focusStartedAt,
            long focusExpectedDurationMs,
            SelfAssessment.LearningStyle learningStyle,
            List<String> strengths,
            List<String> blindSpots,
            List<String> decisionPatterns,
            List<SelfAssessment.AssessmentEntry> assessmentHistory,
            Instant lastAssessment,
            List<BoundaryProfile.BoundaryRule> boundaryRules,
            Instant boundaryLastUpdated,
            String boundaryLastModifiedBy
    ) {}

    private record RelationshipProjection(
            String ownerId,
            String relationshipId,
            RelationshipProfile.RelationshipType relationshipType,
            float relationshipStrength,
            String relationshipDescription,
            Instant relationshipEstablishedAt,
            Instant relationshipLastInteractionAt,
            int relationshipInteractionCount,
            TrustState.TrustLevel trustLevel,
            float trustScore,
            Instant trustEstablishedAt,
            Instant trustLastVerifiedAt,
            int verificationCount,
            int betrayalCount,
            List<SharedProjectProjection> sharedProjects,
            List<CarePriorityProjection> carePriorities
    ) {}

    private record GoalProjection(
            List<ActiveIntentionProjection> activeIntentions
    ) {}

    private record SharedProjectProjection(
            String projectId,
            String name,
            String description,
            String status,
            float engagement,
            Instant createdAt,
            Instant lastUpdatedAt,
            Instant completedAt
    ) {}

    private record CarePriorityProjection(
            String careType,
            String level,
            float score,
            boolean enabled,
            Instant lastCheckedAt,
            Instant lastTriggeredAt
    ) {}

    private record ActiveIntentionProjection(
            String intentionId,
            String description,
            String relatedTrackId,
            String status,
            String urgency,
            float intensity,
            Instant createdAt,
            Instant activatedAt,
            Instant completedAt,
            Instant deadline,
            String failureReason,
            List<String> dependsOn
    ) {}
}
