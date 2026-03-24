package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.sensor.AudioSensor;

/**
 * S23-4: Voice Emotion Analysis Service
 *
 * Analyzes emotion from voice/audio input by extracting acoustic features
 * and mapping them to emotion categories with confidence scores.
 *
 * Features:
 * - Pitch analysis (average, variance)
 * - Speech rate detection
 * - Volume level assessment
 * - Tone quality evaluation
 * - Real-time emotion tracking
 * - Text-based emotion inference
 *
 * Integration:
 * - AudioSensor for audio data source
 * - EmotionHistoryService for historical tracking
 */
@Service
public class VoiceEmotionAnalysis {

    private static final Logger logger = LoggerFactory.getLogger(VoiceEmotionAnalysis.class);

    // Static instance for non-Spring access
    private static volatile VoiceEmotionAnalysis instance = null;

    // Audio sensor for audio data
    private final AudioSensor audioSensor;

    // Emotion history service for tracking
    private final EmotionHistoryService emotionHistoryService;

    // Recent analysis results cache
    private final List<VoiceEmotionResult> recentResults = new ArrayList<>();
    private static final int MAX_RECENT_RESULTS = 100;

    // Analysis threshold
    private static final int MIN_AUDIO_SAMPLES = 1600; // ~100ms at 16kHz

    public VoiceEmotionAnalysis(
            @Autowired(required = false) AudioSensor audioSensor,
            @Autowired(required = false) EmotionHistoryService emotionHistoryService) {
        this.audioSensor = audioSensor != null ? audioSensor : new AudioSensor();
        this.emotionHistoryService = emotionHistoryService;
        instance = this;
        logger.info("VoiceEmotionAnalysis initialized - AudioSensor: {}, EmotionHistoryService: {}",
                audioSensor != null ? "available" : "unavailable",
                emotionHistoryService != null ? "available" : "unavailable");
    }

    /**
     * S23-4: Get static instance (for non-Spring components)
     */
    public static VoiceEmotionAnalysis getInstance() {
        return instance;
    }

    /**
     * S23-4: Voice Emotion enum
     */
    public enum Emotion {
        HAPPY,
        SAD,
        ANGRY,
        CALM,
        EXCITED,
        FRUSTRATED,
        FEARFUL,
        NEUTRAL
    }

    /**
     * S23-4: Emotion trend direction
     */
    public enum TrendDirection {
        RISING,
        FALLING,
        STABLE
    }

