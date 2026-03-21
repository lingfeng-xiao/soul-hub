package com.openclaw.digitalbeings.interfaces.rest.status;

import com.openclaw.digitalbeings.application.status.PlatformStatusService;
import com.github.f4b6a3.ulid.UlidCreator;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/platform")
public class PlatformStatusController {

    private final PlatformStatusService platformStatusService;

    public PlatformStatusController(PlatformStatusService platformStatusService) {
        this.platformStatusService = platformStatusService;
    }

    @GetMapping("/status")
    public RequestEnvelope<Object> status() {
        return new RequestEnvelope<>(
                UlidCreator.getUlid().toString(),
                Instant.now(),
                true,
                platformStatusService.currentStatus(),
                null
        );
    }
}
