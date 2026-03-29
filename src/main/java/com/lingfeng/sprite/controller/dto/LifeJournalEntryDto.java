package com.lingfeng.sprite.controller.dto;

import com.lingfeng.sprite.life.LifeJournalService;

import java.time.Instant;

public record LifeJournalEntryDto(
        Long id,
        String entryType,
        String title,
        String detail,
        Instant createdAt
) {
    public static LifeJournalEntryDto from(LifeJournalService.LifeJournalEntryView view) {
        return new LifeJournalEntryDto(
                view.id(),
                view.entryType(),
                view.title(),
                view.detail(),
                view.createdAt()
        );
    }
}