    /**
     * S23-4: Acoustic features extracted from voice
     *
     * @param pitchAverage Average pitch in Hz (normal range: 80-250 Hz for male, 150-400 Hz for female)
     * @param pitchVariance Variance of pitch (higher variance may indicate emotional arousal)
     * @param speechRate Words per minute (normal: 120-150 WPM)
     * @param volumeLevel Volume level 0.0-1.0
     * @param toneQuality Tone quality score 0.0-1.0 (0=hoarse, 1=clear)
     */
    public record AcousticFeatures(
            float pitchAverage,
            float pitchVariance,
            float speechRate,
            float volumeLevel,
            float toneQuality
    ) {
        public AcousticFeatures {
            // Clamp values to valid ranges
            pitchAverage = clamp(pitchAverage, 0f, 500f);
            pitchVariance = clamp(pitchVariance, 0f, 200f);
            speechRate = clamp(speechRate, 0f, 300f);
            volumeLevel = clamp(volumeLevel, 0f, 1f);
            toneQuality = clamp(toneQuality, 0f, 1f);
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    /**
     * S23-4: Voice emotion analysis result
     *
     * @param emotion Detected emotion category
     * @param confidence Confidence score 0.0-1.0
     * @param acousticFeatures Extracted acoustic features
     * @param timestamp Analysis timestamp
     */
    public record VoiceEmotionResult(
            Emotion emotion,
            float confidence,
            AcousticFeatures acousticFeatures,
            Instant timestamp
    ) {
        public VoiceEmotionResult {
            confidence = clamp(confidence, 0f, 1f);
            if (timestamp == null) timestamp = Instant.now();
            if (acousticFeatures == null) acousticFeatures = new AcousticFeatures(0f, 0f, 0f, 0f, 0f);
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    /**
     * S23-4: Emotion trend analysis result
     *
     * @param direction Trend direction (RISING/FALLING/STABLE)
     * @param averageEmotion Most common emotion in the period
     * @param volatility Emotion volatility score 0.0-1.0
     * @param trendStrength Trend strength 0.0-1.0
     */
    public record EmotionTrend(
            TrendDirection direction,
            Emotion averageEmotion,
            float volatility,
            float trendStrength
    ) {
        public EmotionTrend {
            volatility = clamp(volatility, 0f, 1f);
            trendStrength = clamp(trendStrength, 0f, 1f);
            if (averageEmotion == null) averageEmotion = Emotion.NEUTRAL;
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    /**
     * S23-4: Analyze emotion from audio data
     *
     * @param audioData Raw audio data bytes (typically 16-bit PCM, 16kHz mono)
     * @return VoiceEmotionResult with detected emotion and confidence
     */
    public VoiceEmotionResult analyzeEmotion(byte[] audioData) {
        if (audioData == null || audioData.length < MIN_AUDIO_SAMPLES) {
            logger.warn("Insufficient audio data for analysis: {} bytes", audioData != null ? audioData.length : 0);
            return createUnknownResult();
        }

        try {
            // Extract acoustic features from audio
            AcousticFeatures features = extractAcousticFeatures(audioData);

            // Map features to emotion
            EmotionPair result = mapFeaturesToEmotion(features);

            VoiceEmotionResult voiceResult = new VoiceEmotionResult(
                    result.emotion,
                    result.confidence,
                    features,
                    Instant.now()
            );

            // Cache result
            cacheResult(voiceResult);

            // Optionally integrate with EmotionHistoryService
            if (emotionHistoryService != null) {
                integrateWithEmotionHistory(voiceResult);
            }

            logger.debug("Voice emotion analyzed: {} (confidence: %.2f)", result.emotion, result.confidence);
            return voiceResult;

        } catch (Exception e) {
            logger.error("Error analyzing voice emotion", e);
            return createUnknownResult();
        }
    }

    /**
     * S23-4: Infer emotion from text (speech-to-text analysis)
     *
     * @param text Input text (typically from speech recognition)
     * @return VoiceEmotionResult with inferred emotion and confidence
     */
    public VoiceEmotionResult inferEmotionFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return createUnknownResult();
        }

        try {
            // Extract acoustic-like features from text characteristics
            AcousticFeatures features = inferFeaturesFromText(text);

            // Map features to emotion
            EmotionPair result = mapFeaturesToEmotion(features);

            VoiceEmotionResult voiceResult = new VoiceEmotionResult(
                    result.emotion,
                    result.confidence,
                    features,
                    Instant.now()
            );

            cacheResult(voiceResult);

            if (emotionHistoryService != null) {
                integrateWithEmotionHistory(voiceResult);
            }

            logger.debug("Emotion inferred from text: {} (confidence: %.2f)", result.emotion, result.confidence);
            return voiceResult;

        } catch (Exception e) {
            logger.error("Error inferring emotion from text", e);
            return createUnknownResult();
        }
    }

    /**
     * S23-4: Track emotion trend over history
     *
     * @param history List of voice emotion results to analyze
     * @return EmotionTrend with direction and statistics
     */
    public EmotionTrend trackEmotion(List<VoiceEmotionResult> history) {
        if (history == null || history.size() < 2) {
            return new EmotionTrend(TrendDirection.STABLE, Emotion.NEUTRAL, 0f, 0f);
        }

        try {
            // Count emotions
            Map<Emotion, Integer> emotionCounts = new HashMap<>();
            for (VoiceEmotionResult result : history) {
                emotionCounts.merge(result.emotion(), 1, Integer::sum);
            }

            // Find most common emotion
            Emotion mostCommon = Emotion.NEUTRAL;
            int maxCount = 0;
            for (Map.Entry<Emotion, Integer> entry : emotionCounts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    mostCommon = entry.getKey();
                }
            }

            // Calculate average confidence
            float avgConfidence = (float) history.stream()
                    .mapToDouble(VoiceEmotionResult::confidence)
                    .average()
                    .orElse(0.5f);

            // Calculate volatility (variance in confidence)
            float volatility = calculateVolatility(history);

            // Determine trend direction
            TrendDirection direction = determineTrendDirection(history);

            // Calculate trend strength
            float trendStrength = calculateTrendStrength(history);

            return new EmotionTrend(direction, mostCommon, volatility, trendStrength);

        } catch (Exception e) {
            logger.error("Error tracking emotion trend", e);
            return new EmotionTrend(TrendDirection.STABLE, Emotion.NEUTRAL, 0f, 0f);
        }
    }

    /**
     * Check if the service is available
     */
    public boolean isAvailable() {
        return audioSensor != null && audioSensor.isAvailable();
    }

    /**
     * Get recent analysis results
     */
    public List<VoiceEmotionResult> getRecentResults() {
        return new ArrayList<>(recentResults);
    }

    /**
     * Get recent results limited by count
     */
    public List<VoiceEmotionResult> getRecentResults(int limit) {
        int size = recentResults.size();
        int start = Math.max(0, size - limit);
        return new ArrayList<>(recentResults.subList(start, size));
    }

    // ==================== Private Helper Methods ====================

    /**
     * Extract acoustic features from raw audio data
     */
    private AcousticFeatures extractAcousticFeatures(byte[] audioData) {
        // Calculate basic audio statistics
        float volumeLevel = calculateRMSLevel(audioData);
        float pitchAverage = estimatePitch(audioData);
        float pitchVariance = calculatePitchVariance(audioData);
        float speechRate = estimateSpeechRate(audioData);
        float toneQuality = assessToneQuality(audioData);

        return new AcousticFeatures(pitchAverage, pitchVariance, speechRate, volumeLevel, toneQuality);
    }

    /**
     * Calculate RMS (Root Mean Square) volume level
     */
    private float calculateRMSLevel(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return 0f;
        }

        double sum = 0;
        int sampleCount = 0;

        // Process as 16-bit samples (little-endian)
        for (int i = 0; i < audioData.length - 1; i += 2) {
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xFF);
            // Handle signed short
            if (sample > 32767) sample -= 65536;
            sum += sample * sample;
            sampleCount++;
        }

        if (sampleCount == 0) {
            return 0f;
        }

        double rms = Math.sqrt(sum / sampleCount);
        // Normalize to 0-1 range (assuming 16-bit audio)
        return (float) Math.min(1.0, rms / 32768.0);
    }

    /**
     * Estimate fundamental pitch from audio data
     * Uses zero-crossing rate as a simplified pitch estimation
     */
    private float estimatePitch(byte[] audioData) {
        if (audioData == null || audioData.length < 2) {
            return 0f;
        }

        int zeroCrossings = 0;
        int sampleCount = 0;
        short previousSample = 0;

        for (int i = 0; i < audioData.length - 1; i += 2) {
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xFF);
            if (sample > 32767) sample -= 65536;
            short currentSample = (short) sample;

            if (sampleCount > 0) {
                if ((previousSample > 0 && currentSample <= 0) ||
                    (previousSample < 0 && currentSample >= 0)) {
                    zeroCrossings++;
                }
            }
            previousSample = currentSample;
            sampleCount++;
        }

        if (sampleCount == 0) {
            return 0f;
        }

        // Estimate frequency from zero-crossing rate
        // Assuming 16kHz sample rate
        float zeroCrossingRate = (float) zeroCrossings / sampleCount;
        // Pitch estimation: frequency ≈ zeroCrossingRate * sampleRate / 2
        float estimatedPitch = zeroCrossingRate * 8000f;

        // Clamp to reasonable human speech range (80-400 Hz)
        return Math.max(80f, Math.min(400f, estimatedPitch));
    }

