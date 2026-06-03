package upc.com.pe.backendplannia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test del contexto completo. Arranca TODO Spring → necesita una BD real y los secretos
 * (JWT, datasource). Por eso está protegido por @EnabledIfEnvironmentVariable: NO corre en la suite
 * normal ni en CI (donde no hay Postgres), evitando romper el build/deploy.
 *
 * Para ejecutarlo localmente con la BD levantada: FULL_CONTEXT_TEST=true ./mvnw test -Dtest=BackendPlanniaApplicationTests
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FULL_CONTEXT_TEST", matches = "true")
class BackendPlanniaApplicationTests {

    @Test
    void contextLoads() {
    }

}
