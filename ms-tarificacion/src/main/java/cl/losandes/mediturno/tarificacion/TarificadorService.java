package cl.losandes.mediturno.tarificacion;

import java.time.LocalDate;
import java.time.Period;

/**
 * Cálculo del copago de una atención, según las reglas de negocio RN-TAR-01 a RN-TAR-10
 * de la especificación de MediTurno 2.0.
 *
 * <p>El cálculo sigue esta secuencia:</p>
 * <ol>
 *   <li>Arancel base de la especialidad (RN-TAR-01).</li>
 *   <li>Recargos por urgencia y por horario extendido (RN-TAR-05, RN-TAR-06).</li>
 *   <li>Bonificación previsional sobre el arancel resultante (RN-TAR-02 a RN-TAR-04).</li>
 *   <li>Descuento por adulto mayor o convenio de empresa (RN-TAR-07 a RN-TAR-09).</li>
 *   <li>Copago final, nunca negativo y en pesos enteros (RN-TAR-10).</li>
 * </ol>
 */
public class TarificadorService {

    private static final long TOPE_ISAPRE = 30_000L;
    private static final int PORCENTAJE_ISAPRE = 60;
    private static final int PORCENTAJE_RECARGO_URGENCIA = 20;
    private static final int PORCENTAJE_RECARGO_HORARIO_EXTENDIDO = 15;
    private static final int PORCENTAJE_DESCUENTO_ADULTO_MAYOR = 10;
    private static final int PORCENTAJE_DESCUENTO_CONVENIO = 15;
    private static final int EDAD_ADULTO_MAYOR = 65;
    private static final int HORA_INICIO_HORARIO_EXTENDIDO = 18;

    private final Reloj reloj;
    private final Convenios convenios;

    public TarificadorService(Reloj reloj, Convenios convenios) {
        this.reloj = reloj;
        this.convenios = convenios;
    }

    public DetalleTarifa calcular(SolicitudTarificacion solicitud) {
        Paciente paciente = solicitud.paciente();

        long arancelBase = solicitud.especialidad().arancelBase();

        long recargoUrgencia = esUrgencia(solicitud)
                ? arancelBase * PORCENTAJE_RECARGO_URGENCIA / 100
                : 0L;

        long recargoHorarioExtendido = esHorarioExtendido(solicitud)
                ? arancelBase * PORCENTAJE_RECARGO_HORARIO_EXTENDIDO / 100
                : 0L;

        long arancelBruto = arancelBase + recargoUrgencia + recargoHorarioExtendido;

        long bonificacion = calcularBonificacion(paciente, arancelBruto);

        long copago = arancelBruto - bonificacion;

        // RN-TAR-07 a RN-TAR-09: no son acumulables, se aplica el de mayor beneficio.
        long descuentoAdultoMayor = esAdultoMayor(solicitud)
                ? copago * PORCENTAJE_DESCUENTO_ADULTO_MAYOR / 100
                : 0L;
        long descuentoConvenio = convenios.tieneConvenioVigente(paciente.rut())
                ? copago * PORCENTAJE_DESCUENTO_CONVENIO / 100
                : 0L;

        String descuentoAplicado;
        long montoDescuento;
        if (descuentoConvenio >= descuentoAdultoMayor && descuentoConvenio > 0) {
            descuentoAplicado = "CONVENIO_EMPRESA";
            montoDescuento = descuentoConvenio;
        } else if (descuentoAdultoMayor > 0) {
            descuentoAplicado = "ADULTO_MAYOR";
            montoDescuento = descuentoAdultoMayor;
        } else {
            descuentoAplicado = "NINGUNO";
            montoDescuento = 0L;
        }
        copago -= montoDescuento;

        if (copago < 0) {
            copago = 0;
        }

        return new DetalleTarifa(arancelBase,
                                 recargoUrgencia,
                                 recargoHorarioExtendido,
                                 arancelBruto,
                                 bonificacion,
                                 descuentoAplicado,
                                 montoDescuento,
                                 copago);
    }

    private long calcularBonificacion(Paciente paciente, long arancelBruto) {
        return switch (paciente.prevision()) {
            case FONASA -> arancelBruto * paciente.tramoFonasa().porcentajeBonificacion() / 100;
            case ISAPRE -> Math.min(arancelBruto * PORCENTAJE_ISAPRE / 100, TOPE_ISAPRE);
            case PARTICULAR -> 0L;
        };
    }

    /** RN-TAR-05 — Se considera urgencia la atención que ocurre el mismo día. */
    private boolean esUrgencia(SolicitudTarificacion solicitud) {
        LocalDate hoy = reloj.ahora().toLocalDate();
        return solicitud.fechaHoraAtencion().toLocalDate().isEqual(hoy);
    }

    /** RN-TAR-06 — Horario extendido: después de las 18:00 o el día sábado. */
    private boolean esHorarioExtendido(SolicitudTarificacion solicitud) {
        return solicitud.fechaHoraAtencion().getHour() >= HORA_INICIO_HORARIO_EXTENDIDO
                || solicitud.fechaHoraAtencion().getDayOfWeek() == java.time.DayOfWeek.SATURDAY;
    }

    /** RN-TAR-07 — Adulto mayor: 65 años o más a la fecha de atención. */
    private boolean esAdultoMayor(SolicitudTarificacion solicitud) {
        int edad = Period.between(solicitud.paciente().fechaNacimiento(),
                                  solicitud.fechaHoraAtencion().toLocalDate()).getYears();
        return edad >= EDAD_ADULTO_MAYOR;
    }

}
