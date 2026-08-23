package cl.losandes.mediturno.notificaciones.eventos;

import java.time.LocalDateTime;

/** Solicitud explícita de aviso, publicada por ms-reservas. */
public record SolicitudNotificacion(String solicitudId,
                                    int version,
                                    String rutPaciente,
                                    String canal,
                                    String plantilla,
                                    String cuerpo,
                                    Long idReserva,
                                    LocalDateTime venceEn,
                                    LocalDateTime ocurridoEn) {
}
