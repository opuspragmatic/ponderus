package io.pragmatic.ponderus.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.pragmatic.ponderus.domain.Score;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ScoreRequest;
import io.pragmatic.ponderus.repository.ScoreRepository;
import lombok.AllArgsConstructor;

/**
 * Logique métier des scores (croisement critère × option d'un projet).
 * Le scoping par utilisateur est délégué à {@link CriterionService} et
 * {@link OptionService} (qui vérifient à la fois la propriété du projet et
 * l'appartenance de l'enfant au projet) ainsi qu'à {@link ProjectService}
 * pour la lecture d'ensemble. Toute ressource d'autrui est traitée en 404.
 */
@Service
@AllArgsConstructor
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final ProjectService projectService;
    private final CriterionService criterionService;
    private final OptionService optionService;

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
        // Vérifie que critère ET option appartiennent à un projet de l'utilisateur (404 sinon).
        criterionService.findOne(user, projectId, criterionId);
        optionService.findOne(user, projectId, optionId);

        // value peut être null volontairement (« pas encore noté »).
        scoreRepository.upsert(UUID.randomUUID(), criterionId, optionId, request.value(), request.note());

        return scoreRepository.findByCriterionIdAndOptionId(criterionId, optionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Score introuvable juste après upsert : " + criterionId + "/" + optionId));
    }
}
