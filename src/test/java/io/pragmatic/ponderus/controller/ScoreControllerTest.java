package io.pragmatic.ponderus.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.pragmatic.ponderus.domain.Criterion;
import io.pragmatic.ponderus.domain.Option;
import io.pragmatic.ponderus.domain.Score;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ScoreRequest;
import io.pragmatic.ponderus.service.ScoreService;

@ExtendWith(MockitoExtension.class)
class ScoreControllerTest {

    @Mock
    private ScoreService scoreService;

    private MockMvc mockMvc;

    private final User user = User.builder()
            .id(UUID.randomUUID())
            .email("owner@example.com")
            .firebaseUid("firebase-owner")
            .build();

    private final UUID projectId = UUID.randomUUID();
    private final UUID criterionId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ScoreController(scoreService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Score score(String value) {
        Criterion criterion = new Criterion();
        criterion.setId(criterionId);
        Option option = new Option();
        option.setId(optionId);
        Score s = new Score();
        s.setId(UUID.randomUUID());
        s.setCriterion(criterion);
        s.setOption(option);
        s.setValue(value != null ? new BigDecimal(value) : null);
        return s;
    }

    @Test
    void list_returnsScores() throws Exception {
        when(scoreService.findByProject(user, projectId)).thenReturn(List.of(score("4.5")));

        mockMvc.perform(get("/api/projects/" + projectId + "/scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].criterionId").value(criterionId.toString()))
                .andExpect(jsonPath("$[0].optionId").value(optionId.toString()))
                .andExpect(jsonPath("$[0].value").value(4.5));
    }

    @Test
    void upsert_returns200_withBody() throws Exception {
        when(scoreService.upsert(eq(user), eq(projectId), eq(criterionId), eq(optionId), any(ScoreRequest.class)))
                .thenReturn(score("3.5"));

        mockMvc.perform(put("/api/projects/" + projectId + "/scores/" + criterionId + "/" + optionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":3.5,\"note\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(3.5));
    }

    @Test
    void upsert_acceptsNullValue() throws Exception {
        when(scoreService.upsert(eq(user), eq(projectId), eq(criterionId), eq(optionId), any(ScoreRequest.class)))
                .thenReturn(score(null));

        mockMvc.perform(put("/api/projects/" + projectId + "/scores/" + criterionId + "/" + optionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").doesNotExist());
    }

    @Test
    void upsert_returns400_whenValueHasTooManyDigits() throws Exception {
        // NUMERIC(3,1) : 100.0 dépasse (3 chiffres avant la virgule).
        mockMvc.perform(put("/api/projects/" + projectId + "/scores/" + criterionId + "/" + optionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":100.0}"))
                .andExpect(status().isBadRequest());

        verify(scoreService, never()).upsert(any(), any(), any(), any(), any());
    }
}
