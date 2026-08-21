package io.pragmatic.ponderus.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import io.pragmatic.ponderus.domain.Score;

public interface ScoreRepository extends CrudRepository<Score, UUID> {

    /** Score existant pour un couple (critère, option), s'il a déjà été saisi. */
    Optional<Score> findByCriterionIdAndOptionId(UUID criterionId, UUID optionId);

    /**
     * Upsert atomique du score d'un couple (critère, option), délégué à
     * PostgreSQL (INSERT ... ON CONFLICT). Évite la course « lire puis insérer »
     * : deux requêtes concurrentes sur la même cellule ne peuvent plus violer la
     * contrainte UNIQUE(criterion_id, option_id) — la seconde bascule en UPDATE.
     * L'id fourni n'est utilisé qu'à la création ; en cas de conflit la ligne
     * existante (et son id) est conservée.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO scores (id, criterion_id, option_id, value, note)
            VALUES (:id, :criterionId, :optionId, :value, :note)
            ON CONFLICT (criterion_id, option_id)
            DO UPDATE SET value = EXCLUDED.value, note = EXCLUDED.note
            """, nativeQuery = true)
    void upsert(@Param("id") UUID id,
                @Param("criterionId") UUID criterionId,
                @Param("optionId") UUID optionId,
                @Param("value") BigDecimal value,
                @Param("note") String note);

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
