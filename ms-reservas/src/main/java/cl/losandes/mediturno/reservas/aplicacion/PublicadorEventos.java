package cl.losandes.mediturno.reservas.aplicacion;

import cl.losandes.mediturno.reservas.aplicacion.eventos.ReservaEvento;
import cl.losandes.mediturno.reservas.aplicacion.eventos.SolicitudNotificacion;

/**
 * Salida del servicio hacia la mensajería asíncrona.
 *
 * <p>La implementación productiva escribe en Kafka. El perfil {@code efimero} usa una
 * implementación que no hace nada, para que ms-reservas se pueda levantar y probar sin
 * broker.</p>
 */
public interface PublicadorEventos {

    void publicar(ReservaEvento evento);

    void publicar(SolicitudNotificacion solicitud);
}
