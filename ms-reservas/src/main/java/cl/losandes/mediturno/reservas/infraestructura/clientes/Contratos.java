package cl.losandes.mediturno.reservas.infraestructura.clientes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representación que ms-reservas espera de sus proveedores.
 *
 * <p>Estos tipos son la visión del consumidor: no se importan desde el módulo del
 * proveedor a propósito. Compartir la clase escondería justamente el problema que la
 * Entrega 3 quiere hacer visible — que el proveedor puede cambiar su carga útil sin que
 * el consumidor se entere hasta el ambiente integrado. Es lo que los pactos verifican.</p>
 */
public final class Contratos {

    private Contratos() {
    }

    /** Lo que ms-reservas envía a ms-tarificacion. */
    public record PacienteParaTarificar(String rut,
                                        LocalDate fechaNacimiento,
                                        String prevision,
                                        String tramoFonasa) {
    }

    public record SolicitudTarificacion(PacienteParaTarificar paciente,
                                        String especialidad,
                                        LocalDateTime fechaHoraAtencion) {
    }

    /** Lo que ms-tarificacion devuelve. */
    public record DetalleTarifa(long arancelBase,
                                long recargoUrgencia,
                                long recargoHorarioExtendido,
                                long arancelBruto,
                                long bonificacion,
                                String descuentoAplicado,
                                long montoDescuento,
                                long copago) {
    }

    /** Lo que ms-pacientes devuelve. */
    public record PacienteDelPadron(String rut,
                                    String nombre,
                                    LocalDate fechaNacimiento,
                                    String prevision,
                                    String tramoFonasa,
                                    boolean convenioEmpresa) {
    }

    /** Lo que devuelve el Validador de Previsión, que es un servicio externo. */
    public record ValidacionPrevision(String rut,
                                      boolean afiliado,
                                      String prevision,
                                      String tramo,
                                      LocalDate vigenteHasta) {
    }
}
