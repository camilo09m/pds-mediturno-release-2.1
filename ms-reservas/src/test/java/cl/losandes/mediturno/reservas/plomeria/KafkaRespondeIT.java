package cl.losandes.mediturno.reservas.plomeria;

import cl.losandes.mediturno.reservas.aplicacion.eventos.Topics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plomería: el broker acepta un mensaje y lo devuelve, y los topics del sistema existen
 * con su cola de mensajes muertos.
 *
 * <p>No prueba ningún flujo de negocio: publica una carga útil de prueba en un topic
 * propio y la lee de vuelta. Es la comprobación que separa "mi consumidor tiene un
 * defecto" de "el mensaje nunca llegó al broker".</p>
 */
class KafkaRespondeIT extends BaseDePlomeria {

    private static final String TOPIC_DE_PRUEBA = "mediturno.plomeria";

    @Autowired KafkaTemplate<String, String> plantilla;
    @Autowired KafkaAdmin admin;

    @Test
    @DisplayName("los seis topics del sistema existen, cada uno con su cola de mensajes muertos")
    void topicsCreados() {
        Set<String> existentes = admin.describeTopics(
                Topics.RESERVAS_EVENTOS,
                Topics.dlqDe(Topics.RESERVAS_EVENTOS),
                Topics.NOTIFICACIONES_SOLICITUDES,
                Topics.dlqDe(Topics.NOTIFICACIONES_SOLICITUDES),
                Topics.PAGOS_CONFIRMACIONES,
                Topics.dlqDe(Topics.PAGOS_CONFIRMACIONES)).keySet();

        assertThat(existentes).containsExactlyInAnyOrder(
                Topics.RESERVAS_EVENTOS,
                Topics.dlqDe(Topics.RESERVAS_EVENTOS),
                Topics.NOTIFICACIONES_SOLICITUDES,
                Topics.dlqDe(Topics.NOTIFICACIONES_SOLICITUDES),
                Topics.PAGOS_CONFIRMACIONES,
                Topics.dlqDe(Topics.PAGOS_CONFIRMACIONES));
    }

    @Test
    @DisplayName("lo que se publica se puede leer de vuelta, con su clave de partición")
    void idaYVuelta() {
        String clave = UUID.randomUUID().toString();
        String carga = "{\"plomeria\":true}";

        plantilla.send(TOPIC_DE_PRUEBA, clave, carga);
        plantilla.flush();

        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", KAFKA.getBootstrapServers());
        config.put("group.id", "plomeria-" + clave);
        config.put("auto.offset.reset", "earliest");

        try (KafkaConsumer<String, String> consumidor =
                     new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumidor.subscribe(List.of(TOPIC_DE_PRUEBA));
            ConsumerRecords<String, String> registros = consumidor.poll(Duration.ofSeconds(20));

            assertThat(registros.count()).isPositive();
            ConsumerRecord<String, String> primero = registros.iterator().next();
            assertThat(primero.key()).isEqualTo(clave);
            assertThat(primero.value()).isEqualTo(carga);
        }
    }
}
