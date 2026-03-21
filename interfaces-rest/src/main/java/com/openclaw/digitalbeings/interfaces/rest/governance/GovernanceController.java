package com.openclaw.digitalbeings.interfaces.rest.governance;

import com.openclaw.digitalbeings.application.governance.ManagedAgentSpecView;
import com.openclaw.digitalbeings.application.governance.OwnerProfileFactView;
import com.openclaw.digitalbeings.application.governance.GovernanceService;
import com.openclaw.digitalbeings.application.governance.RecordOwnerProfileFactCommand;
import com.openclaw.digitalbeings.application.governance.RegisterManagedAgentSpecCommand;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelopes;
import com.openclaw.digitalbeings.interfaces.rest.status.RequestEnvelope;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class GovernanceController {

    private final GovernanceService governanceService;

    public GovernanceController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @PostMapping("/owner-profile-facts")
    public ResponseEntity<RequestEnvelope<OwnerProfileFactView>> recordOwnerProfileFact(
            @RequestBody RecordOwnerProfileFactRequest request
    ) {
        OwnerProfileFactView data = governanceService.recordOwnerProfileFact(new RecordOwnerProfileFactCommand(
                request.beingId(),
                request.section(),
                request.key(),
                request.summary(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/owner-profile-facts/{beingId}")
    public ResponseEntity<RequestEnvelope<List<OwnerProfileFactView>>> listOwnerProfileFacts(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(governanceService.listOwnerProfileFacts(beingId)));
    }

    @PostMapping("/managed-agent-specs")
    public ResponseEntity<RequestEnvelope<ManagedAgentSpecView>> registerManagedAgentSpec(
            @RequestBody RegisterManagedAgentSpecRequest request
    ) {
        ManagedAgentSpecView data = governanceService.registerManagedAgentSpec(new RegisterManagedAgentSpecCommand(
                request.beingId(),
                request.role(),
                request.status(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/managed-agent-specs/{beingId}")
    public ResponseEntity<RequestEnvelope<List<ManagedAgentSpecView>>> listManagedAgentSpecs(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(governanceService.listManagedAgentSpecs(beingId)));
    }
}
