package cl.losandes.mediturno.notificaciones.eventos;

import java.time.LocalDateTime;

/**
 * Visión que ms-notificaciones tiene de un evento de reserva.
 *
 * <p>Es una copia deliberada: este servicio solo declara los campos que usa. Si
 * ms-reservas agrega campos nuevos, aquí se ignoran y nada se rompe; si quita o renombra
 * uno de los que están acá, el aviso sale mal y hay que enterarse antes de producción.
 * Eso es lo que verifica el pacto.</p>
 */
public record ReservaEvento(String eventoId,
                            int version,
                            String tipo,
                            Long idReserva,
                            String rutPaciente,
                            String especialidad,
                            LocalDateTime fechaHoraAtencion,
                            String estado,
                            LocalDateTime venceConfirmacionEn,
                            LocalDateTime ocurridoEn) {
}
