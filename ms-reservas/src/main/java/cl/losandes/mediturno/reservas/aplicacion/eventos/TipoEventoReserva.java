package cl.losandes.mediturno.reservas.aplicacion.eventos;

/** Tipos de evento que ms-reservas publica en el topic {@code mediturno.reservas.eventos}. */
public enum TipoEventoReserva {
    CREADA,
    CONFIRMADA,
    INICIADA,
    FINALIZADA,
    ANULADA,
    MODIFICADA,
    INASISTENCIA,
    CUPO_OFRECIDO,
    OFERTA_VENCIDA
}
