package com.lingfeng.sprite.life.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LifeCommandExecutionRepository extends JpaRepository<LifeCommandExecutionEntity, Long> {}
