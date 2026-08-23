package cl.losandes.mediturno.reservas.infraestructura.clientes;

import cl.losandes.mediturno.reservas.aplicacion.RepositorioPacientes;
import cl.losandes.mediturno.reservas.dominio.Paciente;
import cl.losandes.mediturno.reservas.dominio.TipoPrevision;
import cl.losandes.mediturno.reservas.dominio.TramoFonasa;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Cliente de ms-pacientes.
 *
 * <p>Release 2.1 — el padrón salió de ms-reservas y ahora se consulta por HTTP. Un 404
 * significa "no está en el padrón" y es una respuesta legítima del negocio, no una falla:
 * por eso no cuenta para el circuit breaker.</p>
 */
@Component
@Profile("!efimero")
public class RepositorioPacientesHttp implements RepositorioPacientes {

    private static final Logger log = LoggerFactory.getLogger(RepositorioPacientesHttp.class);

    private final RestClient cliente;

    public RepositorioPacientesHttp(@Qualifier("clientePacientes") RestClient cliente) {
        this.cliente = cliente;
    }

    @Override
    @Retry(name = "pacientes")
    @CircuitBreaker(name = "pacientes", fallbackMethod = "sinPadron")
    public Optional<Paciente> porRut(String rut) {
        try {
            Contratos.PacienteDelPadron dto = cliente.get()
                    .uri("/api/pacientes/{rut}", rut)
                    .retrieve()
                    .body(Contratos.PacienteDelPadron.class);
            return Optional.ofNullable(dto).map(RepositorioPacientesHttp::aDominio);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public Paciente guardar(Paciente paciente) {
        cliente.post()
                .uri("/api/pacientes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(aDto(paciente))
                .retrieve()
                .toBodilessEntity();
        return paciente;
    }

    @Override
    public List<Paciente> todos() {
        Contratos.PacienteDelPadron[] dtos = cliente.get()
                .uri("/api/pacientes")
                .retrieve()
                .body(Contratos.PacienteDelPadron[].class);
        if (dtos == null) {
            return List.of();
        }
        return Arrays.stream(dtos).map(RepositorioPacientesHttp::aDominio).toList();
    }

    @SuppressWarnings("unused")
    private Optional<Paciente> sinPadron(String rut, Throwable causa) {
        log.error("ms-pacientes no disponible al consultar {}: {}", rut, causa.toString());
        throw new ServicioNoDisponibleException("ms-pacientes",
                "No se puede consultar el padrón de pacientes en este momento.", causa);
    }

    private static Paciente aDominio(Contratos.PacienteDelPadron d) {
        return new Paciente(d.rut(), d.nombre(), d.fechaNacimiento(),
                TipoPrevision.valueOf(d.prevision()),
                d.tramoFonasa() == null || d.tramoFonasa().isBlank()
                        ? null : TramoFonasa.valueOf(d.tramoFonasa()),
                d.convenioEmpresa());
    }

    private static Contratos.PacienteDelPadron aDto(Paciente p) {
        return new Contratos.PacienteDelPadron(p.rut(), p.nombre(), p.fechaNacimiento(),
                p.prevision().name(),
                p.tramoFonasa() == null ? null : p.tramoFonasa().name(),
                p.convenioEmpresa());
    }
}
