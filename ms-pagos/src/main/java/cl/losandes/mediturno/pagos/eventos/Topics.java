package cl.losandes.mediturno.pagos.eventos;

public final class Topics {

    public static final String RESERVAS_EVENTOS = "mediturno.reservas.eventos";
    public static final String PAGOS_CONFIRMACIONES = "mediturno.pagos.confirmaciones";
    public static final String SUFIJO_DLQ = ".dlq";

    private Topics() {
    }

    public static String dlqDe(String topic) {
        return topic + SUFIJO_DLQ;
    }
}
