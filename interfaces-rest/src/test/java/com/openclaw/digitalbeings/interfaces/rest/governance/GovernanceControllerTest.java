package com.openclaw.digitalbeings.interfaces.rest.governance;

import com.openclaw.digitalbeings.application.governance.ManagedAgentSpecView;
import com.openclaw.digitalbeings.application.governance.OwnerProfileFactView;
import com.openclaw.digitalbeings.application.governance.GovernanceService;
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
class GovernanceControllerTest {

    @Mock
    private GovernanceService governanceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GovernanceController(governanceService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void recordOwnerProfileFactReturnsEnvelope() throws Exception {
        when(governanceService.recordOwnerProfileFact(any())).thenReturn(sampleOwnerProfileFactView());

        mockMvc.perform(post("/owner-profile-facts")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"beingId":"01HZX0000000000000000000000","section":"preferences","key":"tone","summary":"warm","actor":"codex"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.factId").value("01HZX0000000000000000000001"));
    }

    @Test
    void listOwnerProfileFactsReturnsEnvelope() throws Exception {
        when(governanceService.listOwnerProfileFacts("01HZX0000000000000000000000"))
                .thenReturn(List.of(sampleOwnerProfileFactView()));

        mockMvc.perform(get("/owner-profile-facts/01HZX0000000000000000000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].section").value("preferences"));
    }

    @Test
    void registerManagedAgentSpecReturnsEnvelope() throws Exception {
        when(governanceService.registerManagedAgentSpec(any())).thenReturn(sampleManagedAgentSpecView());

        mockMvc.perform(post("/managed-agent-specs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"beingId":"01HZX0000000000000000000000","role":"planner","status":"ACTIVE","actor":"codex"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managedAgentId").value("01HZX0000000000000000000002"));
    }

    @Test
    void listManagedAgentSpecsReturnsEnvelope() throws Exception {
        when(governanceService.listManagedAgentSpecs("01HZX0000000000000000000000"))
                .thenReturn(List.of(sampleManagedAgentSpecView()));

        mockMvc.perform(get("/managed-agent-specs/01HZX0000000000000000000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].role").value("planner"));
    }

    private static OwnerProfileFactView sampleOwnerProfileFactView() {
        return new OwnerProfileFactView(
                "01HZX0000000000000000000000",
                "01HZX0000000000000000000001",
                "preferences",
                "tone",
                "warm",
                Instant.parse("2026-03-21T00:00:00Z")
        );
    }

    private static ManagedAgentSpecView sampleManagedAgentSpecView() {
        return new ManagedAgentSpecView(
                "01HZX0000000000000000000000",
                "01HZX0000000000000000000002",
                "planner",
                "ACTIVE",
                Instant.parse("2026-03-21T00:00:00Z")
        );
    }
}
