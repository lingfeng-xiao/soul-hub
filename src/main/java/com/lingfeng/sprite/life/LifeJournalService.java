package com.lingfeng.sprite.life;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.controller.dto.LifeCommandRequest;
import com.lingfeng.sprite.domain.command.CommandResult;
import com.lingfeng.sprite.life.persistence.LifeJournalEntryEntity;
import com.lingfeng.sprite.life.persistence.LifeJournalEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LifeJournalService {

    private static final Logger logger = LoggerFactory.getLogger(LifeJournalService.class);

    private final LifeJournalEntryRepository repository;
    private final ObjectMapper objectMapper;

    public LifeJournalService(LifeJournalEntryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public LifeJournalEntryView record(String entryType, String title, String detail, Object payload) {
        LifeJournalEntryEntity entity = new LifeJournalEntryEntity();
        entity.setEntryType(entryType);
        entity.setTitle(title);
        entity.setDetail(detail);
        entity.setPayloadJson(toJson(payload));
        entity.setCreatedAt(Instant.now());
        LifeJournalEntryEntity saved = repository.save(entity);
        return toView(saved);
    }

    public LifeJournalEntryView recordCommand(LifeCommandRequest request, CommandResult result) {
        String title = request.type() + ": " + truncate(request.content(), 72);
        String detail = result.getDetail().isBlank() ? result.getSummary() : result.getDetail();
        return record("COMMAND", title, detail, result);
    }

    public List<LifeJournalEntryView> getRecentEntries(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toView)
                .toList();
    }

    public void clear() {
        repository.deleteAllInBatch();
    }

    private LifeJournalEntryView toView(LifeJournalEntryEntity entity) {
        return new LifeJournalEntryView(
                entity.getId(),
                entity.getEntryType(),
                entity.getTitle(),
                entity.getDetail(),
                entity.getCreatedAt()
        );
    }

    private String toJson(Object payload) {
        if (payload == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            logger.debug("Failed to serialize journal payload: {}", exception.getMessage());
            return "{}";
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    public record LifeJournalEntryView(
            Long id,
            String entryType,
            String title,
            String detail,
            Instant createdAt
    ) {}
}