    /**
     * Calculate variance in pitch over the audio signal
     */
    private float calculatePitchVariance(byte[] audioData) {
        if (audioData == null || audioData.length < 4) {
            return 0f;
        }

        // Split audio into segments and estimate pitch for each
        List<Float> segmentPitches = new ArrayList<>();
        int segmentSize = Math.max(2, audioData.length / 10); // 10 segments

        for (int i = 0; i < audioData.length - segmentSize; i += segmentSize) {
            byte[] segment = new byte[segmentSize];
            System.arraycopy(audioData, i, segment, 0, segmentSize);
            float pitch = estimatePitch(segment);
            if (pitch > 0) {
                segmentPitches.add(pitch);
            }
        }

        if (segmentPitches.size() < 2) {
            return 0f;
        }

        // Calculate variance
        float mean = (float) segmentPitches.stream().mapToDouble(p -> p).sum() / segmentPitches.size();
        float variance = 0f;
        for (Float pitch : segmentPitches) {
            float diff = pitch - mean;
            variance += diff * diff;
        }
        variance /= segmentPitches.size();

        return variance;
    }

    /**
     * Estimate speech rate (words per minute)
     * Based on energy peaks and silence detection
     */
    private float estimateSpeechRate(byte[] audioData) {
        if (audioData == null || audioData.length < 2) {
            return 0f;
        }

        // Count energy peaks above threshold
        int segmentSize = 1600; // ~100ms at 16kHz
        int peakCount = 0;
        boolean wasSilent = true;

        for (int i = 0; i < audioData.length - segmentSize; i += segmentSize) {
            float energy = calculateRMSLevel(copyOfRange(audioData, i, i + segmentSize));

            // Detect speech vs silence (energy threshold ~0.05)
            boolean isSpeaking = energy > 0.05f;

            // Count transitions from silence to speech as word boundaries
            if (isSpeaking && wasSilent) {
                peakCount++;
            }
            wasSilent = !isSpeaking;
        }

        // Estimate duration in minutes
        float durationSeconds = (float) audioData.length / 16000f; // Assuming 16kHz
        float durationMinutes = Math.max(0.1f, durationSeconds / 60f);

        // Estimate WPM (assuming ~4 syllables per word on average, each peak ~= 1 syllable)
        float estimatedWPM = (peakCount * 4f) / durationMinutes;

        // Clamp to reasonable range (50-250 WPM)
        return Math.max(50f, Math.min(250f, estimatedWPM));
    }

