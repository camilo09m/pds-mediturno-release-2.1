package cl.losandes.mediturno.tarificacion;

import java.time.LocalDateTime;

/** Hora real del sistema. */
public class RelojSistema implements Reloj {

    @Override
    public LocalDateTime ahora() {
        return LocalDateTime.now();
    }
}
