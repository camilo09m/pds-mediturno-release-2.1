package cl.losandes.mediturno.notificaciones.proveedor;

/**
 * El proveedor de mensajería devolvió 429: se agotó la cuota de 500 mensajes por hora.
 *
 * <p>No es reintentable dentro de la ventana de reintentos del consumidor: la cuota se
 * repone por hora, no por segundos. El mensaje se deriva a la cola de mensajes muertos
 * para reprocesarlo cuando la cuota vuelva.</p>
 */
public class CuotaAgotadaException extends RuntimeException {

    public CuotaAgotadaException(String mensaje) {
        super(mensaje);
    }
}
