package cl.losandes.mediturno.pacientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Padrón de pacientes de MediTurno 2.0.
 *
 * <p>Hasta el Release 2.0 este padrón vivía dentro de ms-reservas. El Release 2.1
 * lo extrae a un servicio propio: ms-reservas pasa a consultarlo por HTTP y, por lo
 * tanto, pasa a depender de un contrato que puede romperse.</p>
 */
@SpringBootApplication
public class MsPacientesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPacientesApplication.class, args);
    }
}
