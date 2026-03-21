package com.openclaw.digitalbeings.boot.config;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("memory")
public class MemoryBeingStoreConfiguration {

    private BeingStore beingStore;

    @Bean
    BeingStore beingStore(InMemoryBeingStore store) {
        this.beingStore = store;
        return store;
    }

    @PostConstruct
    public void validateNeo4jProfile() {
        if (beingStore instanceof InMemoryBeingStore) {
            throw new IllegalStateException(
                "InMemoryBeingStore is deprecated for production use. " +
                "Please activate the 'neo4j' profile to use Neo4j存储. " +
                "Run with: --spring.profiles.active=neo4j"
            );
        }
    }
}
