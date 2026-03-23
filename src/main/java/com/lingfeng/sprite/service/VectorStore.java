package com.lingfeng.sprite.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * S20-1: 向量嵌入相似度检索 - Vector embedding similarity search
 *
 * Simple in-memory vector store for semantic similarity search.
 * Provides cosine similarity-based retrieval of memory embeddings.
 */
@Service
public class VectorStore {

    // Vector storage: id -> embedding vector
    private final Map<String, float[]> vectors = new ConcurrentHashMap<>();

    // Metadata storage: id -> memory metadata
    private final Map<String, VectorMetadata> metadata = new ConcurrentHashMap<>();

    /**
     * Vector metadata for retrieval context
     */
    public record VectorMetadata(
        String id,
        String memoryType,    // EPISODIC, SEMANTIC, PROCEDURAL, PERCEPTIVE
        String content,
        long timestamp
    ) {}

    /**
     * Search result with similarity score
     */
    public record SearchResult(
        String id,
        float score,
        VectorMetadata metadata
    ) {}

    /**
     * Store a vector embedding with metadata
     *
     * @param id Unique identifier for the memory
     * @param embedding The vector embedding
     * @param memoryType Type of memory (EPISODIC, SEMANTIC, etc.)
     * @param content Original content for context
     */
    public void store(String id, float[] embedding, String memoryType, String content) {
        vectors.put(id, embedding.clone());
        metadata.put(id, new VectorMetadata(id, memoryType, content, System.currentTimeMillis()));
    }

    /**
     * Store a vector embedding without metadata
     *
     * @param id Unique identifier
     * @param embedding The vector embedding
     */
    public void store(String id, float[] embedding) {
        store(id, embedding, "UNKNOWN", "");
    }

    /**
     * Search for similar vectors using cosine similarity
     *
     * @param queryEmbedding The query vector
     * @param topK Number of results to return
     * @return List of top-k similar vector IDs with scores
     */
    public List<String> search(float[] queryEmbedding, int topK) {
        return searchWithScores(queryEmbedding, topK).stream()
            .map(SearchResult::id)
            .collect(Collectors.toList());
    }

    /**
     * Search for similar vectors with similarity scores
     *
     * @param queryEmbedding The query vector
     * @param topK Number of results to return
     * @return List of search results with scores
     */
    public List<SearchResult> searchWithScores(float[] queryEmbedding, int topK) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return List.of();
        }

        // Calculate similarity for all vectors
        List<SearchResult> results = vectors.entrySet().stream()
            .map(entry -> {
                String id = entry.getKey();
                float[] vector = entry.getValue();
                float similarity = cosineSimilarity(queryEmbedding, vector);
                return new SearchResult(id, similarity, metadata.get(id));
            })
            .filter(r -> r.score() > 0) // Filter out zero similarity
            .sorted((a, b) -> Float.compare(b.score(), a.score())) // Sort by score descending
            .limit(topK)
            .collect(Collectors.toList());

        return results;
    }

    /**
     * Search within a specific memory type
     *
     * @param queryEmbedding The query vector
     * @param memoryType Filter by memory type
     * @param topK Number of results
     * @return Filtered search results
     */
    public List<SearchResult> searchByType(float[] queryEmbedding, String memoryType, int topK) {
        return searchWithScores(queryEmbedding, vectors.size()).stream()
            .filter(r -> r.metadata() != null && memoryType.equals(r.metadata().memoryType()))
            .limit(topK)
            .collect(Collectors.toList());
    }

    /**
     * Calculate cosine similarity between two vectors
     *
     * @param a First vector
     * @param b Second vector
     * @return Cosine similarity score (0 to 1)
     */
    public float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0f;
        }

        float dotProduct = 0f;
        float normA = 0f;
        float normB = 0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0f || normB == 0f) {
            return 0f;
        }

        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Get a vector by ID
     *
     * @param id Vector identifier
     * @return The vector or null if not found
     */
    public float[] get(String id) {
        float[] vector = vectors.get(id);
        return vector != null ? vector.clone() : null;
    }

    /**
     * Get metadata for a vector
     *
     * @param id Vector identifier
     * @return Metadata or null
     */
    public VectorMetadata getMetadata(String id) {
        return metadata.get(id);
    }

    /**
     * Delete a vector
     *
     * @param id Vector identifier
     * @return true if deleted
     */
    public boolean delete(String id) {
        vectors.remove(id);
        return metadata.remove(id) != null;
    }

    /**
     * Get all vector IDs
     *
     * @return Set of all IDs
     */
    public Set<String> getAllIds() {
        return new HashSet<>(vectors.keySet());
    }

    /**
     * Get count of stored vectors
     *
     * @return Number of vectors
     */
    public int size() {
        return vectors.size();
    }

    /**
     * Clear all vectors
     */
    public void clear() {
        vectors.clear();
        metadata.clear();
    }

    /**
     * Generate a simple embedding from text (placeholder for real embedding model)
     * In production, this would call an actual embedding model API
     *
     * @param text Input text
     * @return Simple hash-based pseudo-embedding
     */
    public float[] generateTextEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            return new float[128];
        }

        // Simple hash-based embedding for demo purposes
        // In production, use: OpenAI embeddings, Sentence Transformers, etc.
        float[] embedding = new float[128];
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int bucket = (c * (i + 1)) % 128;
            embedding[bucket] += (float) c / 255f;
        }

        // Normalize
        float norm = 0f;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }

        return embedding;
    }
}
