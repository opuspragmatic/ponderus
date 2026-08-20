package io.pragmatic.ponderus.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ProjectRequest;
import io.pragmatic.ponderus.dto.ProjectResponse;
import io.pragmatic.ponderus.service.ProjectService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@AllArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal User user) {
        return projectService.findAll(user).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return ProjectResponse.from(projectService.findOne(user, id));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProjectRequest request) {

        ProjectResponse created = ProjectResponse.from(projectService.create(user, request));
        return ResponseEntity
                .created(URI.create("/api/projects/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request) {

        return ProjectResponse.from(projectService.update(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        projectService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
