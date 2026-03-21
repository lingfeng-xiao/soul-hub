package com.openclaw.digitalbeings.interfaces.rest.hostcontract;

import com.openclaw.digitalbeings.application.hostcontract.HostContractService;
import com.openclaw.digitalbeings.application.hostcontract.HostContractView;
import com.openclaw.digitalbeings.application.hostcontract.RegisterHostContractCommand;
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
@RequestMapping("/host-contracts")
public class HostContractController {

    private final HostContractService hostContractService;

    public HostContractController(HostContractService hostContractService) {
        this.hostContractService = hostContractService;
    }

    @PostMapping
    public ResponseEntity<RequestEnvelope<HostContractView>> registerHostContract(@RequestBody RegisterHostContractRequest request) {
        HostContractView data = hostContractService.registerHostContract(new RegisterHostContractCommand(
                request.beingId(),
                request.hostType(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/{beingId}")
    public ResponseEntity<RequestEnvelope<List<HostContractView>>> listHostContracts(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(hostContractService.listHostContracts(beingId)));
    }
}
