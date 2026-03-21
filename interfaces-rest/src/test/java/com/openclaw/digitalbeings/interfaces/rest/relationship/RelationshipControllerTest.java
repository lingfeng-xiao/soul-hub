package com.openclaw.digitalbeings.interfaces.rest.relationship;

import com.openclaw.digitalbeings.application.relationship.RelationshipEntityView;
import com.openclaw.digitalbeings.application.relationship.RelationshipService;
import com.openclaw.digitalbeings.interfaces.rest.api.ApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RelationshipControllerTest {

    @Mock
    private RelationshipService relationshipService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RelationshipController(relationshipService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createRelationshipReturnsEnvelope() throws Exception {
        when(relationshipService.createRelationshipEntity(any())).thenReturn(sampleRelationshipView());

        mockMvc.perform(post("/relationships")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"beingId":"01HZX0000000000000000000000","kind":"friend","displayName":"lingfeng","actor":"codex"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relationshipEntityId").value("01HZX0000000000000000000001"));
    }

    @Test
    void listRelationshipsReturnsEnvelope() throws Exception {
        when(relationshipService.listRelationshipEntities("01HZX0000000000000000000000"))
                .thenReturn(List.of(sampleRelationshipView()));

        mockMvc.perform(get("/relationships/01HZX0000000000000000000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].relationshipEntityId").value("01HZX0000000000000000000001"));
    }

    private static RelationshipEntityView sampleRelationshipView() {
        return new RelationshipEntityView(
                "01HZX0000000000000000000000",
                "01HZX0000000000000000000001",
                "friend",
                "lingfeng",
                Instant.parse("2026-03-21T00:00:00Z")
        );
    }
}
