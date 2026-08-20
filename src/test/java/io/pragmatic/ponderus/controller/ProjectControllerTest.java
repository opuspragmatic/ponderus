package io.pragmatic.ponderus.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ProjectRequest;
import io.pragmatic.ponderus.service.ProjectService;
import io.pragmatic.ponderus.web.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    private MockMvc mockMvc;

    private final User user = User.builder()
            .id(UUID.randomUUID())
            .email("owner@example.com")
            .firebaseUid("firebase-owner")
            .build();

    @BeforeEach
    void setUp() {
        // standaloneSetup : pas de contexte Spring complet (ni DB ni Firebase) ;
        // on injecte le @AuthenticationPrincipal via le SecurityContext.
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(projectService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Project project(String name) {
        Project p = new Project();
        p.setId(UUID.randomUUID());
        p.setUser(user);
        p.setName(name);
        return p;
    }

    @Test
    void list_returnsUsersProjects() throws Exception {
        when(projectService.findAll(user)).thenReturn(List.of(project("Maisons")));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maisons"));
    }

    @Test
    void create_returns201_andLocation() throws Exception {
        Project created = project("Maisons");
        when(projectService.create(eq(user), any(ProjectRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Maisons\",\"elimThreshold\":60}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/projects/" + created.getId()))
                .andExpect(jsonPath("$.name").value("Maisons"));
    }

    @Test
    void create_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verify(projectService, org.mockito.Mockito.never())
                .create(any(), any());
    }

    @Test
    void get_returns404_whenNotOwned() throws Exception {
        UUID id = UUID.randomUUID();
        when(projectService.findOne(user, id))
                .thenThrow(new ResourceNotFoundException("Projet introuvable : " + id));

        mockMvc.perform(get("/api/projects/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/projects/" + id))
                .andExpect(status().isNoContent());

        verify(projectService).delete(user, id);
    }
}
