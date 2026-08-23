package cl.losandes.mediturno.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pagos de MediTurno 2.0.
 *
 * <p>Recibe el webhook de Webpay y lo traduce a un evento interno. Es el borde del
 * sistema con la pasarela: todo lo raro que hace una pasarela de pago —confirmar dos
 * veces, confirmar antes que el comercio, confirmar algo ya anulado— entra por aquí.</p>
 */
@SpringBootApplication
public class MsPagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPagosApplication.class, args);
    }
}
