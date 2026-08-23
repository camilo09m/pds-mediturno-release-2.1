package cl.losandes.mediturno.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Avisos a pacientes de MediTurno 2.0.
 *
 * <p>Consume los eventos de reserva y las solicitudes de aviso, y despacha mensajes por
 * el proveedor de mensajería, que es un servicio externo con cuota horaria.</p>
 */
@SpringBootApplication
public class MsNotificacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsNotificacionesApplication.class, args);
    }
}
