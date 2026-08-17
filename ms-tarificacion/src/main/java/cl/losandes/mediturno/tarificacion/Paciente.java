package cl.losandes.mediturno.tarificacion;

import java.time.LocalDate;

/**
 * Datos del paciente necesarios para tarificar.
 *
 * @param rut               RUT con dígito verificador, formato 12.345.678-9
 * @param fechaNacimiento   usada para determinar la edad a la fecha de atención
 * @param prevision         sistema previsional
 * @param tramoFonasa       solo aplica cuando la previsión es FONASA; puede ser null
 */
public record Paciente(String rut,
                       LocalDate fechaNacimiento,
                       TipoPrevision prevision,
                       TramoFonasa tramoFonasa) {
}
