package com.openclaw.digitalbeings.interfaces.rest.review;

import com.openclaw.digitalbeings.application.review.CanonicalProjectionView;
import com.openclaw.digitalbeings.application.review.ReviewItemView;
import com.openclaw.digitalbeings.application.review.ReviewService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void draftReviewReturnsEnvelope() throws Exception {
        when(reviewService.draftReview(any())).thenReturn(sampleReviewItemView());

        mockMvc.perform(post("/reviews")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"beingId":"01HZX0000000000000000000000","lane":"canonical","kind":"identity","proposal":"normalize","actor":"codex"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewItemId").value("01HZX0000000000000000000001"));
    }

    @Test
    void rebuildCanonicalProjectionReturnsEnvelope() throws Exception {
        when(reviewService.rebuildCanonicalProjection(any())).thenReturn(sampleProjectionView());

        mockMvc.perform(post("/canonical-projections/rebuild")
                        .contentType(APPLICATION_JSON)
                        .content("{\"beingId\":\"01HZX0000000000000000000000\",\"actor\":\"codex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));
    }

    private static ReviewItemView sampleReviewItemView() {
        return new ReviewItemView(
                "01HZX0000000000000000000000",
                "01HZX0000000000000000000001",
                "canonical",
                "identity",
                "normalize",
                "DRAFT",
                Instant.parse("2026-03-21T00:00:00Z"),
                Instant.parse("2026-03-21T00:00:00Z"),
                "codex"
        );
    }

    private static CanonicalProjectionView sampleProjectionView() {
        return new CanonicalProjectionView(
                "01HZX0000000000000000000000",
                "01HZX0000000000000000000002",
                2L,
                Instant.parse("2026-03-21T00:00:00Z"),
                List.of("01HZX0000000000000000000001"),
                "01HZX0000000000000000000001"
        );
    }
}
