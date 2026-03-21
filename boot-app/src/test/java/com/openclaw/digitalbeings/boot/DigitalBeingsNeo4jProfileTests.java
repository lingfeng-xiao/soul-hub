package com.openclaw.digitalbeings.boot;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.adapter.Neo4jBeingStore;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository.BeingNodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(
        classes = DigitalBeingsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration,org.springframework.boot.autoconfigure.neo4j.Neo4jDataAutoConfiguration"
        }
)
@ActiveProfiles("neo4j")
@Import(DigitalBeingsNeo4jProfileTests.RepositoryStubConfiguration.class)
class DigitalBeingsNeo4jProfileTests {

    @Autowired
    private BeingStore beingStore;

    @Test
    void neo4jProfileUsesRealAdapterWhenRepositoryIsAvailable() {
        assertInstanceOf(Neo4jBeingStore.class, beingStore);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RepositoryStubConfiguration {

        @Bean
        BeingNodeRepository beingNodeRepository() {
            InvocationHandler handler = (proxy, method, args) -> defaultReturnValue(method);
            return (BeingNodeRepository) Proxy.newProxyInstance(
                    BeingNodeRepository.class.getClassLoader(),
                    new Class<?>[] {BeingNodeRepository.class},
                    handler
            );
        }

        @Bean
        Neo4jBeingStore neo4jBeingStore(BeingNodeRepository repository) {
            return new Neo4jBeingStore(repository);
        }

        private static Object defaultReturnValue(Method method) {
            Class<?> returnType = method.getReturnType();
            if (!returnType.isPrimitive()) {
                return null;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == byte.class) {
                return (byte) 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == float.class) {
                return 0F;
            }
            if (returnType == double.class) {
                return 0D;
            }
            if (returnType == char.class) {
                return '\0';
            }
            return null;
        }
    }
}
