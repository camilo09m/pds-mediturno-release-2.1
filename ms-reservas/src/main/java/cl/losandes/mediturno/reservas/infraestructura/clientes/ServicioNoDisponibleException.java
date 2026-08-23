package cl.losandes.mediturno.reservas.infraestructura.clientes;

/** Un servicio del que depende ms-reservas no respondió a tiempo o respondió con error. */
public class ServicioNoDisponibleException extends RuntimeException {

    private final String servicio;

    public ServicioNoDisponibleException(String servicio, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.servicio = servicio;
    }

    public String servicio() {
        return servicio;
    }
}
