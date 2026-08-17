package cl.losandes.mediturno.tarificacion;

/**
 * Resultado del cálculo de tarificación, con el desglose completo.
 *
 * @param arancelBase                 RN-TAR-01
 * @param recargoUrgencia             RN-TAR-05
 * @param recargoHorarioExtendido     RN-TAR-06
 * @param arancelBruto                arancel base más los recargos que correspondan
 * @param bonificacion                RN-TAR-02, RN-TAR-03, RN-TAR-04
 * @param descuentoAplicado           nombre del descuento aplicado, o "NINGUNO"
 * @param montoDescuento              RN-TAR-07, RN-TAR-08, RN-TAR-09
 * @param copago                      monto final que paga el paciente, RN-TAR-10
 */
public record DetalleTarifa(long arancelBase,
                            long recargoUrgencia,
                            long recargoHorarioExtendido,
                            long arancelBruto,
                            long bonificacion,
                            String descuentoAplicado,
                            long montoDescuento,
                            long copago) {
}
