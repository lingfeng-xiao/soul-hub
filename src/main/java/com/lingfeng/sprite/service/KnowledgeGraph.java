package com.lingfeng.sprite.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * S20-2: 知识图谱集成 - Knowledge graph integration
 *
 * Manages entity relationships in a simple knowledge graph structure.
 * Provides traversal and querying of entity connections.
 */
@Service
public class KnowledgeGraph {

    // Entity storage: entity name -> Entity object
    private final Map<String, Entity> entities = new ConcurrentHashMap<>();

    // Relation storage: (entity1, relation, entity2) -> strength
    private final Map<RelationKey, Float> relations = new ConcurrentHashMap<>();

    /**
     * Entity in the knowledge graph
     */
    public record Entity(
        String name,
        String type,          // PERSON, PLACE, CONCEPT, EVENT, OBJECT
        Map<String, String> attributes,
        long createdAt
    ) {
        public Entity(String name, String type) {
            this(name, type, new HashMap<>(), System.currentTimeMillis());
        }
    }

    /**
     * Relation key for hash-based storage
     */
    private record RelationKey(String entity1, String relation, String entity2) {}

    /**
     * Relation query result
     */
    public record Relation(
        String entity1,
        String relation,
        String entity2,
        float strength
    ) {}

    /**
     * Add or update an entity
     *
     * @param entity The entity to add
     */
    public void addEntity(Entity entity) {
        entities.put(entity.name(), entity);
    }

    /**
     * Add an entity by name and type
     *
     * @param name Entity name
     * @param type Entity type
     */
    public void addEntity(String name, String type) {
        entities.put(name, new Entity(name, type));
    }

    /**
     * Add a relation between two entities
     *
     * @param entity1 First entity
     * @param relation Relation type (e.g., "KNOWS", "LOCATED_AT", "RELATED_TO")
     * @param entity2 Second entity
     * @param strength Relation strength (0 to 1)
     */
    public void addRelation(String entity1, String relation, String entity2, float strength) {
        // Ensure entities exist
        if (!entities.containsKey(entity1)) {
            addEntity(entity1, "UNKNOWN");
        }
        if (!entities.containsKey(entity2)) {
            addEntity(entity2, "UNKNOWN");
        }

        // Store relation
        RelationKey key = new RelationKey(entity1, relation, entity2);
        relations.put(key, Math.max(0f, Math.min(1f, strength)));
    }

    /**
     * Add a relation with default strength
     *
     * @param entity1 First entity
     * @param relation Relation type
     * @param entity2 Second entity
     */
    public void addRelation(String entity1, String relation, String entity2) {
        addRelation(entity1, relation, entity2, 0.5f);
    }

    /**
     * Get all entities related to a given entity
     *
     * @param entity Entity name
     * @return List of related entities with relation info
     */
    public List<Relation> getRelated(String entity) {
        return relations.entrySet().stream()
            .filter(e -> e.getKey().entity1().equals(entity) || e.getKey().entity2().equals(entity))
            .map(e -> {
                RelationKey key = e.getKey();
                if (key.entity1().equals(entity)) {
                    return new Relation(key.entity1(), key.relation(), key.entity2(), e.getValue());
                } else {
                    return new Relation(key.entity2(), key.relation(), key.entity1(), e.getValue());
                }
            })
            .sorted((a, b) -> Float.compare(b.strength(), a.strength()))
            .collect(Collectors.toList());
    }

    /**
     * Get entities related via a specific relation type
     *
     * @param entity Entity name
     * @param relationType Filter by relation type
     * @return Filtered list of relations
     */
    public List<Relation> getRelatedByType(String entity, String relationType) {
        return getRelated(entity).stream()
            .filter(r -> r.relation().equals(relationType))
            .collect(Collectors.toList());
    }

    /**
     * Get all entities of a specific type
     *
     * @param type Entity type
     * @return List of matching entities
     */
    public List<Entity> getEntitiesByType(String type) {
        return entities.values().stream()
            .filter(e -> e.type().equals(type))
            .collect(Collectors.toList());
    }

    /**
     * Get an entity by name
     *
     * @param name Entity name
     * @return Entity or null
     */
    public Entity getEntity(String name) {
        return entities.get(name);
    }

    /**
     * Check if entity exists
     *
     * @param name Entity name
     * @return true if exists
     */
    public boolean hasEntity(String name) {
        return entities.containsKey(name);
    }

    /**
     * Get all relations in the graph
     *
     * @return All relations
     */
    public List<Relation> getAllRelations() {
        return relations.entrySet().stream()
            .map(e -> new Relation(
                e.getKey().entity1(),
                e.getKey().relation(),
                e.getKey().entity2(),
                e.getValue()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get relation strength between two entities
     *
     * @param entity1 First entity
     * @param relation Relation type
     * @param entity2 Second entity
     * @return Strength or 0 if not found
     */
    public float getRelationStrength(String entity1, String relation, String entity2) {
        RelationKey key = new RelationKey(entity1, relation, entity2);
        return relations.getOrDefault(key, 0f);
    }

    /**
     * Delete an entity and all its relations
     *
     * @param name Entity name
     */
    public void deleteEntity(String name) {
        entities.remove(name);
        // Remove all relations involving this entity
        relations.keySet().removeIf(key ->
            key.entity1().equals(name) || key.entity2().equals(name));
    }

    /**
     * Delete a specific relation
     *
     * @param entity1 First entity
     * @param relation Relation type
     * @param entity2 Second entity
     */
    public void deleteRelation(String entity1, String relation, String entity2) {
        RelationKey key = new RelationKey(entity1, relation, entity2);
        relations.remove(key);
    }

    /**
     * Get entity count
     *
     * @return Number of entities
     */
    public int entityCount() {
        return entities.size();
    }

    /**
     * Get relation count
     *
     * @return Number of relations
     */
    public int relationCount() {
        return relations.size();
    }

    /**
     * Clear all data
     */
    public void clear() {
        entities.clear();
        relations.clear();
    }

    /**
     * Find path between two entities (BFS)
     *
     * @param start Start entity
     * @param end End entity
     * @return Path or empty list if no path found
     */
    public List<String> findPath(String start, String end) {
        if (!entities.containsKey(start) || !entities.containsKey(end)) {
            return List.of();
        }

        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.offer(List.of(start));
        visited.add(start);

        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String current = path.get(path.size() - 1);

            if (current.equals(end)) {
                return path;
            }

            List<Relation> related = getRelated(current);
            for (Relation r : related) {
                String nextEntity = r.entity2();
                if (!visited.contains(nextEntity)) {
                    visited.add(nextEntity);
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(nextEntity);
                    queue.offer(newPath);
                }
            }
        }

        return List.of(); // No path found
    }
}
