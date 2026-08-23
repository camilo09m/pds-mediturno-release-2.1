package cl.losandes.mediturno.reservas.aplicacion.eventos;

import cl.losandes.mediturno.reservas.dominio.Reserva;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de dominio publicado en {@code mediturno.reservas.eventos}.
 *
 * <p>{@code eventoId} es la clave de deduplicación: el consumidor debe tolerar recibir
 * dos veces el mismo evento sin producir dos efectos. {@code version} identifica la
 * versión del esquema y permite evolucionarlo sin romper a los consumidores antiguos.</p>
 *
 * <p>La clave de partición del mensaje es el id de la reserva, de modo que todos los
 * eventos de una misma reserva llegan en orden a la misma partición. Entre reservas
 * distintas no hay garantía de orden.</p>
 */
public record ReservaEvento(String eventoId,
                            int version,
                            TipoEventoReserva tipo,
                            Long idReserva,
                            String rutPaciente,
                            String especialidad,
                            LocalDateTime fechaHoraAtencion,
                            String estado,
                            long copago,
                            long multa,
                            LocalDateTime venceConfirmacionEn,
                            LocalDateTime ocurridoEn) {

    public static final int VERSION_ACTUAL = 1;

    public static ReservaEvento de(TipoEventoReserva tipo, Reserva r, LocalDateTime ocurridoEn) {
        return new ReservaEvento(UUID.randomUUID().toString(), VERSION_ACTUAL, tipo, r.id(),
                r.rutPaciente(), r.especialidad().name(), r.fechaHoraAtencion(),
                r.estado().name(), r.copago(), r.multa(), r.venceConfirmacionEn(), ocurridoEn);
    }

    /** Clave de partición: garantiza el orden por reserva. */
    public String clave() {
        return idReserva == null ? rutPaciente : String.valueOf(idReserva);
    }
}
