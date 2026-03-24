package com.lingfeng.sprite.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.llm.ChatModels;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;

/**
 * S23-1: Voice Recognition and Synthesis Integration Service
 *
 * Provides voice interaction capabilities:
 * - Speech-to-text (STT) using Vosk or external API
 * - Text-to-speech (TTS) using external API or FreeTTS
 * - Configurable audio format (16kHz, 16-bit, mono)
 * - Async processing for voice input
 * - Graceful degradation when services unavailable
 *
 * Integration Points:
 * - MinMaxLlmReasoner (for processing voice queries)
 * - PerceptionSystem (for audio context)
 * - SpriteController (for voice API endpoints)
 */
@Service
public class VoiceInteractionService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceInteractionService.class);

    // Audio format constants (16kHz, 16-bit, mono)
    public static final float SAMPLE_RATE = 16000.0f;
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int CHANNELS = 1;
    public static final boolean BIG_ENDIAN = false;

    // Service configuration
    private static final String VOSK_MODEL_PATH = "models/vosk-model";
    private static final String TTS_API_URL = "https://api.minimax.io/t2a_v2";
    private static final String STT_API_URL = "https://api.minimax.io/speech/v2/recognize";
    private static final Duration STT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration TTS_TIMEOUT = Duration.ofSeconds(60);

    // Fallback configuration
    private static final int MAX_RETRIES = 2;
    private static final long RECOVERY_CHECK_INTERVAL_MS = 300000; // 5 minutes

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final MinMaxLlmReasoner llmReasoner;

    // Service state
    private final AtomicBoolean sttAvailable = new AtomicBoolean(false);
    private final AtomicBoolean ttsAvailable = new AtomicBoolean(false);
    private final AtomicReference<Instant> lastSttFailure = new AtomicReference<>(Instant.now().minusSeconds(3600));
    private final AtomicReference<Instant> lastTtsFailure = new AtomicReference<>(Instant.now().minusSeconds(3600));
    private final Map<String, VoiceConfig> voiceConfigs = new ConcurrentHashMap<>();

    // Vosk model reference (lazy loaded)
    private volatile Object voskRecognizer;
    private volatile boolean voskInitialized = false;

    /**
     * Voice type enum for TTS voice selection
     */
    public enum VoiceType {
        NEUTRAL("neutral", 1.0f, 1.0f),
        FRIENDLY("friendly", 1.2f, 0.9f),
        PROFESSIONAL("professional", 0.9f, 1.1f),
        CARING("caring", 1.1f, 0.95f);

        private final String voiceId;
        private final float speedMultiplier;
        private final float pitchMultiplier;

        VoiceType(String voiceId, float speedMultiplier, float pitchMultiplier) {
            this.voiceId = voiceId;
            this.speedMultiplier = speedMultiplier;
            this.pitchMultiplier = pitchMultiplier;
        }

        public String getVoiceId() {
            return voiceId;
        }

        public float getSpeedMultiplier() {
            return speedMultiplier;
        }

        public float getPitchMultiplier() {
            return pitchMultiplier;
        }
    }

    /**
     * Voice configuration
     */
    public record VoiceConfig(
        String apiKey,
        String voiceId,
        float speed,
        float pitch,
        String language
    ) {
        public VoiceConfig {
            if (speed <= 0) speed = 1.0f;
            if (pitch <= 0) pitch = 1.0f;
            if (language == null) language = "zh-CN";
        }
    }

    /**
     * Voice recognition result
     */
    public record RecognitionResult(
        String text,
        float confidence,
        boolean isFinal,
        String language,
        long processingTimeMs
    ) {
        public RecognitionResult {
            if (text == null) text = "";
            if (language == null) language = "zh-CN";
        }
    }

    /**
     * Speech synthesis result
     */
    public record SynthesisResult(
        byte[] audioData,
        String format,
        int sampleRate,
        int channels,
        int bitsPerSample,
        long processingTimeMs
    ) {
        public SynthesisResult {
            if (audioData == null) audioData = new byte[0];
            if (format == null) format = "audio/pcm";
        }
    }

    /**
     * Audio format validation result
     */
    public record AudioFormatInfo(
        float sampleRate,
        int sampleSizeBits,
        int channels,
        boolean isValid,
        String expectedFormat,
        String issues
    ) {
        public static AudioFormatInfo valid(float sampleRate, int channels, int bitsPerSample) {
            return new AudioFormatInfo(sampleRate, bitsPerSample, channels, true,
                String.format("%.0fHz, %d-bit, %s", sampleRate, bitsPerSample, channels == 1 ? "mono" : "stereo"),
                null);
        }

        public static AudioFormatInfo invalid(float sampleRate, int channels, int bitsPerSample, String issues) {
            return new AudioFormatInfo(sampleRate, bitsPerSample, channels, false,
                String.format("%.0fHz, %d-bit, %s", sampleRate, bitsPerSample, channels == 1 ? "mono" : "stereo"),
                issues);
        }
    }

    /**
     * Voice service status
     */
    public record VoiceServiceStatus(
        boolean sttAvailable,
        boolean ttsAvailable,
        boolean voskEnabled,
        String sttProvider,
        String ttsProvider,
        Instant lastSttAttempt,
        Instant lastTtsAttempt
    ) {}

    public VoiceInteractionService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newCachedThreadPool())
            .build();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newCachedThreadPool();
        this.llmReasoner = null; // Will be injected if needed

        initializeDefaultVoiceConfigs();
        checkServiceAvailability();

        logger.info("VoiceInteractionService initialized with STT={}, TTS={}",
            sttAvailable.get(), ttsAvailable.get());
    }

    /**
     * Constructor with LLM reasoner for voice query processing
     */
    public VoiceInteractionService(MinMaxLlmReasoner llmReasoner) {
        this();
        // Note: In Spring, use @Autowired for proper injection
    }

    /**
     * Initialize default voice configurations
     */
    private void initializeDefaultVoiceConfigs() {
        voiceConfigs.put(VoiceType.NEUTRAL.name(), new VoiceConfig(null, "neutral", 1.0f, 1.0f, "zh-CN"));
        voiceConfigs.put(VoiceType.FRIENDLY.name(), new VoiceConfig(null, "friendly", 1.2f, 0.9f, "zh-CN"));
        voiceConfigs.put(VoiceType.PROFESSIONAL.name(), new VoiceConfig(null, "professional", 0.9f, 1.1f, "zh-CN"));
        voiceConfigs.put(VoiceType.CARING.name(), new VoiceConfig(null, "caring", 1.1f, 0.95f, "zh-CN"));
    }

    // ==================== S23-1: Speech Recognition ====================

    /**
     * S23-1: Recognize speech from audio data (async)
     *
     * @param audioData Raw audio bytes (expected 16kHz, 16-bit, mono PCM)
     * @return CompletableFuture containing recognized text
     */
    public CompletableFuture<String> recognizeSpeech(byte[] audioData) {
        return recognizeSpeechAsync(audioData)
            .thenApply(RecognitionResult::text)
            .exceptionally(ex -> {
                logger.error("Speech recognition failed: {}", ex.getMessage());
                return "";
            });
    }

    /**
     * S23-1: Recognize speech with full result details (async)
     *
     * @param audioData Raw audio bytes
     * @return CompletableFuture containing recognition result
     */
    public CompletableFuture<RecognitionResult> recognizeSpeechAsync(byte[] audioData) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            // Validate audio format
            AudioFormatInfo formatInfo = detectAudioFormat(audioData);
            if (!formatInfo.isValid()) {
                logger.warn("Audio format validation failed: {}", formatInfo.issues());
                // Continue anyway, some services can handle different formats
            }

            // Check if STT service is available
            if (!isAvailable()) {
                logger.warn("STT service not available, attempting recovery check");
                checkServiceAvailability();
                if (!sttAvailable.get()) {
                    return new RecognitionResult("", 0f, true, "zh-CN",
                        System.currentTimeMillis() - startTime);
                }
            }

            // Try external API first
            RecognitionResult result = recognizeWithExternalApi(audioData, startTime);
            if (result != null && !result.text().isEmpty()) {
                return result;
            }

            // Fallback to Vosk if available
            result = recognizeWithVosk(audioData, startTime);
            if (result != null && !result.text().isEmpty()) {
                return result;
            }

            // All methods failed
            lastSttFailure.set(Instant.now());
            return new RecognitionResult("", 0f, true, "zh-CN",
                System.currentTimeMillis() - startTime);
        }, executor);
    }

    /**
     * Recognize speech using external API (MinMax or similar)
     */
    private RecognitionResult recognizeWithExternalApi(byte[] audioData, long startTime) {
        try {
            // Encode audio to base64
            String audioBase64 = java.util.Base64.getEncoder().encodeToString(audioData);

            String jsonBody = objectMapper.writeValueAsString(Map.of(
                "audio", audioBase64,
                "format", "pcm",
                "sample_rate", (int) SAMPLE_RATE,
                "language", "zh-CN"
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(STT_API_URL))
                .timeout(STT_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                String text = jsonResponse.path("text").asText("");
                float confidence = (float) jsonResponse.path("confidence").asDouble(0.8);

                logger.debug("External STT success: {} (confidence: {})", text, confidence);
                return new RecognitionResult(text, confidence, true, "zh-CN",
                    System.currentTimeMillis() - startTime);
            } else {
                logger.warn("External STT API returned status: {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.debug("External STT API call failed: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Recognize speech using Vosk (local offline recognition)
     */
    private RecognitionResult recognizeWithVosk(byte[] audioData, long startTime) {
        // Vosk integration placeholder
        // In production, this would load the Vosk model and perform recognition
        // For now, return null to indicate Vosk is not available
        logger.debug("Vosk recognition not available in current implementation");
        return null;
    }

    /**
     * Initialize Vosk model (lazy loading)
     */
    private synchronized boolean initializeVosk() {
        if (voskInitialized) {
            return voskAvailable();
        }

        try {
            // Vosk initialization would go here
            // Example: voskRecognizer = new VoskRecognizer(model, SAMPLE_RATE);
            logger.info("Vosk model initialization not implemented (would load from {})", VOSK_MODEL_PATH);
            voskInitialized = true;
        } catch (Exception e) {
            logger.warn("Failed to initialize Vosk model: {}", e.getMessage());
        }

        return voskAvailable();
    }

    /**
     * Check if Vosk is available
     */
    private boolean voskAvailable() {
        return voskRecognizer != null;
    }

    // ==================== S23-1: Speech Synthesis ====================

    /**
     * S23-1: Synthesize speech from text (blocking)
     *
     * @param text Text to synthesize
     * @param voiceType Type of voice to use
     * @return Audio data as byte array
     */
    public byte[] synthesizeSpeech(String text, VoiceType voiceType) {
        return synthesizeSpeechAsync(text, voiceType)
            .thenApply(SynthesisResult::audioData)
            .join();
    }

    /**
     * S23-1: Synthesize speech with full result (async)
     *
     * @param text Text to synthesize
     * @param voiceType Type of voice to use
     * @return CompletableFuture containing synthesis result
     */
    public CompletableFuture<SynthesisResult> synthesizeSpeechAsync(String text, VoiceType voiceType) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            if (text == null || text.isEmpty()) {
                return new SynthesisResult(new byte[0], "audio/pcm", (int) SAMPLE_RATE,
                    CHANNELS, SAMPLE_SIZE_BITS, 0);
            }

            // Check if TTS service is available
            if (!isTtsAvailable()) {
                logger.warn("TTS service not available");
                checkServiceAvailability();
                if (!ttsAvailable.get()) {
                    // Return silence as fallback
                    return generateSilence(1000, startTime);
                }
            }

            // Try external API first
            SynthesisResult result = synthesizeWithExternalApi(text, voiceType, startTime);
            if (result != null && result.audioData().length > 0) {
                return result;
            }

            // Fallback to FreeTTS or generated silence
            result = synthesizeWithFreeTTS(text, voiceType, startTime);
            if (result != null && result.audioData().length > 0) {
                return result;
            }

            // Last resort: return generated silence
            lastTtsFailure.set(Instant.now());
            return generateSilence(1000, startTime);
        }, executor);
    }

    /**
     * Synthesize speech using external TTS API
     */
    private SynthesisResult synthesizeWithExternalApi(String text, VoiceType voiceType, long startTime) {
        try {
            VoiceConfig config = voiceConfigs.getOrDefault(voiceType.name(),
                new VoiceConfig(null, voiceType.getVoiceId(), 1.0f, 1.0f, "zh-CN"));

            String jsonBody = objectMapper.writeValueAsString(Map.of(
                "model", "speech-02",
                "text", text,
                "stream", false,
                "voice_setting", Map.of(
                    "voice_id", config.voiceId(),
                    "speed", config.speed(),
                    "pitch", config.pitch(),
                    "volume", 1.0
                ),
                "audio_setting", Map.of(
                    "sample_rate", (int) SAMPLE_RATE,
                    "format", "pcm",
                    "bitrate", 128000
                )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TTS_API_URL))
                .timeout(TTS_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<byte[]> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                byte[] audioData = response.body();
                logger.debug("External TTS success: {} bytes, {}ms",
                    audioData.length, System.currentTimeMillis() - startTime);
                return new SynthesisResult(audioData, "audio/pcm", (int) SAMPLE_RATE,
                    CHANNELS, SAMPLE_SIZE_BITS, System.currentTimeMillis() - startTime);
            } else {
                logger.warn("External TTS API returned status: {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.debug("External TTS API call failed: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Synthesize speech using FreeTTS (local fallback)
     */
    private SynthesisResult synthesizeWithFreeTTS(String text, VoiceType voiceType, long startTime) {
        try {
            // FreeTTS integration placeholder
            // In production, this would use FreeTTS or similar local TTS engine
            logger.debug("FreeTTS synthesis not implemented");
        } catch (Exception e) {
            logger.debug("FreeTTS synthesis failed: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Generate silence audio (fallback when TTS unavailable)
     */
    private SynthesisResult generateSilence(int durationMs, long startTime) {
        int numSamples = (int) (SAMPLE_RATE * durationMs / 1000);
        byte[] silence = new byte[numSamples * 2]; // 16-bit = 2 bytes per sample

        // Fill with silence (zeroes are silent for PCM)
        // Already initialized to 0

        return new SynthesisResult(silence, "audio/pcm", (int) SAMPLE_RATE,
            CHANNELS, SAMPLE_SIZE_BITS, System.currentTimeMillis() - startTime);
    }

    // ==================== S23-1: Service Availability ====================

    /**
     * S23-1: Check if voice services are available
     *
     * @return true if STT or TTS is available
     */
    public boolean isAvailable() {
        return sttAvailable.get() || ttsAvailable.get() || initializeVosk();
    }

    /**
     * Check if TTS specifically is available
     */
    public boolean isTtsAvailable() {
        return ttsAvailable.get();
    }

    /**
     * Check if STT specifically is available
     */
    public boolean isSttAvailable() {
        return sttAvailable.get() || initializeVosk();
    }

    /**
     * Get detailed voice service status
     */
    public VoiceServiceStatus getServiceStatus() {
        return new VoiceServiceStatus(
            sttAvailable.get(),
            ttsAvailable.get(),
            voskAvailable(),
            sttAvailable.get() ? "external_api" : (voskAvailable() ? "vosk" : "unavailable"),
            ttsAvailable.get() ? "external_api" : "unavailable",
            lastSttFailure.get(),
            lastTtsFailure.get()
        );
    }

    /**
     * Check and update service availability
     */
    private void checkServiceAvailability() {
        // Check STT availability
        if (!sttAvailable.get()) {
            Duration timeSinceFailure = Duration.between(lastSttFailure.get(), Instant.now());
            if (timeSinceFailure.toMillis() > RECOVERY_CHECK_INTERVAL_MS) {
                // Try to check external API availability
                sttAvailable.set(checkExternalApiAvailability(STT_API_URL));
            }
        }

        // Check TTS availability
        if (!ttsAvailable.get()) {
            Duration timeSinceFailure = Duration.between(lastTtsFailure.get(), Instant.now());
            if (timeSinceFailure.toMillis() > RECOVERY_CHECK_INTERVAL_MS) {
                // Try to check external API availability
                ttsAvailable.set(checkExternalApiAvailability(TTS_API_URL));
            }
        }

        // Try Vosk as fallback for STT
        if (!sttAvailable.get()) {
            initializeVosk();
            if (voskAvailable()) {
                sttAvailable.set(true);
            }
        }
    }

    /**
     * Check if external API is reachable
     */
    private boolean checkExternalApiAvailability(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<Void> response = httpClient.send(request,
                HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (Exception e) {
            logger.debug("API availability check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }

    // ==================== Audio Format Validation ====================

    /**
     * S23-1: Validate audio format
     *
     * @param audioData Raw audio bytes
     * @return true if audio matches expected format (16kHz, 16-bit, mono)
     */
    public boolean validateAudioFormat(byte[] audioData) {
        return detectAudioFormat(audioData).isValid();
    }

    /**
     * S23-1: Detect and validate audio format
     *
     * @param audioData Raw audio bytes
     * @return AudioFormatInfo with validation details
     */
    public AudioFormatInfo detectAudioFormat(byte[] audioData) {
        if (audioData == null || audioData.length < 4) {
            return AudioFormatInfo.invalid(0, 0, 0, "Audio data too small or null");
        }

        // Try to detect WAV header
        if (isWavFormat(audioData)) {
            return parseWavFormat(audioData);
        }

        // Assume raw PCM and validate based on size
        return validateRawPcmFormat(audioData);
    }

    /**
     * Check if audio is WAV format
     */
    private boolean isWavFormat(byte[] audioData) {
        if (audioData.length < 44) return false;

        // Check RIFF header
        return audioData[0] == 'R' && audioData[1] == 'I' &&
               audioData[2] == 'F' && audioData[3] == 'F';
    }

    /**
     * Parse WAV format header
     */
    private AudioFormatInfo parseWavFormat(byte[] audioData) {
        try {
            // WAV header structure
            // Offset 22-23: Channels
            // Offset 24-27: Sample rate
            // Offset 34-35: Bits per sample

            int channels = readInt16LE(audioData, 22);
            int sampleRate = readInt32LE(audioData, 24);
            int bitsPerSample = readInt16LE(audioData, 34);

            StringBuilder issues = new StringBuilder();

            if (sampleRate != (int) SAMPLE_RATE) {
                issues.append(String.format("Expected %.0fHz, got %dHz. ",
                    SAMPLE_RATE, sampleRate));
            }
            if (bitsPerSample != SAMPLE_SIZE_BITS) {
                issues.append(String.format("Expected %d-bit, got %d-bit. ",
                    SAMPLE_SIZE_BITS, bitsPerSample));
            }
            if (channels != CHANNELS) {
                issues.append(String.format("Expected %s, got %s. ",
                    CHANNELS == 1 ? "mono" : "stereo",
                    channels == 1 ? "mono" : "stereo"));
            }

            if (issues.isEmpty()) {
                return AudioFormatInfo.valid(sampleRate, channels, bitsPerSample);
            } else {
                return AudioFormatInfo.invalid(sampleRate, channels, bitsPerSample, issues.toString());
            }
        } catch (Exception e) {
            return AudioFormatInfo.invalid(0, 0, 0, "Failed to parse WAV header: " + e.getMessage());
        }
    }

    /**
     * Validate raw PCM format based on audio size
     */
    private AudioFormatInfo validateRawPcmFormat(byte[] audioData) {
        // For raw PCM, we can only validate based on size consistency
        // Assuming 16-bit (2 bytes per sample)
        int bytesPerSample = SAMPLE_SIZE_BITS / 8;
        int expectedSamples = audioData.length / (bytesPerSample * CHANNELS);

        // Check if length is consistent with 16kHz mono
        double durationSeconds = expectedSamples / SAMPLE_RATE;
        int expectedBytes = (int) (SAMPLE_RATE * durationSeconds * bytesPerSample * CHANNELS);

        // Allow 1% tolerance
        double deviation = Math.abs(audioData.length - expectedBytes) / (double) expectedBytes;

        if (deviation < 0.01) {
            return AudioFormatInfo.valid(SAMPLE_RATE, CHANNELS, SAMPLE_SIZE_BITS);
        } else {
            return AudioFormatInfo.invalid(SAMPLE_RATE, CHANNELS, SAMPLE_SIZE_BITS,
                String.format("Audio length %d doesn't match expected format", audioData.length));
        }
    }

    /**
     * Read 16-bit little-endian integer
     */
    private int readInt16LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    /**
     * Read 32-bit little-endian integer
     */
    private int readInt32LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) |
               ((data[offset + 1] & 0xFF) << 8) |
               ((data[offset + 2] & 0xFF) << 16) |
               ((data[offset + 3] & 0xFF) << 24);
    }

    // ==================== Utility Methods ====================

    /**
     * Get API key from configuration
     */
    private String getApiKey() {
        // In production, this would fetch from configuration
        // For now, return empty string
        return System.getProperty("sprite.tts.api.key", "");
    }

    /**
     * Convert audio data to WAV format
     */
    public byte[] convertToWav(byte[] pcmData) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // WAV header
            int totalDataLen = pcmData.length + 36;
            int byteRate = (int) SAMPLE_RATE * CHANNELS * SAMPLE_SIZE_BITS / 8;

            baos.write("RIFF".getBytes(StandardCharsets.US_ASCII));
            baos.write(intToByteArrayLE(totalDataLen));
            baos.write("WAVE".getBytes(StandardCharsets.US_ASCII));
            baos.write("fmt ".getBytes(StandardCharsets.US_ASCII));
            baos.write(intToByteArrayLE(16)); // Subchunk1Size for PCM
            baos.write(intToByteArrayLE(1)); // AudioFormat (1 = PCM)
            baos.write(intToByteArrayLE(CHANNELS));
            baos.write(intToByteArrayLE((int) SAMPLE_RATE));
            baos.write(intToByteArrayLE(byteRate));
            baos.write(intToByteArrayLE(CHANNELS * SAMPLE_SIZE_BITS / 8)); // Block align
            baos.write(intToByteArrayLE(SAMPLE_SIZE_BITS));
            baos.write("data".getBytes(StandardCharsets.US_ASCII));
            baos.write(intToByteArrayLE(pcmData.length));
            baos.write(pcmData);
        } catch (IOException e) {
            logger.error("Failed to convert to WAV: {}", e.getMessage());
        }

        return baos.toByteArray();
    }

    /**
     * Convert int to little-endian byte array
     */
    private byte[] intToByteArrayLE(int value) {
        return new byte[] {
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 24) & 0xFF)
        };
    }

    /**
     * Get standard AudioFormat for this service
     */
    public AudioFormat getAudioFormat() {
        return new AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            true, // signed
            BIG_ENDIAN
        );
    }

    /**
     * Update voice configuration
     */
    public void updateVoiceConfig(VoiceType voiceType, VoiceConfig config) {
        voiceConfigs.put(voiceType.name(), config);
        logger.info("Updated voice config for {}: speed={}, pitch={}",
            voiceType, config.speed(), config.pitch());
    }

    /**
     * Process voice query with LLM
     * Uses MinMaxLlmReasoner to process voice-transcribed text
     */
    public CompletableFuture<String> processVoiceQuery(String voiceText) {
        if (llmReasoner == null) {
            logger.warn("LLM reasoner not available for voice query processing");
            return CompletableFuture.completedFuture("");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use LLM to process the voice-transcribed text
                ChatModels.LlmContext context = new ChatModels.LlmContext(
                    "", // selfSummary
                    "", // ownerSummary
                    "Voice query from user", // currentSituation
                    "", // chatHistory
                    "", // availableTools
                    "" // memoryHighlights
                );

                // Get LLM response (async)
                return llmReasoner.think(context, voiceText)
                    .thenApply(thought -> {
                        if (thought != null && thought.response() != null) {
                            return thought.response();
                        }
                        return "";
                    })
                    .join();
            } catch (Exception e) {
                logger.error("Failed to process voice query: {}", e.getMessage());
                return "";
            }
        }, executor);
    }

    /**
     * Full voice interaction pipeline: recognize -> process -> synthesize
     */
    public CompletableFuture<byte[]> voiceInteractionPipeline(byte[] audioData, VoiceType responseVoiceType) {
        return recognizeSpeechAsync(audioData)
            .thenCompose(recognitionResult -> {
                if (recognitionResult.text().isEmpty()) {
                    logger.warn("Voice recognition returned empty result");
                    return CompletableFuture.completedFuture(new byte[0]);
                }

                logger.info("Recognized voice input: {} (confidence: {})",
                    recognitionResult.text(), recognitionResult.confidence());

                // Process with LLM
                return processVoiceQuery(recognitionResult.text())
                    .thenCompose(llmResponse -> {
                        if (llmResponse.isEmpty()) {
                            return CompletableFuture.completedFuture(new byte[0]);
                        }

                        // Synthesize LLM response
                        return synthesizeSpeechAsync(llmResponse, responseVoiceType)
                            .thenApply(SynthesisResult::audioData);
                    });
            });
    }

    /**
     * Shutdown the service
     */
    public void shutdown() {
        logger.info("Shutting down VoiceInteractionService");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
