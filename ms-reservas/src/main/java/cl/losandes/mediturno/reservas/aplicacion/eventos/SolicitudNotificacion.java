package cl.losandes.mediturno.reservas.aplicacion.eventos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Solicitud de aviso al paciente, publicada en
 * {@code mediturno.notificaciones.solicitudes}.
 *
 * @param solicitudId clave de deduplicación
 * @param plantilla   identificador de la plantilla de mensaje
 * @param cuerpo      texto ya resuelto que se envía al proveedor de mensajería
 */
public record SolicitudNotificacion(String solicitudId,
                                    int version,
                                    String rutPaciente,
                                    String canal,
                                    String plantilla,
                                    String cuerpo,
                                    Long idReserva,
                                    LocalDateTime venceEn,
                                    LocalDateTime ocurridoEn) {

    public static final int VERSION_ACTUAL = 1;

    public static SolicitudNotificacion cupoOfrecido(String rut, Long idReserva,
                                                     LocalDateTime venceEn, LocalDateTime ahora) {
        return new SolicitudNotificacion(UUID.randomUUID().toString(), VERSION_ACTUAL, rut, "SMS",
                "CUPO_OFRECIDO",
                "Se liberó un cupo para su hora médica. Tiene hasta las "
                        + venceEn.toLocalTime().withNano(0) + " para confirmarla.",
                idReserva, venceEn, ahora);
    }
}
