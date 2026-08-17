package cl.losandes.mediturno.reservas.aplicacion;

import cl.losandes.mediturno.reservas.dominio.Reserva;

/**
 * Avisos al paciente. Release 2.1: la lista de espera notifica automáticamente
 * cuando se libera un cupo.
 */
public interface Notificador {

    /** El cupo quedó disponible y hay una ventana de confirmación abierta. */
    void cupoOfrecido(Reserva reserva);
}