    /**
     * Assess tone quality (clearness vs hoarseness)
     * Based on harmonic concentration in the signal
     */
    private float assessToneQuality(byte[] audioData) {
        if (audioData == null || audioData.length < 2) {
            return 0f;
        }

        // Simplified tone quality assessment based on dynamic range
        // Higher dynamic range with clear peaks indicates better tone quality
        float rms = calculateRMSLevel(audioData);

        // Count zero crossings (higher suggests more periodic/clear tone)
        int zeroCrossings = 0;
        int sampleCount = 0;
        short previousSample = 0;

        for (int i = 0; i < audioData.length - 1; i += 2) {
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xFF);
            if (sample > 32767) sample -= 65536;
            short currentSample = (short) sample;

            if (sampleCount > 0) {
                if ((previousSample > 0 && currentSample <= 0) ||
                    (previousSample < 0 && currentSample >= 0)) {
                    zeroCrossings++;
                }
            }
            previousSample = currentSample;
            sampleCount++;
        }

        float zeroCrossingRate = sampleCount > 0 ? (float) zeroCrossings / sampleCount : 0f;

        // Ideal zero crossing rate for human speech is around 0.05-0.1
        // Deviation from this range indicates lower tone quality
        float idealZCR = 0.075f;
        float zcrDeviation = Math.abs(zeroCrossingRate - idealZCR) / idealZCR;

        // Combine RMS and ZCR deviation for quality score
        float quality = (rms * 0.6f) + ((1f - Math.min(1f, zcrDeviation)) * 0.4f);

