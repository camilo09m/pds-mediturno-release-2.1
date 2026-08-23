package cl.losandes.mediturno.reservas.infraestructura.mensajeria;

/**
 * Envoltorio de un evento que debe salir del servicio.
 *
 * <p>Se emite como evento de Spring dentro de la transacción y se despacha a Kafka
 * recién cuando la transacción confirma. Así se evita el caso en que se publica un
 * "reserva creada" y la transacción termina revirtiéndose: el mundo exterior se enteraría
 * de algo que no ocurrió, y no habría forma de retirarlo.</p>
 *
 * <p>Lo que esto no resuelve es el caso inverso: si la transacción confirma y Kafka no
 * está disponible en ese instante, el evento se pierde. Resolverlo requiere una bandeja
 * de salida transaccional, que este release no implementa.</p>
 */
public record EventoDeIntegracion(Object carga) {
}
