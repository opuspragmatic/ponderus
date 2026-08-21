package io.pragmatic.ponderus.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import io.pragmatic.ponderus.AbstractPostgresIntegrationTest;
import io.pragmatic.ponderus.domain.Criterion;
import io.pragmatic.ponderus.domain.Option;
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.Score;
import io.pragmatic.ponderus.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

@Transactional
class ScoreRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ScoreRepository scoreRepository;

    @PersistenceContext
    private EntityManager em;

    private Project project;
    private Criterion criterion;
    private Option option;

    private void seedProjectGraph() {
        User user = User.builder()
                .email("owner-" + UUID.randomUUID() + "@example.com")
                .firebaseUid("uid-" + UUID.randomUUID())
                .build();
        em.persist(user);

        project = new Project();
        project.setUser(user);
        project.setName("Comparaison maisons");
        em.persist(project);

        criterion = Criterion.builder()
                .project(project).label("Prix").weight(3).elim(false).position(0).build();
        em.persist(criterion);

        option = Option.builder()
                .project(project).name("Maison A").position(0).build();
        em.persist(option);
    }

    private Score newScore(Criterion c, Option o, String value) {
        Score score = new Score();
        score.setCriterion(c);
        score.setOption(o);
        score.setValue(value != null ? new BigDecimal(value) : null);
        return score;
    }

    @Test
    void uniqueConstraint_rejectsDuplicateCriterionOptionCouple() {
        seedProjectGraph();
        em.persist(newScore(criterion, option, "4.5"));
        em.persist(newScore(criterion, option, "2.0"));

        // Contrainte UNIQUE (criterion_id, option_id) : le second insert doit échouer.
        assertThatThrownBy(em::flush).isInstanceOf(PersistenceException.class);
    }

    @Test
    void findByCriterionIdAndOptionId_findsExistingScore_forUpsert() {
        seedProjectGraph();
        Score score = newScore(criterion, option, "4.5");
        em.persist(score);
        em.flush();
        em.clear();

        assertThat(scoreRepository.findByCriterionIdAndOptionId(criterion.getId(), option.getId()))
                .isPresent();
    }

    @Test
    void nullValue_isPersistedAsNull_distinctFromZero() {
        seedProjectGraph();
        Score score = newScore(criterion, option, null);
        score.setNote("pas encore visité");
        em.persist(score);
        em.flush();
        em.clear();

        Score reloaded = scoreRepository.findByCriterionIdAndOptionId(criterion.getId(), option.getId())
                .orElseThrow();
        assertThat(reloaded.getValue()).isNull();
        assertThat(reloaded.getNote()).isEqualTo("pas encore visité");
    }

    @Test
    void findByProjectId_returnsProjectScores_orderedByCriterionThenOptionPosition() {
        seedProjectGraph();
        Criterion criterion2 = Criterion.builder()
                .project(project).label("Surface").weight(4).elim(false).position(1).build();
        em.persist(criterion2);
        Option option2 = Option.builder()
                .project(project).name("Maison B").position(1).build();
        em.persist(option2);

        // Insérés dans le désordre ; on attend l'ordre (crit.position, opt.position).
        em.persist(newScore(criterion2, option2, "1.0")); // (1,1)
        em.persist(newScore(criterion, option2, "2.0"));   // (0,1)
        em.persist(newScore(criterion2, option, "3.0"));   // (1,0)
        em.persist(newScore(criterion, option, "4.0"));    // (0,0)
        em.flush();
        em.clear();

        List<Score> scores = scoreRepository.findByProjectId(project.getId());

        assertThat(scores).extracting(Score::getValue)
                .containsExactly(new BigDecimal("4.0"), new BigDecimal("2.0"),
                        new BigDecimal("3.0"), new BigDecimal("1.0"));
    }

    @Test
    void findByProjectId_isScopedToProject() {
        seedProjectGraph();
        em.persist(newScore(criterion, option, "4.5"));
        em.flush();

        User otherUser = User.builder()
                .email("other-" + UUID.randomUUID() + "@example.com")
                .firebaseUid("uid-" + UUID.randomUUID())
                .build();
        em.persist(otherUser);
        Project otherProject = new Project();
        otherProject.setUser(otherUser);
        otherProject.setName("Autre");
        em.persist(otherProject);
        em.flush();

        assertThat(scoreRepository.findByProjectId(project.getId())).hasSize(1);
        assertThat(scoreRepository.findByProjectId(otherProject.getId())).isEmpty();
    }
}
