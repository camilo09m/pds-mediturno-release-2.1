package cl.losandes.mediturno.reservas.aplicacion;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Registro de las ofertas de cupo con ventana de confirmación abierta.
 *
 * <p>Release 2.1 — la fuente de verdad de la ventana sigue siendo el campo
 * {@code venceConfirmacionEn} de la reserva. Este registro es una vista de lectura con
 * expiración automática, que permite responder "¿esta reserva tiene una oferta viva?"
 * sin consultar la base. Al ser una segunda copia del mismo dato, puede quedar
 * desalineada de la base: esa divergencia es observable y es materia de prueba.</p>
 */
public interface RegistroOfertas {

    /** Abre la ventana. La entrada expira sola al llegar {@code venceEn}. */
    void abrir(Long idReserva, LocalDateTime venceEn, LocalDateTime ahora);

    boolean vigente(Long idReserva);

    void cerrar(Long idReserva);

    /** Ids con ventana viva según este registro. */
    Set<Long> vigentes();
}
