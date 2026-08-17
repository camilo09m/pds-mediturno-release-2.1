package cl.losandes.mediturno.tarificacion;

import java.time.LocalDateTime;

/** Fuente de la hora. Inyectable desde el Release 2.1. */
public interface Reloj {
    LocalDateTime ahora();
}
