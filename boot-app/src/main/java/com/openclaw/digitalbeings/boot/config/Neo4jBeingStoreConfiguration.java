package com.openclaw.digitalbeings.boot.config;

import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.adapter.Neo4jBeingStore;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository.BeingNodeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@Configuration(proxyBeanMethods = false)
@Profile("neo4j")
@ConditionalOnMissingBean(BeingNodeRepository.class)
@EnableNeo4jRepositories(basePackageClasses = BeingNodeRepository.class)
class Neo4jBeingStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean(Neo4jBeingStore.class)
    Neo4jBeingStore neo4jBeingStore(BeingNodeRepository repository) {
        return new Neo4jBeingStore(repository);
    }
}
