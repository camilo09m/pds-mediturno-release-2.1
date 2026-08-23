package cl.losandes.mediturno.pagos.eventos;

import java.time.LocalDateTime;

/**
 * Confirmación de pago que este servicio publica en
 * {@code mediturno.pagos.confirmaciones}.
 *
 * <p>{@code confirmacionId} es la clave de deduplicación del consumidor.</p>
 */
public record ConfirmacionPago(String confirmacionId,
                               int version,
                               Long idReserva,
                               String ordenCompra,
                               String estado,
                               long monto,
                               LocalDateTime ocurridoEn) {

    public static final int VERSION_ACTUAL = 1;
}
