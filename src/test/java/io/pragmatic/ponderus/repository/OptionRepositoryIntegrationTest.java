package io.pragmatic.ponderus.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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

@Transactional
class OptionRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    private Project persistProject() {
        User user = userRepository.save(User.builder()
                .email("owner-" + UUID.randomUUID() + "@example.com")
                .firebaseUid("uid-" + UUID.randomUUID())
                .build());
        Project project = new Project();
        project.setUser(user);
        project.setName("Comparaison maisons");
        return projectRepository.save(project);
    }

    private Option persistOption(Project project, String name, int position) {
        return optionRepository.save(Option.builder()
                .project(project)
                .name(name)
                .position(position)
                .build());
    }

    @Test
    void findByProjectIdOrderByPositionAsc_ordersByPosition() {
        Project project = persistProject();
        persistOption(project, "Deuxième", 1);
        persistOption(project, "Première", 0);
        persistOption(project, "Troisième", 2);

        List<Option> options = optionRepository.findByProjectIdOrderByPositionAsc(project.getId());

        assertThat(options).extracting(Option::getName)
                .containsExactly("Première", "Deuxième", "Troisième");
    }

    @Test
    void findByIdAndProjectId_isScopedToProject() {
        Project project = persistProject();
        Project other = persistProject();
        Option option = persistOption(project, "Maison A", 0);

        assertThat(optionRepository.findByIdAndProjectId(option.getId(), project.getId()))
                .isPresent();
        assertThat(optionRepository.findByIdAndProjectId(option.getId(), other.getId()))
                .isEmpty();
    }

    @Test
    void deletingOption_cascadesToScores() {
        // On persiste tout via le même EntityManager pour rester dans un seul
        // contexte de persistance (évite les états transient/détaché mélangés).
        User user = User.builder()
                .email("owner-" + UUID.randomUUID() + "@example.com")
                .firebaseUid("uid-" + UUID.randomUUID())
                .build();
        em.persist(user);

        Project project = new Project();
        project.setUser(user);
        project.setName("Comparaison maisons");
        em.persist(project);

        Option option = Option.builder()
                .project(project)
                .name("Maison A")
                .position(0)
                .build();
        em.persist(option);

        Criterion criterion = new Criterion();
        criterion.setProject(project);
        criterion.setLabel("Prix");
        criterion.setWeight(3);
        criterion.setElim(false);
        criterion.setPosition(0);
        em.persist(criterion);

        Score score = new Score();
        score.setCriterion(criterion);
        score.setOption(option);
        score.setValue(new BigDecimal("4.5"));
        em.persist(score);
        em.flush();

        assertThat(countScores()).isEqualTo(1L);

        UUID optionId = option.getId();
        em.clear();

        // Rechargée managée, sans le score en session : la suppression émet un
        // simple DELETE sur options, et la FK scores.option_id (ON DELETE CASCADE)
        // doit faire disparaître le score associé.
        Option managed = optionRepository.findById(optionId).orElseThrow();
        optionRepository.delete(managed);
        em.flush();
        em.clear();

        assertThat(countScores()).isZero();
    }

    private long countScores() {
        Object count = em.createNativeQuery("SELECT count(*) FROM scores").getSingleResult();
        return ((Number) count).longValue();
    }

    // Garde-fou : la requête de scoping ne casse pas si l'id n'existe pas.
    @Test
    void findByIdAndProjectId_returnsEmpty_whenUnknown() {
        Optional<Option> result =
                optionRepository.findByIdAndProjectId(UUID.randomUUID(), UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}
