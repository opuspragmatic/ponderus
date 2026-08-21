package io.pragmatic.ponderus.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import io.pragmatic.ponderus.domain.Score;

public interface ScoreRepository extends CrudRepository<Score, UUID> {

    /** Score existant pour un couple (critère, option), s'il a déjà été saisi. */
    Optional<Score> findByCriterionIdAndOptionId(UUID criterionId, UUID optionId);

    /**
     * Tous les scores d'un projet (via le critère), triés par position de
     * critère puis d'option pour un ordre stable exploitable par le frontend.
     */
    @Query("""
            select s from Score s
            where s.criterion.project.id = :projectId
            order by s.criterion.position, s.option.position
            """)
    List<Score> findByProjectId(@Param("projectId") UUID projectId);
}
