package cl.losandes.mediturno.reservas.infraestructura.clientes;

import cl.losandes.mediturno.reservas.aplicacion.TarificadorPort;
import cl.losandes.mediturno.reservas.dominio.Especialidad;
import cl.losandes.mediturno.reservas.dominio.Paciente;
import cl.losandes.mediturno.reservas.dominio.Tarifa;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

/**
 * Cliente de ms-tarificacion.
 *
 * <p>Release 2.1 — el cálculo del copago deja de ser una llamada en memoria y pasa a ser
 * un salto de red. Con eso aparecen modos de falla que antes no existían: latencia,
 * indisponibilidad y cambio de contrato.</p>
 *
 * <p>No hay respaldo local. Tarificar mal es peor que no tarificar: un copago calculado
 * con reglas desactualizadas se le cobra al paciente y nadie lo nota. Cuando el proveedor
 * no responde, la reserva se rechaza con {@code TAR-NO-DISPONIBLE}.</p>
 */
@Component
@Profile("!efimero")
public class TarificadorHttp implements TarificadorPort {

    private static final Logger log = LoggerFactory.getLogger(TarificadorHttp.class);

    private final RestClient cliente;

    public TarificadorHttp(@Qualifier("clienteTarificacion") RestClient cliente) {
        this.cliente = cliente;
    }

    @Override
    @Retry(name = "tarificacion")
    @CircuitBreaker(name = "tarificacion", fallbackMethod = "sinTarificador")
    public Tarifa tarificar(Paciente paciente, Especialidad especialidad,
                            LocalDateTime fechaHoraAtencion, LocalDateTime ahora) {
        Contratos.SolicitudTarificacion solicitud = new Contratos.SolicitudTarificacion(
                new Contratos.PacienteParaTarificar(paciente.rut(), paciente.fechaNacimiento(),
                        paciente.prevision().name(),
                        paciente.tramoFonasa() == null ? null : paciente.tramoFonasa().name()),
                especialidad.name(), fechaHoraAtencion);

        Contratos.DetalleTarifa detalle = cliente.post()
                .uri("/api/tarificacion/calcular")
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .body(Contratos.DetalleTarifa.class);

        if (detalle == null) {
            throw new ServicioNoDisponibleException("ms-tarificacion",
                    "ms-tarificacion respondió con un cuerpo vacío.", null);
        }
        return new Tarifa(detalle.arancelBruto(), detalle.copago());
    }

    @SuppressWarnings("unused")
    private Tarifa sinTarificador(Paciente paciente, Especialidad especialidad,
                                  LocalDateTime fechaHoraAtencion, LocalDateTime ahora,
                                  Throwable causa) {
        log.error("ms-tarificacion no disponible al tarificar {} para {}: {}",
                especialidad, paciente.rut(), causa.toString());
        throw new ServicioNoDisponibleException("ms-tarificacion",
                "No se puede calcular el copago en este momento.", causa);
    }
}
