package cl.losandes.mediturno.reservas.infraestructura.mensajeria;

import cl.losandes.mediturno.reservas.aplicacion.PublicadorEventos;
import cl.losandes.mediturno.reservas.aplicacion.eventos.ReservaEvento;
import cl.losandes.mediturno.reservas.aplicacion.eventos.SolicitudNotificacion;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Despacha a la mensajería los eventos emitidos durante una transacción ya confirmada. */
@Component
public class PuenteDeEventos {

    private final PublicadorEventos publicador;

    public PuenteDeEventos(PublicadorEventos publicador) {
        this.publicador = publicador;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void despachar(EventoDeIntegracion sobre) {
        if (sobre.carga() instanceof ReservaEvento evento) {
            publicador.publicar(evento);
        } else if (sobre.carga() instanceof SolicitudNotificacion solicitud) {
            publicador.publicar(solicitud);
        }
    }
}
