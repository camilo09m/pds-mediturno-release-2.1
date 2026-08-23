package cl.losandes.mediturno.notificaciones.proveedor;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Cliente del proveedor de mensajería.
 *
 * <p>Cuota de 500 SMS por hora; al superarla responde 429. Ese caso se distingue de una
 * caída porque el tratamiento es distinto: ante una caída se reintenta, ante cuota
 * agotada se deja el mensaje en la cola de mensajes muertos y se reprocesa más tarde.</p>
 */
@Component
public class ProveedorMensajeriaHttp implements ProveedorMensajeria {

    private static final Logger log = LoggerFactory.getLogger(ProveedorMensajeriaHttp.class);

    private final RestClient cliente;

    public ProveedorMensajeriaHttp(
            @Value("${mediturno.clientes.mensajeria.url}") String url,
            @Value("${mediturno.clientes.mensajeria.timeout-conexion-ms:1000}") int conexion,
            @Value("${mediturno.clientes.mensajeria.timeout-lectura-ms:3000}") int lectura) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(conexion);
        fabrica.setReadTimeout(lectura);
        this.cliente = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    @Override
    @Retry(name = "mensajeria")
    @CircuitBreaker(name = "mensajeria")
    public void enviar(String canal, String destinatario, String cuerpo) {
        try {
            cliente.post()
                    .uri("/mensajes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("canal", canal, "destinatario", destinatario, "cuerpo", cuerpo))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Cuota de mensajería agotada al avisar a {}.", destinatario);
            throw new CuotaAgotadaException(
                    "El proveedor de mensajería agotó su cuota horaria de 500 envíos.");
        }
    }
}
