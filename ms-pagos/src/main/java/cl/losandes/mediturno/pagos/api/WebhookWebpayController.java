package cl.losandes.mediturno.pagos.api;

import cl.losandes.mediturno.pagos.mensajeria.PublicadorPagos;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Punto de entrada del webhook de Webpay.
 *
 * <p>Responde 200 en todos los casos que no sean un error del propio servicio. Una
 * pasarela que recibe un 4xx reintenta y, si insiste, marca la transacción como no
 * conciliada: devolver un error por una orden que no reconocemos convierte un problema
 * nuestro en un problema del pago del paciente.</p>
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookWebpayController {

    private static final Logger log = LoggerFactory.getLogger(WebhookWebpayController.class);

    private final PublicadorPagos publicador;

    public WebhookWebpayController(PublicadorPagos publicador) {
        this.publicador = publicador;
    }

    @PostMapping("/webpay")
    public ResponseEntity<Map<String, String>> webpay(@Valid @RequestBody WebhookWebpayRequest req) {
        boolean nuevo = publicador.publicar(req);
        if (!nuevo) {
            log.info("Webhook repetido para la orden {}; ya fue publicado.", req.ordenCompra());
            return ResponseEntity.ok(Map.of("resultado", "REPETIDO", "ordenCompra", req.ordenCompra()));
        }
        return ResponseEntity.ok(Map.of("resultado", "RECIBIDO", "ordenCompra", req.ordenCompra()));
    }
}
