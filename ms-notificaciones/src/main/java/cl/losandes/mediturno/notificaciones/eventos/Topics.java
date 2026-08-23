package cl.losandes.mediturno.notificaciones.eventos;

/** Topics que este servicio consume, con sus colas de mensajes muertos. */
public final class Topics {

    public static final String RESERVAS_EVENTOS = "mediturno.reservas.eventos";
    public static final String NOTIFICACIONES_SOLICITUDES = "mediturno.notificaciones.solicitudes";
    public static final String SUFIJO_DLQ = ".dlq";

    private Topics() {
    }

    public static String dlqDe(String topic) {
        return topic + SUFIJO_DLQ;
    }
}
