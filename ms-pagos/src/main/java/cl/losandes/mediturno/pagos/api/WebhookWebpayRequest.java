package cl.losandes.mediturno.pagos.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Carga útil del webhook de Webpay.
 *
 * <p>Los nombres de los campos son los de la pasarela, no los nuestros: este record es
 * el borde exacto del contrato con un tercero y se traduce de inmediato al vocabulario
 * interno.</p>
 */
public record WebhookWebpayRequest(
        @JsonProperty("buy_order") @NotBlank String ordenCompra,
        @JsonProperty("session_id") String sesion,
        @JsonProperty("amount") long monto,
        @JsonProperty("status") @NotBlank String estado,
        @JsonProperty("authorization_code") String codigoAutorizacion,
        @JsonProperty("transaction_date") LocalDateTime fechaTransaccion) {
}
