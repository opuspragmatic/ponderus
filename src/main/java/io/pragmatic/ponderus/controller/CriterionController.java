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
import io.pragmatic.ponderus.dto.CriterionRequest;
import io.pragmatic.ponderus.dto.CriterionResponse;
import io.pragmatic.ponderus.service.CriterionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/projects/{projectId}/criteria")
@AllArgsConstructor
public class CriterionController {

    private final CriterionService criterionService;

    @GetMapping
    public List<CriterionResponse> list(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId) {

        return criterionService.findByProject(user, projectId).stream()
                .map(CriterionResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<CriterionResponse> create(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId,
            @Valid @RequestBody CriterionRequest request) {

        CriterionResponse created = CriterionResponse.from(criterionService.create(user, projectId, request));
        return ResponseEntity
                .created(URI.create("/api/projects/" + projectId + "/criteria/" + created.id()))
                .body(created);
    }

    @PutMapping("/{criterionId}")
    public CriterionResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId,
            @PathVariable UUID criterionId,
            @Valid @RequestBody CriterionRequest request) {

        return CriterionResponse.from(criterionService.update(user, projectId, criterionId, request));
    }

    @DeleteMapping("/{criterionId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId,
            @PathVariable UUID criterionId) {

        criterionService.delete(user, projectId, criterionId);
        return ResponseEntity.noContent().build();
    }
}
