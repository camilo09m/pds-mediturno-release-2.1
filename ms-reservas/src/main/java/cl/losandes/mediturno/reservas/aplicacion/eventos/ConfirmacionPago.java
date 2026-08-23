package cl.losandes.mediturno.reservas.aplicacion.eventos;

import java.time.LocalDateTime;

/**
 * Confirmación de pago que ms-pagos publica en {@code mediturno.pagos.confirmaciones}
 * a partir del webhook de Webpay.
 *
 * <p>El webhook de Webpay puede llegar duplicado, y puede llegar antes o después de
 * que el comercio confirme. El consumidor de este evento debe ser idempotente.</p>
 */
public record ConfirmacionPago(String confirmacionId,
                               int version,
                               Long idReserva,
                               String ordenCompra,
                               String estado,
                               long monto,
                               LocalDateTime ocurridoEn) {

    public static final int VERSION_ACTUAL = 1;

    public boolean autorizado() {
        return "AUTORIZADO".equalsIgnoreCase(estado);
    }
}
