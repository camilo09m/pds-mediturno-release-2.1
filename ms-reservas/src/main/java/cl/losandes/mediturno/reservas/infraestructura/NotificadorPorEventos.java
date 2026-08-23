package cl.losandes.mediturno.reservas.infraestructura;

import cl.losandes.mediturno.reservas.aplicacion.Notificador;
import cl.losandes.mediturno.reservas.aplicacion.RegistroOfertas;
import cl.losandes.mediturno.reservas.aplicacion.eventos.ReservaEvento;
import cl.losandes.mediturno.reservas.aplicacion.eventos.SolicitudNotificacion;
import cl.losandes.mediturno.reservas.aplicacion.eventos.TipoEventoReserva;
import cl.losandes.mediturno.reservas.dominio.Reserva;
import cl.losandes.mediturno.reservas.infraestructura.mensajeria.EventoDeIntegracion;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Aviso de cupo ofrecido.
 *
 * <p>Hace tres cosas al liberarse un cupo: registra la ventana en el registro de ofertas,
 * emite el evento de dominio y pide el aviso al paciente. Las tres salen del servicio
 * recién cuando la transacción confirma.</p>
 */
@Component
public class NotificadorPorEventos implements Notificador {

    private final ApplicationEventPublisher local;
    private final RegistroOfertas ofertas;

    public NotificadorPorEventos(ApplicationEventPublisher local, RegistroOfertas ofertas) {
        this.local = local;
        this.ofertas = ofertas;
    }

    @Override
    public void cupoOfrecido(Reserva reserva) {
        LocalDateTime ahora = LocalDateTime.now();
        ofertas.abrir(reserva.id(), reserva.venceConfirmacionEn(), ahora);
        local.publishEvent(new EventoDeIntegracion(
                ReservaEvento.de(TipoEventoReserva.CUPO_OFRECIDO, reserva, ahora)));
        local.publishEvent(new EventoDeIntegracion(SolicitudNotificacion.cupoOfrecido(
                reserva.rutPaciente(), reserva.id(), reserva.venceConfirmacionEn(), ahora)));
    }
}
