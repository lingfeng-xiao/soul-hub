package com.openclaw.digitalbeings.interfaces.rest.being;

import com.openclaw.digitalbeings.application.being.AddIdentityFacetCommand;
import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.being.IdentityFacetView;
import com.openclaw.digitalbeings.application.governance.GovernanceService;
import com.openclaw.digitalbeings.application.governance.GovernanceSummaryView;
import com.openclaw.digitalbeings.application.governance.OwnerProfileCompilationView;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelope;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelopes;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/beings")
public class BeingController {

    private final BeingService beingService;
    private final GovernanceService governanceService;

    public BeingController(BeingService beingService, GovernanceService governanceService) {
        this.beingService = beingService;
        this.governanceService = governanceService;
    }

    @PostMapping
    public ResponseEntity<RequestEnvelope<BeingView>> createBeing(@RequestBody CreateBeingRequest request) {
        BeingView data = beingService.createBeing(new CreateBeingCommand(request.displayName(), request.actor()));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/{beingId}")
    public ResponseEntity<RequestEnvelope<BeingView>> getBeing(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(beingService.getBeing(beingId)));
    }

    @GetMapping
    public ResponseEntity<RequestEnvelope<List<BeingView>>> listBeings() {
        return ResponseEntity.ok(RequestEnvelopes.success(beingService.listBeings()));
    }

    @PostMapping("/{beingId}/identity-facets")
    public ResponseEntity<RequestEnvelope<IdentityFacetView>> addIdentityFacet(
            @PathVariable("beingId") String beingId,
            @RequestBody AddIdentityFacetRequest request
    ) {
        IdentityFacetView data = beingService.addIdentityFacet(
                beingId,
                request.kind(),
                request.summary(),
                request.actor(),
                java.time.Clock.systemUTC().instant()
        );
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/{beingId}/governance/summary")
    public ResponseEntity<RequestEnvelope<GovernanceSummaryView>> getGovernanceSummary(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(governanceService.getGovernanceSummary(beingId)));
    }

    @GetMapping("/{beingId}/owner-profile")
    public ResponseEntity<RequestEnvelope<OwnerProfileCompilationView>> getOwnerProfile(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(governanceService.getOwnerProfile(beingId)));
    }
}
