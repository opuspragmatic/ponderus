package io.pragmatic.ponderus.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import io.pragmatic.ponderus.domain.Option;

public interface OptionRepository extends CrudRepository<Option, UUID> {

    /** Options d'un projet, triées par position puis par id pour un ordre stable. */
    List<Option> findByProjectIdOrderByPositionAscIdAsc(UUID projectId);

    Optional<Option> findByIdAndProjectId(UUID id, UUID projectId);

    /**
     * Position maximale déjà utilisée dans le projet, ou -1 si aucune option.
     * Sert à ajouter une nouvelle option en fin de liste sans collision, même
     * après des suppressions (contrairement à un simple count qui recréerait
     * des positions déjà prises quand la séquence a des trous).
     */
    @Query("select coalesce(max(o.position), -1) from Option o where o.project.id = :projectId")
    int findMaxPositionByProjectId(@Param("projectId") UUID projectId);
}
