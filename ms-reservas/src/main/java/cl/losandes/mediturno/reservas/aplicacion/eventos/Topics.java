package cl.losandes.mediturno.reservas.aplicacion.eventos;

/**
 * Topics del sistema.
 *
 * <p>Cada topic tiene su cola de mensajes muertos, con el mismo nombre y el sufijo
 * {@code .dlq}. Un mensaje llega ahí cuando el consumidor no logra procesarlo tras
 * agotar los reintentos, o cuando la carga útil no se puede interpretar.</p>
 */
public final class Topics {

    /** Publica ms-reservas. Consumen ms-notificaciones y ms-pagos. */
    public static final String RESERVAS_EVENTOS = "mediturno.reservas.eventos";

    /** Publica ms-reservas. Consume ms-notificaciones. */
    public static final String NOTIFICACIONES_SOLICITUDES = "mediturno.notificaciones.solicitudes";

    /** Publica ms-pagos a partir del webhook de Webpay. Consume ms-reservas. */
    public static final String PAGOS_CONFIRMACIONES = "mediturno.pagos.confirmaciones";

    public static final String SUFIJO_DLQ = ".dlq";

    private Topics() {
    }

    public static String dlqDe(String topic) {
        return topic + SUFIJO_DLQ;
    }
}
