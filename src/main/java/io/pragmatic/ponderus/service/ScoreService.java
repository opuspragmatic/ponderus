package io.pragmatic.ponderus.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.pragmatic.ponderus.domain.Score;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ScoreRequest;
import io.pragmatic.ponderus.repository.CriterionRepository;
import io.pragmatic.ponderus.repository.OptionRepository;
import io.pragmatic.ponderus.repository.ScoreRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;
import lombok.AllArgsConstructor;

/**
 * Logique métier des scores (croisement critère × option d'un projet).
 * Scoping par utilisateur délégué à {@link ProjectService}. Le critère ET
 * l'option doivent appartenir au projet ciblé, sinon 404.
 */
@Service
@AllArgsConstructor
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final CriterionRepository criterionRepository;
    private final OptionRepository optionRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<Score> findByProject(User user, UUID projectId) {
        projectService.findOne(user, projectId); // 404 si le projet n'est pas à l'utilisateur
        return scoreRepository.findByProjectId(projectId);
    }

    /**
     * Upsert du score d'un couple (critère, option) : met à jour la ligne
     * existante si elle existe, sinon la crée. Idempotent et sûr en cas de
     * requêtes concurrentes (upsert atomique côté PostgreSQL).
     */
    @Transactional
    public Score upsert(User user, UUID projectId, UUID criterionId, UUID optionId, ScoreRequest request) {
        projectService.findOne(user, projectId);
        // Vérifie que critère ET option appartiennent bien au projet (404 sinon).
        requireCriterion(projectId, criterionId);
        requireOption(projectId, optionId);

        // value peut être null volontairement (« pas encore noté »).
        scoreRepository.upsert(UUID.randomUUID(), criterionId, optionId, request.value(), request.note());

        return scoreRepository.findByCriterionIdAndOptionId(criterionId, optionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Score introuvable juste après upsert : " + criterionId + "/" + optionId));
    }

    private void requireCriterion(UUID projectId, UUID criterionId) {
        criterionRepository.findByIdAndProjectId(criterionId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Critère introuvable : " + criterionId));
    }

    private void requireOption(UUID projectId, UUID optionId) {
        optionRepository.findByIdAndProjectId(optionId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Option introuvable : " + optionId));
    }
}
