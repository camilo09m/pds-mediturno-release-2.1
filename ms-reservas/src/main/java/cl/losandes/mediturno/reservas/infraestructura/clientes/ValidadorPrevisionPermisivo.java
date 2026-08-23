package cl.losandes.mediturno.reservas.infraestructura.clientes;

import cl.losandes.mediturno.reservas.aplicacion.ValidadorPrevision;

/**
 * Sustituto del Validador de Previsión para el perfil {@code efimero}, que se levanta
 * sin servicios externos. Nunca verifica, de modo que ninguna reserva se rechaza por
 * previsión.
 */
public class ValidadorPrevisionPermisivo implements ValidadorPrevision {

    @Override
    public Resultado validar(String rut) {
        return Resultado.noVerificado();
    }
}
