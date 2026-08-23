package cl.losandes.mediturno.reservas.infraestructura;

import cl.losandes.mediturno.reservas.aplicacion.ReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vence las ventanas de confirmación de la lista de espera y traspasa el cupo al
 * siguiente de la fila.
 *
 * <p>Se puede apagar con {@code mediturno.vencimiento-ofertas.habilitado=false}. Las
 * pruebas de integración la apagan y llaman a {@code vencerOfertas()} a mano: una tarea
 * que se dispara sola cada minuto mientras corre la suite es una de las causas
 * habituales de pruebas inestables.</p>
 */
@Component
@ConditionalOnProperty(name = "mediturno.vencimiento-ofertas.habilitado",
        havingValue = "true", matchIfMissing = true)
public class TareaVencerOfertas {

    private static final Logger log = LoggerFactory.getLogger(TareaVencerOfertas.class);

    private final ReservaService reservas;

    public TareaVencerOfertas(ReservaService reservas) {
        this.reservas = reservas;
    }

    @Scheduled(fixedDelayString = "${mediturno.vencimiento-ofertas.cada-ms:60000}")
    public void barrer() {
        int vencidas = reservas.vencerOfertas();
        if (vencidas > 0) {
            log.info("Vencieron {} ofertas de cupo sin confirmar.", vencidas);
        }
    }
}
