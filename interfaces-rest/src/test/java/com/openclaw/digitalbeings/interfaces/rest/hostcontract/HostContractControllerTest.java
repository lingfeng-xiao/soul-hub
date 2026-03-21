package com.openclaw.digitalbeings.interfaces.rest.hostcontract;

import com.openclaw.digitalbeings.application.hostcontract.HostContractService;
import com.openclaw.digitalbeings.application.hostcontract.HostContractView;
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
class HostContractControllerTest {

    @Mock
    private HostContractService hostContractService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HostContractController(hostContractService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void registerHostContractReturnsEnvelope() throws Exception {
        when(hostContractService.registerHostContract(any())).thenReturn(sampleHostContractView());

        mockMvc.perform(post("/host-contracts")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"beingId":"01HZX0000000000000000000000","hostType":"codex","actor":"codex"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contractId").value("01HZX0000000000000000000001"));
    }

    @Test
    void listHostContractsReturnsEnvelope() throws Exception {
        when(hostContractService.listHostContracts("01HZX0000000000000000000000"))
                .thenReturn(List.of(sampleHostContractView()));

        mockMvc.perform(get("/host-contracts/01HZX0000000000000000000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].contractId").value("01HZX0000000000000000000001"));
    }

    private static HostContractView sampleHostContractView() {
        return new HostContractView(
                "01HZX0000000000000000000000",
                "01HZX0000000000000000000001",
                "codex",
                "ACTIVE",
                Instant.parse("2026-03-21T00:00:00Z")
        );
    }
}
