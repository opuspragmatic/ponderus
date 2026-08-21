package io.pragmatic.ponderus.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import io.pragmatic.ponderus.domain.Criterion;

public interface CriterionRepository extends CrudRepository<Criterion, UUID> {

    /** Critères d'un projet, triés par position puis par id pour un ordre stable. */
    List<Criterion> findByProjectIdOrderByPositionAscIdAsc(UUID projectId);

    Optional<Criterion> findByIdAndProjectId(UUID id, UUID projectId);

    /**
     * Position maximale déjà utilisée dans le projet, ou -1 si aucun critère.
     * Permet d'ajouter un critère en fin de liste sans collision, même après
     * des suppressions (contrairement à un simple count).
     */
    @Query("select coalesce(max(c.position), -1) from Criterion c where c.project.id = :projectId")
    int findMaxPositionByProjectId(@Param("projectId") UUID projectId);
}
