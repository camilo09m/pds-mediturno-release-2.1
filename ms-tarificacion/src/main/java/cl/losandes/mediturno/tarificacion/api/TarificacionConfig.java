package cl.losandes.mediturno.tarificacion.api;

import cl.losandes.mediturno.tarificacion.Convenios;
import cl.losandes.mediturno.tarificacion.Reloj;
import cl.losandes.mediturno.tarificacion.RelojSistema;
import cl.losandes.mediturno.tarificacion.RepositorioConvenios;
import cl.losandes.mediturno.tarificacion.TarificadorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

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
    public Reloj reloj() {
        return new RelojSistema();
    }

    @Bean
    public Convenios convenios(@Value("${mediturno.convenios.archivo:datos/convenios.csv}") String ruta) {
        return new RepositorioConvenios(Path.of(ruta));
    }

    @Bean
    public TarificadorService tarificadorService(Reloj reloj, Convenios convenios) {
        return new TarificadorService(reloj, convenios);
    }
}
