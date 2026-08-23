package cl.losandes.mediturno.pacientes.api;

import cl.losandes.mediturno.pacientes.dominio.Paciente;

import java.time.LocalDate;

/**
 * Representación pública de un paciente.
 *
 * <p>Este record es el contrato con ms-reservas. Cambiar un nombre de campo, quitar
 * uno o cambiar su tipo rompe al consumidor: por eso está cubierto por Pact.</p>
 */
public record PacienteResponse(String rut,
                               String nombre,
                               LocalDate fechaNacimiento,
                               String prevision,
                               String tramoFonasa,
                               boolean convenioEmpresa) {

    public static PacienteResponse de(Paciente p) {
        return new PacienteResponse(p.rut(), p.nombre(), p.fechaNacimiento(),
                p.prevision().name(),
                p.tramoFonasa() == null ? null : p.tramoFonasa().name(),
                p.convenioEmpresa());
    }
}
