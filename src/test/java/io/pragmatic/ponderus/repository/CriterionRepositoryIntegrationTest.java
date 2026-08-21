package io.pragmatic.ponderus.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.pragmatic.ponderus.dto.CriterionResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Transactional
class CriterionRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CriterionRepository criterionRepository;

    @PersistenceContext
    private EntityManager em;

    private Project persistProject() {
        User user = User.builder()
                .email("owner-" + UUID.randomUUID() + "@example.com")
                .firebaseUid("uid-" + UUID.randomUUID())
                .build();
        em.persist(user);
        Project project = new Project();
        project.setUser(user);
        project.setName("Comparaison maisons");
        em.persist(project);
        return project;
    }

    private Criterion persistCriterion(Project project, String label, int weight, int position) {
        Criterion criterion = Criterion.builder()
                .project(project)
                .label(label)
                .weight(weight)
                .elim(false)
                .position(position)
                .build();
        em.persist(criterion);
        return criterion;
    }

    @Test
    void findByProjectIdOrderByPositionAscIdAsc_ordersByPosition() {
        Project project = persistProject();
        persistCriterion(project, "Deuxième", 2, 1);
        persistCriterion(project, "Première", 3, 0);
        persistCriterion(project, "Troisième", 4, 2);
        em.flush();

        List<Criterion> criteria = criterionRepository.findByProjectIdOrderByPositionAscIdAsc(project.getId());

        assertThat(criteria).extracting(Criterion::getLabel)
                .containsExactly("Première", "Deuxième", "Troisième");
    }

    @Test
    void findMaxPositionByProjectId_returnsMax_orMinusOneWhenEmpty() {
        Project empty = persistProject();
        em.flush();
        assertThat(criterionRepository.findMaxPositionByProjectId(empty.getId())).isEqualTo(-1);

        Project project = persistProject();
        persistCriterion(project, "A", 1, 0);
        persistCriterion(project, "C", 2, 2);
        em.flush();
        assertThat(criterionRepository.findMaxPositionByProjectId(project.getId())).isEqualTo(2);
    }

    @Test
    void findByIdAndProjectId_isScopedToProject() {
        Project project = persistProject();
        Project other = persistProject();
        Criterion criterion = persistCriterion(project, "Prix", 3, 0);
        em.flush();

        assertThat(criterionRepository.findByIdAndProjectId(criterion.getId(), project.getId()))
                .isPresent();
        assertThat(criterionRepository.findByIdAndProjectId(criterion.getId(), other.getId()))
                .isEmpty();
    }

    @Test
    void deletingCriterion_cascadesToScores() {
        Project project = persistProject();
        Criterion criterion = persistCriterion(project, "Prix", 3, 0);

        Option option = Option.builder()
                .project(project)
                .name("Maison A")
                .position(0)
                .build();
        em.persist(option);

        Score score = new Score();
        score.setCriterion(criterion);
        score.setOption(option);
        score.setValue(new BigDecimal("4.5"));
        em.persist(score);
        em.flush();

        assertThat(countScores()).isEqualTo(1L);

        UUID criterionId = criterion.getId();
        em.clear();

        // FK scores.criterion_id est ON DELETE CASCADE : le score doit disparaître.
        Criterion managed = criterionRepository.findById(criterionId).orElseThrow();
        criterionRepository.delete(managed);
        em.flush();
        em.clear();

        assertThat(countScores()).isZero();
    }

    @Test
    void criterionResponse_mapsProjectId_evenWhenProjectProxyDetached() {
        // Reproduit le mapping fait dans le controller (hors transaction de service,
        // open-in-view=false) : l'entité est détachée et son projet est un proxy LAZY.
        Project project = persistProject();
        Criterion criterion = persistCriterion(project, "Prix", 3, 0);
        UUID criterionId = criterion.getId();
        em.flush();
        em.clear();

        Criterion reloaded = criterionRepository.findById(criterionId).orElseThrow();
        em.clear(); // détache : plus aucune session ne couvre le proxy project

        CriterionResponse response = CriterionResponse.from(reloaded);
        assertThat(response.projectId()).isEqualTo(project.getId());
        assertThat(response.label()).isEqualTo("Prix");
        assertThat(response.weight()).isEqualTo(3);
    }

    private long countScores() {
        Object count = em.createNativeQuery("SELECT count(*) FROM scores").getSingleResult();
        return ((Number) count).longValue();
    }
}
