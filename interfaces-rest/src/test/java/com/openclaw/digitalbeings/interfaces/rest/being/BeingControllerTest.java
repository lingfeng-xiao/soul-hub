package com.openclaw.digitalbeings.interfaces.rest.being;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
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
class BeingControllerTest {

    @Mock
    private BeingService beingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BeingController(beingService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createBeingReturnsEnvelope() throws Exception {
        when(beingService.createBeing(any())).thenReturn(sampleBeingView());

        mockMvc.perform(post("/beings")
                        .contentType(APPLICATION_JSON)
                        .content("{\"displayName\":\"guan-guan\",\"actor\":\"codex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.beingId").value("01HZX0000000000000000000000"));
    }

    @Test
    void listBeingsReturnsEnvelope() throws Exception {
        when(beingService.listBeings()).thenReturn(List.of(sampleBeingView()));

        mockMvc.perform(get("/beings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].beingId").value("01HZX0000000000000000000000"));
    }

    private static BeingView sampleBeingView() {
        return new BeingView(
                "01HZX0000000000000000000000",
                "guan-guan",
                1L,
                Instant.parse("2026-03-21T00:00:00Z"),
                0,
                0,
                0,
                0,
                0,
                0,
                null
        );
    }
}
