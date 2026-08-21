package io.pragmatic.ponderus.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.CriterionRequest;
import io.pragmatic.ponderus.service.CriterionService;

@ExtendWith(MockitoExtension.class)
class CriterionControllerTest {

    @Mock
    private CriterionService criterionService;

    private MockMvc mockMvc;

    private final User user = User.builder()
            .id(UUID.randomUUID())
            .email("owner@example.com")
            .firebaseUid("firebase-owner")
            .build();

    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CriterionController(criterionService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Criterion criterion(String label, int weight, int position) {
        Project project = new Project();
        project.setId(projectId);
        Criterion c = new Criterion();
        c.setId(UUID.randomUUID());
        c.setProject(project);
        c.setLabel(label);
        c.setWeight(weight);
        c.setElim(false);
        c.setPosition(position);
        return c;
    }

    @Test
    void list_returnsCriteria() throws Exception {
        when(criterionService.findByProject(user, projectId))
                .thenReturn(List.of(criterion("Prix", 3, 0)));

        mockMvc.perform(get("/api/projects/" + projectId + "/criteria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Prix"))
                .andExpect(jsonPath("$[0].weight").value(3))
                .andExpect(jsonPath("$[0].projectId").value(projectId.toString()));
    }

    @Test
    void create_returns201_andLocation() throws Exception {
        Criterion created = criterion("Surface", 5, 1);
        when(criterionService.create(eq(user), eq(projectId), any(CriterionRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/projects/" + projectId + "/criteria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Surface\",\"weight\":5}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/projects/" + projectId + "/criteria/" + created.getId()))
                .andExpect(jsonPath("$.label").value("Surface"));
    }

    @Test
    void create_returns400_whenWeightAboveRange() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/criteria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Prix\",\"weight\":6}"))
                .andExpect(status().isBadRequest());

        verify(criterionService, never()).create(any(), any(), any());
    }

    @Test
    void create_returns400_whenWeightBelowRange() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/criteria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Prix\",\"weight\":0}"))
                .andExpect(status().isBadRequest());

        verify(criterionService, never()).create(any(), any(), any());
    }

    @Test
    void create_returns400_whenWeightMissing() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/criteria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Prix\"}"))
                .andExpect(status().isBadRequest());

        verify(criterionService, never()).create(any(), any(), any());
    }

    @Test
    void create_returns400_whenLabelBlank() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/criteria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"  \",\"weight\":3}"))
                .andExpect(status().isBadRequest());

        verify(criterionService, never()).create(any(), any(), any());
    }

    @Test
    void delete_returns204() throws Exception {
        UUID criterionId = UUID.randomUUID();

        mockMvc.perform(delete("/api/projects/" + projectId + "/criteria/" + criterionId))
                .andExpect(status().isNoContent());

        verify(criterionService).delete(user, projectId, criterionId);
    }
}
