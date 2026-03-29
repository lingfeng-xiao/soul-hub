package com.lingfeng.sprite.life.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifeJournalEntryRepository extends JpaRepository<LifeJournalEntryEntity, Long> {
    Page<LifeJournalEntryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
