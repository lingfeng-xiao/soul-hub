package com.openclaw.digitalbeings.interfaces.rest.relationship;

import com.openclaw.digitalbeings.application.relationship.CreateRelationshipEntityCommand;
import com.openclaw.digitalbeings.application.relationship.RelationshipEntityView;
import com.openclaw.digitalbeings.application.relationship.RelationshipService;
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
@RequestMapping("/relationships")
public class RelationshipController {

    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @PostMapping
    public ResponseEntity<RequestEnvelope<RelationshipEntityView>> createRelationshipEntity(
            @RequestBody CreateRelationshipEntityRequest request
    ) {
        RelationshipEntityView data = relationshipService.createRelationshipEntity(new CreateRelationshipEntityCommand(
                request.beingId(),
                request.kind(),
                request.displayName(),
                request.actor()
        ));
        return ResponseEntity.ok(RequestEnvelopes.success(data));
    }

    @GetMapping("/{beingId}")
    public ResponseEntity<RequestEnvelope<List<RelationshipEntityView>>> listRelationshipEntities(@PathVariable("beingId") String beingId) {
        return ResponseEntity.ok(RequestEnvelopes.success(relationshipService.listRelationshipEntities(beingId)));
    }
}
