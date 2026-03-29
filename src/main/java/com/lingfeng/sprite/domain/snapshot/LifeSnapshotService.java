package com.lingfeng.sprite.domain.snapshot;

import com.lingfeng.sprite.domain.goal.GoalService;
import com.lingfeng.sprite.domain.identity.IdentityService;
import com.lingfeng.sprite.domain.relationship.RelationshipService;
import com.lingfeng.sprite.domain.self.AttentionFocus;
import com.lingfeng.sprite.domain.self.SelfService;
import com.lingfeng.sprite.domain.self.SelfState;
import com.lingfeng.sprite.life.AutonomyPolicyService;
import com.lingfeng.sprite.life.LifeJournalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public final class LifeSnapshotService {

    private static final Logger logger = LoggerFactory.getLogger(LifeSnapshotService.class);

    private final IdentityService identityService;
    private final SelfService selfService;
    private final RelationshipService relationshipService;
    private final GoalService goalService;
    private final LifeJournalService lifeJournalService;
    private final AutonomyPolicyService autonomyPolicyService;

    public LifeSnapshotService(
            IdentityService identityService,
            SelfService selfService,
            RelationshipService relationshipService,
            GoalService goalService,
            LifeJournalService lifeJournalService,
            AutonomyPolicyService autonomyPolicyService
    ) {
        this.identityService = identityService;
        this.selfService = selfService;
        this.relationshipService = relationshipService;
        this.goalService = goalService;
        this.lifeJournalService = lifeJournalService;
        this.autonomyPolicyService = autonomyPolicyService;
    }

    public LifeSnapshot generateSnapshot() {
        logger.debug("Generating LifeSnapshot...");

        List<LifeJournalService.LifeJournalEntryView> journalEntries = lifeJournalService.getRecentEntries(8);
        List<RecentChange> recentChanges = journalEntries.stream()
                .limit(5)
                .map(this::toRecentChange)
                .toList();
        List<String> recentMemorySummaries = journalEntries.stream()
                .limit(4)
                .map(entry -> entry.title() + ": " + entry.detail())
                .toList();

        SelfState currentState = selfService.getCurrentState() != null
                ? selfService.getCurrentState()
                : SelfState.createDefault();
        AttentionFocus focus = selfService.getCurrentFocus() != null
                ? selfService.getCurrentFocus()
                : AttentionFocus.idle();

        LifeSnapshot snapshot = LifeSnapshot.builder()
                .version("2.0")
                .generatedAt(Instant.now())
                .identitySummary(generateIdentitySummary())
                .emoji(resolveEmoji())
                .displayName(resolveDisplayName())
                .currentState(currentState)
                .attentionFocus(focus)
                .activeIntentions(goalService.getActiveIntentions())
                .relationshipSummary(relationshipService.isComplete()
                        ? buildRelationshipSummary()
                        : RelationshipSummary.createDefault())
                .recentChanges(recentChanges)
                .recentMemorySummaries(recentMemorySummaries)
                .nextLikelyActions(buildNextLikelyActions())
                .coherenceScore(currentState.getCoherenceScore())
                .pacingState(buildPacingState(recentChanges.size()))
                .build();

        logger.debug("LifeSnapshot generated: {}", snapshot);
        return snapshot;
    }

    public boolean isReady() {
        return selfService != null && relationshipService != null;
    }

    private String generateIdentitySummary() {
        return String.format(
                "%s is a digital being focused on %s. Narrative: %s",
                resolveDisplayName(),
                identityService.getProfile().getEssence().isBlank()
                        ? "staying aligned with the owner"
                        : identityService.getProfile().getEssence(),
                identityService.getNarrative().getCurrentNarrative()
        );
    }

    private String resolveDisplayName() {
        String displayName = identityService.getProfile().getDisplayName();
        return displayName == null || displayName.isBlank() ? "Sprite" : displayName;
    }

    private String resolveEmoji() {
        String emoji = identityService.getProfile().getEmoji();
        return emoji == null || emoji.isBlank() ? "AI" : emoji;
    }

    private RelationshipSummary buildRelationshipSummary() {
        return RelationshipSummary.builder()
                .relationshipType(relationshipService.getProfile().getType().name())
                .trustLevel(relationshipService.getTrustState().getLevel().name())
                .trustScore(relationshipService.getTrustState().getScore())
                .relationshipStrength(relationshipService.getProfile().getStrength())
                .interactionCount(relationshipService.getProfile().getInteractionCount())
                .sharedProjectsCount(relationshipService.getActiveProjects().size())
                .topCarePriority(relationshipService.getTopCarePriority() != null
                        ? relationshipService.getTopCarePriority().getCareType().name()
                        : "EMOTIONAL")
                .build();
    }

    private RecentChange toRecentChange(LifeJournalService.LifeJournalEntryView entry) {
        RecentChange.ChangeType type = switch (entry.entryType()) {
            case "AUTONOMY" -> RecentChange.ChangeType.BEHAVIOR;
            case "MODEL" -> RecentChange.ChangeType.CAPABILITY;
            case "RESET" -> RecentChange.ChangeType.IDENTITY;
            case "COMMAND" -> RecentChange.ChangeType.GOAL;
            default -> RecentChange.ChangeType.SELF_STATE;
        };
        return RecentChange.builder()
                .changeId("journal-" + entry.id())
                .type(type)
                .description(entry.title())
                .previousState("")
                .newState(entry.detail())
                .occurredAt(entry.createdAt())
                .trigger(entry.entryType())
                .significance("MEDIUM")
                .build();
    }

    private List<String> buildNextLikelyActions() {
        List<LifeJournalService.LifeJournalEntryView> journalEntries = lifeJournalService.getRecentEntries(3);
        if (!journalEntries.isEmpty()) {
            LifeJournalService.LifeJournalEntryView latest = journalEntries.get(0);
            return List.of(
                    "Continue from the latest journal entry: " + latest.title(),
                    "Keep the current focus coherent.",
                    "Prepare the next helpful action."
            );
        }

        List<String> intentionActions = goalService.getActiveIntentions().stream()
                .limit(3)
                .map(intention -> "Continue: " + intention.getDescription())
                .toList();
        if (!intentionActions.isEmpty()) {
            return intentionActions;
        }
        if (autonomyPolicyService.getStatus().paused()) {
            return List.of("Wait for the owner to resume autonomy.");
        }
        return List.of(
                "Review the latest interaction.",
                "Keep the current focus coherent.",
                "Prepare the next helpful action."
        );
    }

    private PacingState buildPacingState(int recentChanges) {
        return PacingState.fromEvolution(
                autonomyPolicyService.getStatus().paused()
                        ? PacingState.PacingLayer.SLOW
                        : PacingState.PacingLayer.MEDIUM,
                Math.max(0, goalService.getActiveIntentions().size() - 1),
                recentChanges
        );
    }
}
