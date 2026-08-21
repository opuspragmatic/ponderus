package io.pragmatic.ponderus;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.pragmatic.ponderus.config.FirebaseConfig;

/**
 * Base des tests d'intégration : démarre un PostgreSQL réel (Testcontainers)
 * et laisse Flyway appliquer les migrations, pour tester le schéma réel
 * (notamment les contraintes ON DELETE CASCADE et UNIQUE).
 *
 * FirebaseConfig est remplacé par un mock pour éviter l'initialisation du
 * SDK Firebase (qui exige des credentials Google indisponibles en test).
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    protected FirebaseConfig firebaseConfig;
}
