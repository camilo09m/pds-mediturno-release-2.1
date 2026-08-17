package cl.losandes.mediturno.tarificacion;

import java.time.LocalDateTime;

/**
 * Solicitud de cálculo de tarifa para una atención.
 *
 * @param paciente           paciente que se atiende
 * @param especialidad       especialidad de la atención
 * @param fechaHoraAtencion  momento en que se realiza la atención
 */
public record SolicitudTarificacion(Paciente paciente,
                                    Especialidad especialidad,
                                    LocalDateTime fechaHoraAtencion) {
}
