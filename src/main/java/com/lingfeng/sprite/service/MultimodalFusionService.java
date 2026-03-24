package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * S23-3: Multimodal Input Fusion Service
 *
 * Fuses multiple input modalities (text, voice, image) into unified context.
 * Handles temporal alignment, priority-based fusion for conflicts, and
 * confidence scoring for fused inputs.
 *
 * Integration Points:
 * - VoiceInteractionService (S23-1)
 * - ImageUnderstandingService (S23-2)
 * - UnifiedContextService (existing)
 */
@Service
public class MultimodalFusionService {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalFusionService.class);

    // Modality priority weights (higher = more prioritized)
    private static final Map<ModalityType, Integer> MODALITY_PRIORITY = Map.of(
        ModalityType.VOICE, 3,    // Voice has highest priority (most expressive)
        ModalityType.IMAGE, 2,     // Image provides context
        ModalityType.TEXT, 1       // Text is explicit but may need interpretation
    );

    // Confidence thresholds
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.5;

    // Default temporal alignment window
    private static final Duration DEFAULT_ALIGNMENT_WINDOW = Duration.ofSeconds(2);

    /**
     * S23-3: 融合多模态输入
     *
     * Takes a MultimodalInput containing potentially multiple modalities
     * and fuses them into a unified FusedInput context.
     *
     * @param input The multimodal input containing text, voice, and/or image data
     * @return FusedInput with primary content, supporting content, and quality metrics
     */
    public FusedInput fuse(MultimodalInput input) {
        if (input == null) {
            logger.warn("Received null MultimodalInput, returning empty fused input");
            return createEmptyFusedInput();
        }

        logger.debug("Fusing multimodal input: types={}, timestamp={}",
            input.modalities().size(), input.timestamp());

        List<ModalityContent> modalities = new ArrayList<>(input.modalities());

        if (modalities.isEmpty()) {
            return createEmptyFusedInput();
        }

        if (modalities.size() == 1) {
            return fuseSingleModality(modalities.get(0));
        }

        return fuseMultipleModalities(modalities);
    }

    /**
     * S23-3: 对齐多模态输入时间戳
     *
     * Aligns multiple multimodal inputs temporally within a defined window.
     * Inputs within the window are considered synchronized.
     *
     * @param inputs List of multimodal inputs to align
     * @param window Time window for temporal alignment
     * @return AlignedInputs containing synchronized groups and sync accuracy
     */
    public AlignedInputs alignTemporal(List<MultimodalInput> inputs, Duration window) {
        if (inputs == null || inputs.isEmpty()) {
            logger.debug("No inputs to align temporally");
            return new AlignedInputs(List.of(), window, 0.0);
        }

        Duration effectiveWindow = window != null ? window : DEFAULT_ALIGNMENT_WINDOW;

        logger.debug("Aligning {} inputs within window {}", inputs.size(), effectiveWindow);

        // Group inputs by temporal proximity
        List<List<MultimodalInput>> alignedGroups = new ArrayList<>();
        List<MultimodalInput> remaining = new ArrayList<>(inputs);

        while (!remaining.isEmpty()) {
            MultimodalInput anchor = remaining.remove(0);
            List<MultimodalInput> group = new ArrayList<>();
            group.add(anchor);

            Instant anchorTime = anchor.timestamp();
            List<MultimodalInput> toRemove = new ArrayList<>();

            for (MultimodalInput input : remaining) {
                Duration diff = Duration.between(
                    anchorTime.compareTo(input.timestamp()) > 0 ? input.timestamp() : anchorTime,
                    anchorTime.compareTo(input.timestamp()) > 0 ? anchorTime : input.timestamp()
                );

                if (diff.compareTo(effectiveWindow) <= 0) {
                    group.add(input);
                    toRemove.add(input);
                }
            }

            remaining.removeAll(toRemove);
            alignedGroups.add(group);
        }

        // Calculate sync accuracy based on group sizes and temporal spread
        double syncAccuracy = calculateSyncAccuracy(alignedGroups, effectiveWindow);

        logger.debug("Aligned into {} groups with accuracy {}", alignedGroups.size(), syncAccuracy);

        return new AlignedInputs(alignedGroups, effectiveWindow, syncAccuracy);
    }

    /**
     * S23-3: 优先级融合
     *
     * When inputs conflict, uses priority-based fusion to determine
     * the primary content based on modality priority and confidence.
     *
     * @param inputs List of multimodal inputs to fuse with priority
     * @return FusedInput with highest priority content as primary
     */
    public FusedInput priorityFusion(List<MultimodalInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            logger.debug("No inputs for priority fusion, returning empty fused input");
            return createEmptyFusedInput();
        }

        logger.debug("Performing priority fusion on {} inputs", inputs.size());

        // Collect all modalities from all inputs
        List<ModalityContent> allModalities = new ArrayList<>();
        for (MultimodalInput input : inputs) {
            allModalities.addAll(input.modalities());
        }

        if (allModalities.isEmpty()) {
            return createEmptyFusedInput();
        }

        // Sort by priority then by confidence
        List<ModalityContent> sorted = allModalities.stream()
            .sorted(Comparator
                .comparing((ModalityContent m) -> MODALITY_PRIORITY.getOrDefault(m.type(), 0))
                .thenComparing(ModalityContent::confidence)
                .reversed())
            .collect(Collectors.toList());

        // Primary content is highest priority, highest confidence
        ModalityContent primary = sorted.get(0);

        // Supporting content is everything else
        List<ModalityContent> supporting = sorted.size() > 1
            ? sorted.subList(1, sorted.size())
            : List.of();

        // Detect conflicts (same type content with different values)
        Map<ModalityType, List<ModalityContent>> byType = allModalities.stream()
            .collect(Collectors.groupingBy(ModalityContent::type));

        boolean hasConflict = byType.values().stream()
            .anyMatch(list -> list.size() > 1 &&
                list.stream().map(ModalityContent::content).distinct().count() > 1);

        // Calculate fused confidence
        double fusedConfidence = calculateFusedConfidence(sorted);

        // Build fusion quality
        FusionQuality quality = buildFusionQuality(sorted, hasConflict);

        return new FusedInput(
            primary.content(),
            extractSupportingContent(supporting),
            FusionMethod.PRIORITY,
            quality,
            fusedConfidence
        );
    }

    /**
     * 评估融合质量
     *
     * Evaluates the quality of a fused input based on completeness,
     * coherence, and confidence metrics.
     *
     * @param input The fused input to evaluate
     * @return FusionQuality with score, completeness, and coherence
     */
    public FusionQuality evaluateQuality(FusedInput input) {
        if (input == null) {
            return new FusionQuality(0.0, 0.0, 0.0);
        }

        return input.quality();
    }

    // ==================== Private Helper Methods ====================

    private FusedInput fuseSingleModality(ModalityContent modality) {
        FusionQuality quality = new FusionQuality(
            modality.confidence(),
            1.0,  // Single modality is complete by itself
            1.0   // Single modality is inherently coherent
        );

        return new FusedInput(
            modality.content(),
            "",
            FusionMethod.SINGLE,
            quality,
            modality.confidence()
        );
    }

    private FusedInput fuseMultipleModalities(List<ModalityContent> modalities) {
        // Sort by priority then by confidence
        List<ModalityContent> sorted = modalities.stream()
            .sorted(Comparator
                .comparing((ModalityContent m) -> MODALITY_PRIORITY.getOrDefault(m.type(), 0))
                .thenComparing(ModalityContent::confidence)
                .reversed())
            .collect(Collectors.toList());

        ModalityContent primary = sorted.get(0);
        List<ModalityContent> supporting = sorted.subList(1, sorted.size());

        // Check for conflicts
        Map<ModalityType, List<ModalityContent>> byType = modalities.stream()
            .collect(Collectors.groupingBy(ModalityContent::type));

        boolean hasConflict = byType.values().stream()
            .anyMatch(list -> list.size() > 1 &&
                list.stream().map(ModalityContent::content).distinct().count() > 1);

        // Calculate fused confidence
        double fusedConfidence = calculateFusedConfidence(sorted);

        // Build fusion quality
        FusionQuality quality = buildFusionQuality(sorted, hasConflict);

        return new FusedInput(
            primary.content(),
            extractSupportingContent(supporting),
            FusionMethod.COMPLEMENTARY,
            quality,
            fusedConfidence
        );
    }

    private double calculateFusedConfidence(List<ModalityContent> modalities) {
        if (modalities.isEmpty()) {
            return 0.0;
        }

        // Weighted average based on modality priority
        double totalWeight = 0.0;
        double weightedSum = 0.0;

        for (ModalityContent m : modalities) {
            int priority = MODALITY_PRIORITY.getOrDefault(m.type(), 1);
            double weight = priority * m.confidence();
            weightedSum += weight;
            totalWeight += priority;
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }

    private FusionQuality buildFusionQuality(List<ModalityContent> modalities, boolean hasConflict) {
        // Completeness: how many modality types are present
        long uniqueTypes = modalities.stream()
            .map(ModalityContent::type)
            .distinct()
            .count();
        double completeness = Math.min(1.0, uniqueTypes / 3.0); // 3 types max

        // Coherence: agreement between modalities
        double coherence = hasConflict ? 0.5 : 1.0;

        // Adjust confidence based on coherence
        double avgConfidence = modalities.stream()
            .mapToDouble(ModalityContent::confidence)
            .average()
            .orElse(0.0);

        double score = (completeness + coherence + avgConfidence) / 3.0;

        return new FusionQuality(score, completeness, coherence);
    }

    private double calculateSyncAccuracy(List<List<MultimodalInput>> groups, Duration window) {
        if (groups.isEmpty()) {
            return 0.0;
        }

        // Larger groups indicate better sync
        double avgGroupSize = groups.stream()
            .mapToInt(List::size)
            .average()
            .orElse(0.0);

        // More groups with fewer items may indicate poor sync
        double groupCountFactor = Math.max(0, 1.0 - (groups.size() - 1) * 0.1);

        // Base accuracy on group characteristics
        double accuracy = Math.min(1.0, avgGroupSize / 3.0) * groupCountFactor;

        return Math.round(accuracy * 100.0) / 100.0;
    }

    private String extractSupportingContent(List<ModalityContent> supporting) {
        if (supporting.isEmpty()) {
            return "";
        }

        return supporting.stream()
            .map(m -> String.format("[%s: %s (conf: %.2f)]",
                m.type().name(), m.content(), m.confidence()))
            .collect(Collectors.joining(", "));
    }

    private FusedInput createEmptyFusedInput() {
        return new FusedInput(
            "",
            "",
            FusionMethod.NONE,
            new FusionQuality(0.0, 0.0, 0.0),
            0.0
        );
    }

    // ==================== Records ====================

    /**
     * Multimodal Input containing one or more modalities
     */
    public record MultimodalInput(
        List<ModalityContent> modalities,
        Instant timestamp
    ) {
        public MultimodalInput {
            if (modalities == null) {
                modalities = List.of();
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }
    }

    /**
     * Content from a single modality
     */
    public record ModalityContent(
        ModalityType type,
        String content,
        Instant timestamp,
        double confidence
    ) {
        public ModalityContent {
            if (content == null) {
                content = "";
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            confidence = Math.max(0.0, Math.min(1.0, confidence));
        }
    }

    /**
     * Modality types for multimodal input
     */
    public enum ModalityType {
        TEXT,
        VOICE,
        IMAGE
    }

    /**
     * Fused input result containing primary and supporting content
     */
    public record FusedInput(
        String primaryContent,
        String supportingContent,
        FusionMethod fusionMethod,
        FusionQuality quality,
        double confidence
    ) {}

    /**
     * Result of temporal alignment of inputs
     */
    public record AlignedInputs(
        List<List<MultimodalInput>> alignedInputs,
        Duration timeWindow,
        double syncAccuracy
    ) {}

    /**
     * Quality metrics for fused input
     */
    public record FusionQuality(
        double score,
        double completeness,
        double coherence
    ) {}

    /**
     * Methods used for fusion
     */
    public enum FusionMethod {
        NONE,
        SINGLE,
        COMPLEMENTARY,
        PRIORITY
    }
}
