package cl.losandes.mediturno.reservas.infraestructura.redis;

import cl.losandes.mediturno.reservas.aplicacion.RegistroOfertas;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Sustituto en memoria del registro de ofertas, para el perfil {@code efimero}. */
public class RegistroOfertasMemoria implements RegistroOfertas {

    private final Map<Long, LocalDateTime> ofertas = new ConcurrentHashMap<>();

    @Override
    public void abrir(Long idReserva, LocalDateTime venceEn, LocalDateTime ahora) {
        if (idReserva != null && venceEn != null && venceEn.isAfter(ahora)) {
            ofertas.put(idReserva, venceEn);
        }
    }

    @Override
    public boolean vigente(Long idReserva) {
        LocalDateTime vence = idReserva == null ? null : ofertas.get(idReserva);
        return vence != null && vence.isAfter(LocalDateTime.now());
    }

    @Override
    public void cerrar(Long idReserva) {
        ofertas.remove(idReserva);
    }

    @Override
    public Set<Long> vigentes() {
        LocalDateTime ahora = LocalDateTime.now();
        return ofertas.entrySet().stream()
                .filter(e -> e.getValue().isAfter(ahora))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
