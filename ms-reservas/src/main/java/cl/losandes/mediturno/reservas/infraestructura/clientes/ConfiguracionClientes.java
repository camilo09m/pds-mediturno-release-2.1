package cl.losandes.mediturno.reservas.infraestructura.clientes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP de ms-reservas.
 *
 * <p>Los tiempos de espera están puestos aquí y no en cada cliente: un timeout es una
 * decisión de arquitectura, no un detalle de implementación. El del Validador de
 * Previsión es de 3 segundos por contrato, aunque su SLA declarado sea de 500 ms.</p>
 */
@Configuration
@Profile("!efimero")
public class ConfiguracionClientes {

    private static ClientHttpRequestFactory fabrica(int conexionMs, int lecturaMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(conexionMs);
        f.setReadTimeout(lecturaMs);
        return f;
    }

    @Bean
    public RestClient clienteTarificacion(
            @Value("${mediturno.clientes.tarificacion.url}") String url,
            @Value("${mediturno.clientes.tarificacion.timeout-conexion-ms:1000}") int conexion,
            @Value("${mediturno.clientes.tarificacion.timeout-lectura-ms:2000}") int lectura) {
        return RestClient.builder().baseUrl(url).requestFactory(fabrica(conexion, lectura)).build();
    }

    @Bean
    public RestClient clientePacientes(
            @Value("${mediturno.clientes.pacientes.url}") String url,
            @Value("${mediturno.clientes.pacientes.timeout-conexion-ms:1000}") int conexion,
            @Value("${mediturno.clientes.pacientes.timeout-lectura-ms:2000}") int lectura) {
        return RestClient.builder().baseUrl(url).requestFactory(fabrica(conexion, lectura)).build();
    }

    @Bean
    public RestClient clienteValidadorPrevision(
            @Value("${mediturno.clientes.validador-prevision.url}") String url,
            @Value("${mediturno.clientes.validador-prevision.timeout-conexion-ms:1000}") int conexion,
            @Value("${mediturno.clientes.validador-prevision.timeout-lectura-ms:3000}") int lectura) {
        return RestClient.builder().baseUrl(url).requestFactory(fabrica(conexion, lectura)).build();
    }
}
