package cl.losandes.mediturno.reservas.aplicacion;

import java.time.Duration;

/**
 * Marca de mensajes ya procesados, para que un consumidor tolere la entrega
 * duplicada propia del "al menos una vez" de Kafka.
 */
public interface RegistroIdempotencia {

    /**
     * Registra la clave si no estaba registrada.
     *
     * @return {@code true} si es la primera vez que se ve la clave — el llamador debe
     *         procesar el mensaje; {@code false} si ya se había procesado.
     */
    boolean registrarSiEsNuevo(String clave, Duration vigencia);

    void olvidar(String clave);
}
