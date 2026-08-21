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

import io.pragmatic.ponderus.domain.Option;
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.OptionRequest;
import io.pragmatic.ponderus.service.OptionService;
import io.pragmatic.ponderus.web.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class OptionControllerTest {

    @Mock
    private OptionService optionService;

    private MockMvc mockMvc;

    private final User user = User.builder()
            .id(UUID.randomUUID())
            .email("owner@example.com")
            .firebaseUid("firebase-owner")
            .build();

    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OptionController(optionService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Option option(String name, int position) {
        Project project = new Project();
        project.setId(projectId);
        Option o = new Option();
        o.setId(UUID.randomUUID());
        o.setProject(project);
        o.setName(name);
        o.setPosition(position);
        return o;
    }

    @Test
    void list_returnsOptions() throws Exception {
        when(optionService.findByProject(user, projectId))
                .thenReturn(List.of(option("Maison A", 0), option("Maison B", 1)));

        mockMvc.perform(get("/api/projects/" + projectId + "/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maison A"))
                .andExpect(jsonPath("$[0].projectId").value(projectId.toString()))
                .andExpect(jsonPath("$[1].position").value(1));
    }

    @Test
    void create_returns201_andLocation() throws Exception {
        Option created = option("Maison C", 2);
        when(optionService.create(eq(user), eq(projectId), any(OptionRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/projects/" + projectId + "/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Maison C\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/projects/" + projectId + "/options/" + created.getId()))
                .andExpect(jsonPath("$.name").value("Maison C"));
    }

    @Test
    void create_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verify(optionService, never()).create(any(), any(), any());
    }

    @Test
    void delete_returns404_whenProjectNotOwned() throws Exception {
        UUID optionId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Projet introuvable"))
                .when(optionService).delete(user, projectId, optionId);

        mockMvc.perform(delete("/api/projects/" + projectId + "/options/" + optionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        UUID optionId = UUID.randomUUID();

        mockMvc.perform(delete("/api/projects/" + projectId + "/options/" + optionId))
                .andExpect(status().isNoContent());

        verify(optionService).delete(user, projectId, optionId);
    }
}
