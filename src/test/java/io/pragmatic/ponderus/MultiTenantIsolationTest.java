package io.pragmatic.ponderus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import io.pragmatic.ponderus.domain.Criterion;
import io.pragmatic.ponderus.domain.Option;
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.CriterionRequest;
import io.pragmatic.ponderus.dto.OptionRequest;
import io.pragmatic.ponderus.dto.ProjectRequest;
import io.pragmatic.ponderus.dto.ScoreRequest;
import io.pragmatic.ponderus.repository.UserRepository;
import io.pragmatic.ponderus.service.CriterionService;
import io.pragmatic.ponderus.service.OptionService;
import io.pragmatic.ponderus.service.ProjectService;
import io.pragmatic.ponderus.service.ScoreService;
import io.pragmatic.ponderus.web.ResourceNotFoundException;

/**
 * Test d'isolation multi-tenant (issue #7).
 *
 * <p>Vérifie qu'un utilisateur B ne peut ni lire ni modifier AUCUNE ressource
 * d'un utilisateur A — projet, option, critère, score — sur toutes les
 * opérations (lecture, création, mise à jour, suppression, upsert). Chaque
 * tentative doit être traitée comme « inexistante » ({@link ResourceNotFoundException}
 * → 404), sans révéler l'existence de la ressource d'autrui.
 *
 * <p>Le test cible la couche service, où l'enforcement est centralisé
 * ({@code ProjectService.findOne}) : les controllers ne font aucun filtrage,
 * ils ne font que transmettre le {@code @AuthenticationPrincipal User}.
 */
@Transactional
class MultiTenantIsolationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private OptionService optionService;
    @Autowired
    private CriterionService criterionService;
    @Autowired
    private ScoreService scoreService;

    private User userA;
    private User userB;

    private Project projectA;
    private Option optionA;
    private Criterion criterionA;

    @BeforeEach
    void seed() {
        userA = persistUser("a");
        userB = persistUser("b");

        // Graphe complet appartenant à A, créé via les services (chemin nominal).
        projectA = projectService.create(userA, new ProjectRequest("Projet de A", 50));
        optionA = optionService.create(userA, projectA.getId(), new OptionRequest("Maison A", 0));
        criterionA = criterionService.create(userA, projectA.getId(), new CriterionRequest("Prix", 3, false, 0));
        scoreService.upsert(userA, projectA.getId(), criterionA.getId(), optionA.getId(),
                new ScoreRequest(new BigDecimal("4.5"), "ok"));
    }

    private User persistUser(String tag) {
        return userRepository.save(User.builder()
                .email("user-" + tag + "-" + UUID.randomUUID() + "@example.com")
                .firebaseUid("uid-" + tag + "-" + UUID.randomUUID())
                .build());
    }

    // --- Le propriétaire (A) garde bien l'accès : le 404 est une question de
    //     propriété, pas un échec systématique. -----------------------------

    @Test
    void owner_canAccessOwnResources() {
        assertThatCode(() -> {
            projectService.findOne(userA, projectA.getId());
            assertThat(optionService.findByProject(userA, projectA.getId())).hasSize(1);
            assertThat(criterionService.findByProject(userA, projectA.getId())).hasSize(1);
            assertThat(scoreService.findByProject(userA, projectA.getId())).hasSize(1);
        }).doesNotThrowAnyException();

        assertThat(projectService.findAll(userA))
                .extracting(Project::getId).contains(projectA.getId());
    }

    // --- Isolation : B ne voit rien de A. --------------------------------

    @Test
    void otherUser_listsDoNotLeakOwnersData() {
        assertThat(projectService.findAll(userB)).isEmpty();
    }

    @Test
    void otherUser_cannotAccessProjectOfOwner() {
        UUID projectId = projectA.getId();
        assertNotFound(() -> projectService.findOne(userB, projectId));
        assertNotFound(() -> projectService.update(userB, projectId, new ProjectRequest("hack", 0)));
        assertNotFound(() -> projectService.delete(userB, projectId));
    }

    @Test
    void otherUser_cannotAccessOptionsOfOwner() {
        UUID projectId = projectA.getId();
        UUID optionId = optionA.getId();
        assertNotFound(() -> optionService.findByProject(userB, projectId));
        assertNotFound(() -> optionService.create(userB, projectId, new OptionRequest("hack", 1)));
        assertNotFound(() -> optionService.update(userB, projectId, optionId, new OptionRequest("hack", 1)));
        assertNotFound(() -> optionService.delete(userB, projectId, optionId));
    }

    @Test
    void otherUser_cannotAccessCriteriaOfOwner() {
        UUID projectId = projectA.getId();
        UUID criterionId = criterionA.getId();
        assertNotFound(() -> criterionService.findByProject(userB, projectId));
        assertNotFound(() -> criterionService.create(userB, projectId, new CriterionRequest("hack", 2, false, 1)));
        assertNotFound(() -> criterionService.update(userB, projectId, criterionId, new CriterionRequest("hack", 2, false, 1)));
        assertNotFound(() -> criterionService.delete(userB, projectId, criterionId));
    }

    @Test
    void otherUser_cannotAccessScoresOfOwner() {
        UUID projectId = projectA.getId();
        assertNotFound(() -> scoreService.findByProject(userB, projectId));
        assertNotFound(() -> scoreService.upsert(userB, projectId, criterionA.getId(), optionA.getId(),
                new ScoreRequest(new BigDecimal("1.0"), "hack")));
    }

    private void assertNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(ResourceNotFoundException.class);
    }
}
