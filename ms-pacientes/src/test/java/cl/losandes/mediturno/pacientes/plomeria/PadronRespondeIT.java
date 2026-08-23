package cl.losandes.mediturno.pacientes.plomeria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plomería de ms-pacientes: la base responde, la carga inicial corrió y el contrato que
 * consume ms-reservas devuelve lo que promete.
 *
 * <p>No verifica reglas de negocio. Verifica que el servicio esté en pie y que sus dos
 * respuestas —encontrado y no encontrado— tengan la forma acordada, porque son las que
 * el pacto va a fijar.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Testcontainers
class PadronRespondeIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("mediturno_pacientes")
                    .withUsername("mediturno")
                    .withPassword("mediturno");

    @Autowired TestRestTemplate cliente;

    @Test
    @DisplayName("el padrón de ejemplo quedó cargado")
    void padronCargado() {
        ResponseEntity<Object[]> respuesta = cliente.getForEntity("/api/pacientes", Object[].class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("un paciente existente vuelve con todos los campos del contrato")
    void pacienteExistente() {
        ResponseEntity<String> respuesta =
                cliente.getForEntity("/api/pacientes/{rut}", String.class, "5.555.555-5");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody())
                .contains("\"rut\"")
                .contains("\"nombre\"")
                .contains("\"fechaNacimiento\"")
                .contains("\"prevision\"")
                .contains("\"convenioEmpresa\"");
    }

    @Test
    @DisplayName("un paciente inexistente vuelve 404 con el código PAC-404")
    void pacienteInexistente() {
        ResponseEntity<String> respuesta =
                cliente.getForEntity("/api/pacientes/{rut}", String.class, "1.111.111-1");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).contains("PAC-404");
    }
}
