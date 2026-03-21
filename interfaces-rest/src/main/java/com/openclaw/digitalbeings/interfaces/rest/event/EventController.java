package com.openclaw.digitalbeings.interfaces.rest.event;

import com.openclaw.digitalbeings.application.event.DomainEventView;
import com.openclaw.digitalbeings.application.event.EventQueryService;
import com.openclaw.digitalbeings.domain.core.DomainEventType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {
    private final EventQueryService eventQueryService;

    public EventController(EventQueryService eventQueryService) {
        this.eventQueryService = eventQueryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DomainEventView> queryEvents(
        @RequestParam String beingId,
        @RequestParam(required = false) DomainEventType eventType,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @RequestParam(defaultValue = "100") int limit
    ) {
        return eventQueryService.queryEvents(beingId, eventType, from, to, limit)
            .stream().map(DomainEventView::from).toList();
    }
}
