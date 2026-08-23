package cl.losandes.mediturno.notificaciones.mensajeria;

import cl.losandes.mediturno.notificaciones.eventos.Topics;
import cl.losandes.mediturno.notificaciones.proveedor.CuotaAgotadaException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class ConfiguracionKafka {

    @Value("${mediturno.kafka.reintentos:2}")
    private long reintentos;

    @Value("${mediturno.kafka.espera-entre-reintentos-ms:1000}")
    private long esperaMs;

    /**
     * Reintenta y deriva a la cola de mensajes muertos.
     *
     * <p>No se reintentan dos casos: la carga útil ilegible, que no va a mejorar, y la
     * cuota agotada del proveedor, que se repone por hora y no por segundos. Reintentar
     * cualquiera de los dos solo gasta el presupuesto de reintentos.</p>
     */
    @Bean
    public DefaultErrorHandler manejadorDeErrores(KafkaTemplate<String, String> plantilla) {
        DeadLetterPublishingRecoverer aLaDlq = new DeadLetterPublishingRecoverer(plantilla,
                (registro, excepcion) -> new TopicPartition(
                        Topics.dlqDe(registro.topic()), registro.partition()));
        DefaultErrorHandler manejador =
                new DefaultErrorHandler(aLaDlq, new FixedBackOff(esperaMs, reintentos));
        manejador.addNotRetryableExceptions(JsonProcessingException.class,
                IllegalArgumentException.class, CuotaAgotadaException.class);
        return manejador;
    }
}
