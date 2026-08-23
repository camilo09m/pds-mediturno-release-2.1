package cl.losandes.mediturno.pacientes.dominio;

/** Incumplimiento de una regla de negocio, con código estable para el contrato de la API. */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
