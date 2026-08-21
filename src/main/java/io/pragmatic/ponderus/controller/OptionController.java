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
import io.pragmatic.ponderus.dto.OptionRequest;
import io.pragmatic.ponderus.dto.OptionResponse;
import io.pragmatic.ponderus.service.OptionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/projects/{projectId}/options")
@AllArgsConstructor
public class OptionController {

    private final OptionService optionService;

    @GetMapping
    public List<OptionResponse> list(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId) {

        return optionService.findByProject(user, projectId).stream()
                .map(OptionResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<OptionResponse> create(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId,
            @Valid @RequestBody OptionRequest request) {

        OptionResponse created = OptionResponse.from(optionService.create(user, projectId, request));
        return ResponseEntity
                .created(URI.create("/api/projects/" + projectId + "/options/" + created.id()))
                .body(created);
    }

    @PutMapping("/{optionId}")
    public OptionResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId,
            @PathVariable UUID optionId,
            @Valid @RequestBody OptionRequest request) {

        return OptionResponse.from(optionService.update(user, projectId, optionId, request));
    }

    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID projectId,
            @PathVariable UUID optionId) {

        optionService.delete(user, projectId, optionId);
        return ResponseEntity.noContent().build();
    }
}
