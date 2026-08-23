package cl.losandes.mediturno.reservas.infraestructura.redis;

import cl.losandes.mediturno.reservas.aplicacion.RegistroIdempotencia;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sustituto en memoria del registro de idempotencia, para el perfil {@code efimero}. */
public class RegistroIdempotenciaMemoria implements RegistroIdempotencia {

    private final Map<String, Boolean> vistos = new ConcurrentHashMap<>();

    @Override
    public boolean registrarSiEsNuevo(String clave, Duration vigencia) {
        return vistos.putIfAbsent(clave, Boolean.TRUE) == null;
    }

    @Override
    public void olvidar(String clave) {
        vistos.remove(clave);
    }
}
