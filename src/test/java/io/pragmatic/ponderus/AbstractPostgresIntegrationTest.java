package io.pragmatic.ponderus;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

import io.pragmatic.ponderus.config.FirebaseConfig;

/**
 * Base des tests d'intégration : démarre un PostgreSQL réel (Testcontainers)
 * et laisse Flyway appliquer les migrations, pour tester le schéma réel
 * (notamment les contraintes ON DELETE CASCADE et UNIQUE).
 *
 * <p>Le conteneur suit le motif « singleton » : démarré une seule fois au
 * chargement de la classe et jamais arrêté explicitement (Ryuk le nettoie à la
 * fin de la JVM). C'est indispensable car le contexte Spring est mis en cache
 * et partagé entre toutes les classes de test ayant cette configuration : un
 * conteneur géré par {@code @Testcontainers} serait arrêté après la première
 * classe alors que le contexte (et sa datasource) reste réutilisé par les
 * suivantes, provoquant des « Could not open JPA EntityManager ».
 *
 * <p>FirebaseConfig est remplacé par un mock pour éviter l'initialisation du
 * SDK Firebase (qui exige des credentials Google indisponibles en test).
 */
@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    protected FirebaseConfig firebaseConfig;
}
