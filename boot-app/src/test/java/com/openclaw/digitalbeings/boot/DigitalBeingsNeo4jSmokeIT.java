package com.openclaw.digitalbeings.boot;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.adapter.Neo4jBeingStore;
import java.util.UUID;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = DigitalBeingsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=neo4j"
        }
)
@ActiveProfiles("neo4j")
@EnabledIfEnvironmentVariable(named = "DIGITAL_BEINGS_NEO4J_URI", matches = ".*")
class DigitalBeingsNeo4jSmokeIT {

    @Autowired
    private BeingService beingService;

    @Autowired
    private BeingStore beingStore;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void smokeFlowPersistsAndReloadsThroughNeo4j() {
        assertInstanceOf(Neo4jBeingStore.class, beingStore);

        Object beingNodeRepository = applicationContext.getBean("beingNodeRepository");
        long beforeCount = invokeLong(beingNodeRepository, "count");
        String displayName = "remote-smoke-" + UUID.randomUUID();
        String actor = "neo4j-smoke";

        BeingView created = beingService.createBeing(new CreateBeingCommand(displayName, actor));
        assertNotNull(created.beingId());
        assertEquals(displayName, created.displayName());

        BeingView reloaded = beingService.getBeing(created.beingId());
        assertEquals(created.beingId(), reloaded.beingId());
        assertEquals(created.displayName(), reloaded.displayName());
        assertEquals(created.revision(), reloaded.revision());

        Optional<?> persisted = invokeOptional(beingNodeRepository, "findByBeingId", created.beingId());
        assertTrue(persisted.isPresent());
        assertEquals(beforeCount + 1, invokeLong(beingNodeRepository, "count"));
    }

    private static long invokeLong(Object target, String methodName) {
        Object result = invoke(target, methodName);
        if (result instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException(methodName + " did not return a numeric value: " + result);
    }

    @SuppressWarnings("unchecked")
    private static Optional<?> invokeOptional(Object target, String methodName, Object argument) {
        Object result = invoke(target, methodName, argument);
        if (result instanceof Optional<?> optional) {
            return optional;
        }
        throw new IllegalStateException(methodName + " did not return an Optional: " + result);
    }

    private static Object invoke(Object target, String methodName, Object... arguments) {
        try {
            Class<?>[] parameterTypes = new Class<?>[arguments.length];
            for (int index = 0; index < arguments.length; index += 1) {
                parameterTypes[index] = arguments[index].getClass();
            }
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke " + methodName + " on " + target.getClass().getName(), exception);
        }
    }
}