        return Math.max(0f, Math.min(1f, quality));
    }

    /**
     * Infer acoustic features from text characteristics
     */
    private AcousticFeatures inferFeaturesFromText(String text) {
        String lowerText = text.toLowerCase();

        // Estimate speech rate from text length and punctuation
        int wordCount = text.split("\\s+").length;
        int questionCount = text.split("\\?").length - 1;
        int exclamationCount = text.split("!").length - 1;

        // Base speech rate on text length (estimated reading time)
        float speechRate = Math.min(250f, 120f + (wordCount * 2f));

        // Adjust for emotional indicators
        float volumeLevel = 0.5f; // Default
        if (exclamationCount > 0 || questionCount > 0) {
            volumeLevel = 0.7f; // More emphatic
        }

        // Pitch indicators from text analysis
        float pitchAverage = 180f; // Default middle pitch
        float pitchVariance = 30f;

        // Detect emotional keywords
        if (containsEmotionalWords(lowerText, List.of("happy", "joy", "great", "wonderful", "excellent"))) {
            pitchAverage = 220f;
            pitchVariance = 50f;
            speechRate = Math.min(250f, speechRate + 30f);
        } else if (containsEmotionalWords(lowerText, List.of("sad", "unhappy", "depressed", "sorry", "unfortunately"))) {
            pitchAverage = 140f;
            pitchVariance = 15f;
            speechRate = Math.max(80f, speechRate - 30f);
        } else if (containsEmotionalWords(lowerText, List.of("angry", "mad", "furious", "hate", "terrible"))) {
            pitchAverage = 200f;
            pitchVariance = 70f;
            speechRate = Math.min(280f, speechRate + 50f);
            volumeLevel = 0.85f;
        } else if (containsEmotionalWords(lowerText, List.of("excited", "amazing", "incredible", "wow", "awesome"))) {
            pitchAverage = 280f;
            pitchVariance = 80f;
            speechRate = Math.min(270f, speechRate + 60f);
        } else if (containsEmotionalWords(lowerText, List.of("scared", "afraid", "worried", "nervous", "fear"))) {
            pitchAverage = 260f;
            pitchVariance = 60f;
            speechRate = Math.max(90f, speechRate - 20f);
        }

        // Estimate tone quality from sentence structure
        float toneQuality = text.length() > 20 ? 0.7f : 0.5f;

        return new AcousticFeatures(pitchAverage, pitchVariance, speechRate, volumeLevel, toneQuality);
    }

    /**
     * Check if text contains any words from the list
     */
    private boolean containsEmotionalWords(String text, List<String> words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Map acoustic features to emotion category
     */
    private EmotionPair mapFeaturesToEmotion(AcousticFeatures features) {
        float pitchAvg = features.pitchAverage();
        float pitchVar = features.pitchVariance();
        float speechRate = features.speechRate();
        float volume = features.volumeLevel();
        float toneQuality = features.toneQuality();

        // Emotion scoring based on acoustic features
        Map<Emotion, Float> scores = new HashMap<>();

        // HAPPY: moderate-high pitch, moderate pitch variance, moderate speech rate, moderate-high volume
        scores.put(Emotion.HAPPY,
                (pitchAvg / 400f) * 0.3f +
                Math.min(1f, pitchVar / 60f) * 0.2f +
                Math.min(1f, speechRate / 180f) * 0.25f +
                Math.min(1f, volume / 0.7f) * 0.25f);

        // SAD: low pitch, low pitch variance, slow speech rate, low volume
        scores.put(Emotion.SAD,
                (1f - Math.min(1f, pitchAvg / 200f)) * 0.25f +
                (1f - Math.min(1f, pitchVar / 30f)) * 0.25f +
                (1f - Math.min(1f, speechRate / 120f)) * 0.3f +
                (1f - Math.min(1f, volume / 0.4f)) * 0.2f);

        // ANGRY: high pitch, high pitch variance, fast speech rate, high volume
        scores.put(Emotion.ANGRY,
                Math.min(1f, pitchAvg / 300f) * 0.2f +
                Math.min(1f, pitchVar / 80f) * 0.25f +
                Math.min(1f, speechRate / 220f) * 0.25f +
                Math.min(1f, volume / 0.85f) * 0.3f);

        // CALM: moderate pitch, low pitch variance, moderate speech rate, moderate volume
        scores.put(Emotion.CALM,
                (1f - Math.abs(pitchAvg - 180f) / 100f) * 0.25f +
                (1f - Math.min(1f, pitchVar / 40f)) * 0.3f +
                (1f - Math.abs(speechRate - 140f) / 80f) * 0.25f +
                (1f - Math.abs(volume - 0.5f) / 0.3f) * 0.2f);

        // EXCITED: high pitch, high pitch variance, fast speech rate, high volume
        scores.put(Emotion.EXCITED,
                Math.min(1f, pitchAvg / 350f) * 0.25f +
                Math.min(1f, pitchVar / 70f) * 0.25f +
                Math.min(1f, speechRate / 200f) * 0.25f +
                Math.min(1f, volume / 0.8f) * 0.25f);

        // FRUSTRATED: moderate-high pitch, moderate-high pitch variance, interrupted speech rate
        scores.put(Emotion.FRUSTRATED,
                Math.min(1f, pitchAvg / 280f) * 0.25f +
                Math.min(1f, pitchVar / 65f) * 0.3f +
                // Frustrated speech often irregular
                ((speechRate > 180f || speechRate < 100f) ? 0.3f : 0.15f) +
                volume * 0.2f);

        // FEARFUL: high pitch, high pitch variance, fast but weak speech, variable volume
        scores.put(Emotion.FEARFUL,
                Math.min(1f, pitchAvg / 320f) * 0.25f +
                Math.min(1f, pitchVar / 75f) * 0.3f +
                Math.min(1f, speechRate / 200f) * 0.2f +
                // Fear often has uneven volume
                (pitchVar > 50f ? 0.25f : 0.15f));

        // NEUTRAL: default scores (adjusted if other emotions are low)
        float maxOtherScore = scores.values().stream().max(Float::compare).orElse(0.3f);
        scores.put(Emotion.NEUTRAL, maxOtherScore * 0.5f);

        // Apply tone quality adjustment
        for (Emotion emotion : scores.keySet()) {
            float tqWeight = emotion == Emotion.CALM ? 0.3f : 0.1f;
            scores.merge(emotion, toneQuality * tqWeight, Float::sum);
        }

        // Find best matching emotion
        Emotion bestEmotion = Emotion.NEUTRAL;
        float bestScore = 0f;

        for (Map.Entry<Emotion, Float> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestEmotion = entry.getKey();
            }
        }

        // Normalize confidence (best score relative to possible max)
        float confidence = Math.min(0.95f, bestScore * 1.5f);

        // Ensure minimum confidence for NEUTRAL
        if (bestEmotion == Emotion.NEUTRAL && confidence < 0.3f) {
            confidence = 0.3f;
        }

        return new EmotionPair(bestEmotion, confidence);
    }

    /**
     * Internal record for emotion result pair
     */
    private record EmotionPair(Emotion emotion, float confidence) {}

    /**
     * Create unknown/default result
     */
    private VoiceEmotionResult createUnknownResult() {
        return new VoiceEmotionResult(
                Emotion.NEUTRAL,
                0f,
                new AcousticFeatures(0f, 0f, 0f, 0f, 0f),
                Instant.now()
        );
    }

    /**
     * Cache result in recent results
     */
    private void cacheResult(VoiceEmotionResult result) {
        synchronized (recentResults) {
            recentResults.add(result);
            if (recentResults.size() > MAX_RECENT_RESULTS) {
                recentResults.remove(0);
            }
        }
    }

    /**
     * Integrate with EmotionHistoryService
     */
    private void integrateWithEmotionHistory(VoiceEmotionResult result) {
        try {
            // Map voice emotion to OwnerModel.Mood
            OwnerModel.Mood mood = mapToOwnerMood(result.emotion());
            float intensity = result.confidence();

            // Record in emotion history service
            emotionHistoryService.recordEmotion(mood, intensity, "voice_analysis");
        } catch (Exception e) {
            logger.warn("Failed to integrate with EmotionHistoryService", e);
        }
    }

    /**
     * Map VoiceEmotion to OwnerModel.Mood
     */
    private OwnerModel.Mood mapToOwnerMood(Emotion emotion) {
        return switch (emotion) {
            case HAPPY -> OwnerModel.Mood.HAPPY;
            case SAD -> OwnerModel.Mood.SAD;
            case ANGRY -> OwnerModel.Mood.ANXIOUS; // Map to closest
            case CALM -> OwnerModel.Mood.CALM;
            case EXCITED -> OwnerModel.Mood.EXCITED;
            case FRUSTRATED -> OwnerModel.Mood.FRUSTRATED;
            case FEARFUL -> OwnerModel.Mood.ANXIOUS;
            case NEUTRAL -> OwnerModel.Mood.NEUTRAL;
        };
    }

    /**
     * Calculate volatility of emotion results
     */
    private float calculateVolatility(List<VoiceEmotionResult> history) {
        if (history.size() < 2) {
            return 0f;
        }

        // Calculate variance in confidence scores
        float mean = (float) history.stream()
                .mapToDouble(VoiceEmotionResult::confidence)
                .average()
                .orElse(0.5f);

        double variance = 0;
        for (VoiceEmotionResult result : history) {
            float diff = result.confidence() - mean;
            variance += diff * diff;
        }
        variance /= history.size();

        // Normalize to 0-1 range (max variance ~0.25)
        return (float) Math.min(1.0, variance * 4);
    }

    /**
     * Determine trend direction from history
     */
    private TrendDirection determineTrendDirection(List<VoiceEmotionResult> history) {
        if (history.size() < 3) {
            return TrendDirection.STABLE;
        }

        // Split into first half and second half
        int mid = history.size() / 2;
        List<VoiceEmotionResult> firstHalf = history.subList(0, mid);
        List<VoiceEmotionResult> secondHalf = history.subList(mid, history.size());

        // Calculate average confidence for each half
        float firstAvg = (float) firstHalf.stream()
                .mapToDouble(VoiceEmotionResult::confidence)
                .average()
                .orElse(0.5f);

        float secondAvg = (float) secondHalf.stream()
                .mapToDouble(VoiceEmotionResult::confidence)
                .average()
                .orElse(0.5f);

        float diff = secondAvg - firstAvg;

        if (diff > 0.15f) {
            return TrendDirection.RISING;
        } else if (diff < -0.15f) {
            return TrendDirection.FALLING;
        } else {
            return TrendDirection.STABLE;
        }
    }

    /**
     * Calculate trend strength
     */
    private float calculateTrendStrength(List<VoiceEmotionResult> history) {
        if (history.size() < 3) {
            return 0f;
        }

        // Calculate the consistency of emotion categories
        Map<Emotion, Integer> counts = new HashMap<>();
        for (VoiceEmotionResult result : history) {
            counts.merge(result.emotion(), 1, Integer::sum);
        }

        // Find dominant emotion percentage
        int maxCount = counts.values().stream().max(Integer::compare).orElse(1);
        float dominance = (float) maxCount / history.size();

        // Calculate trend strength based on consistency and direction
        float consistency = dominance;
        float changeRate = calculateChangeRate(history);

        return (consistency * 0.6f) + (changeRate * 0.4f);
    }

    /**
     * Calculate rate of change in emotion results
     */
    private float calculateChangeRate(List<VoiceEmotionResult> history) {
        if (history.size() < 2) {
            return 0f;
        }

        int changes = 0;
        Emotion previous = history.get(0).emotion();

        for (int i = 1; i < history.size(); i++) {
            if (history.get(i).emotion() != previous) {
                changes++;
            }
            previous = history.get(i).emotion();
        }

        return (float) changes / (history.size() - 1);
    }

    /**
     * Utility method to copy a range of array
     */
    private static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) throw new IllegalArgumentException(from + " > " + to);
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }
}
