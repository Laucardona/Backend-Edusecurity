package edusecurity.gateway.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/** CORS solo en el gateway y rutas internas bloqueadas (tarea 0.7). */
@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayBordeIntegrationTest {

    /** URI absoluta: CORS compara el Origin con el host de la petición. */
    private static final String GATEWAY = "http://gateway.test";

    @Autowired
    private WebTestClient client;

    @Test
    void unOrigenPermitidoRecibeLasCabecerasCors() {
        client.options().uri(GATEWAY + "/api/modulos")
                .header(HttpHeaders.ORIGIN, "http://localhost:5500")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5500")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    void unOrigenNoPermitidoNoRecibeCabecerasCors() {
        client.options().uri(GATEWAY + "/api/modulos")
                .header(HttpHeaders.ORIGIN, "http://malicioso.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);

        client.get().uri(GATEWAY + "/actuator/health")
                .header(HttpHeaders.ORIGIN, "http://malicioso.example")
                .exchange()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    void lasRutasInternasDevuelven404DesdeFuera() {
        for (String ruta : new String[] {"/internal/x", "/internal/progreso/simulaciones", "/INTERNAL/usuarios/metricas"}) {
            client.get().uri(ruta)
                    .header("X-Service-Key", "clave-interna-solo-pruebas")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody().jsonPath("$.type").isEqualTo("urn:edusecurity:problema:no-encontrado");
        }
    }
}
