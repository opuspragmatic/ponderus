package io.pragmatic.ponderus.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ScoreRequest;
import io.pragmatic.ponderus.dto.ScoreResponse;
import io.pragmatic.ponderus.service.ScoreService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/projects/{projectId}/scores")
@AllArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    /** Tous les scores du projet, exploitables comme matrice critère × option. */
    @GetMapping
    public List<ScoreResponse> list(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId) {

        return scoreService.findByProject(user, projectId).stream()
                .map(ScoreResponse::from)
                .toList();
    }

    /** Upsert idempotent du score pour le couple (critère, option). */
    @PutMapping("/{criterionId}/{optionId}")
    public ScoreResponse upsert(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId,
            @PathVariable UUID criterionId,
            @PathVariable UUID optionId,
            @Valid @RequestBody ScoreRequest request) {

        return ScoreResponse.from(scoreService.upsert(user, projectId, criterionId, optionId, request));
    }
}
