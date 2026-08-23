package cl.losandes.mediturno.reservas.aplicacion;

/**
 * Validador de Previsión: servicio externo que confirma la afiliación previsional
 * declarada por el paciente.
 *
 * <p>Su disponibilidad histórica es de 97,2 %, de modo que la indisponibilidad no es un
 * caso excepcional sino un modo de operación esperable. Por eso el resultado distingue
 * "no está afiliado" de "no se pudo verificar": son cosas distintas y el negocio las
 * trata distinto.</p>
 */
public interface ValidadorPrevision {

    /**
     * @param verificado false cuando el servicio no respondió y la reserva sigue en
     *                   modo degradado; en ese caso el resto de los campos no son
     *                   concluyentes.
     */
    record Resultado(boolean verificado, boolean afiliado, String prevision, String tramo) {

        public static Resultado noVerificado() {
            return new Resultado(false, false, null, null);
        }
    }

    Resultado validar(String rut);
}
