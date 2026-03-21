package com.openclaw.digitalbeings.boot;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.review.ReviewService;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = DigitalBeingsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=memory",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration,org.springframework.boot.autoconfigure.neo4j.Neo4jDataAutoConfiguration"
        }
)
class DigitalBeingsApplicationTests {

    @Autowired
    private BeingService beingService;

    @Autowired
    private LeaseService leaseService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private BeingStore beingStore;

    @Test
    void contextLoads() {
        assertNotNull(beingService);
        assertNotNull(leaseService);
        assertNotNull(reviewService);
        assertInstanceOf(InMemoryBeingStore.class, beingStore);
    }
}
