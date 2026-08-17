package cl.losandes.mediturno.tarificacion.api;

import cl.losandes.mediturno.tarificacion.TarificadorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publica el servicio de dominio como bean.
 *
 * <p>El dominio no depende de Spring a propósito: se puede probar sin levantar el
 * contexto. Si el servicio cambia su forma de construcción, este es el único punto
 * de la capa web que hay que ajustar.</p>
 */
@Configuration
public class TarificacionConfig {

    @Bean
    public TarificadorService tarificadorService() {
        return new TarificadorService();
    }
}
