package cl.losandes.mediturno.reservas.plomeria;

import cl.losandes.mediturno.reservas.aplicacion.PublicadorEventos;
import cl.losandes.mediturno.reservas.aplicacion.RegistroIdempotencia;
import cl.losandes.mediturno.reservas.aplicacion.RegistroOfertas;
import cl.losandes.mediturno.reservas.aplicacion.RepositorioReservas;
import cl.losandes.mediturno.reservas.aplicacion.ReservaService;
import cl.losandes.mediturno.reservas.aplicacion.TarificadorPort;
import cl.losandes.mediturno.reservas.aplicacion.ValidadorPrevision;
import cl.losandes.mediturno.reservas.infraestructura.ReservaServiceOrquestado;
import cl.losandes.mediturno.reservas.infraestructura.clientes.RepositorioPacientesHttp;
import cl.losandes.mediturno.reservas.infraestructura.clientes.TarificadorHttp;
import cl.losandes.mediturno.reservas.infraestructura.mensajeria.PublicadorKafka;
import cl.losandes.mediturno.reservas.infraestructura.redis.RegistroIdempotenciaRedis;
import cl.losandes.mediturno.reservas.infraestructura.redis.RegistroOfertasRedis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plomería: el contexto levanta y cada puerto quedó cableado a la implementación que
 * corresponde al perfil con infraestructura.
 *
 * <p>Es la prueba que evita la pérdida de tiempo más común: creer que se está probando
 * contra PostgreSQL y Kafka cuando en realidad el contexto cayó al perfil en memoria.</p>
 */
class ContextoLevantaIT extends BaseDePlomeria {

    @Autowired ReservaService reservaService;
    @Autowired RepositorioReservas repositorioReservas;
    @Autowired TarificadorPort tarificador;
    @Autowired ValidadorPrevision validador;
    @Autowired PublicadorEventos publicador;
    @Autowired RegistroOfertas ofertas;
    @Autowired RegistroIdempotencia idempotencia;

    @Test
    @DisplayName("cada puerto está cableado a su adaptador con infraestructura real")
    void cableadoCorrecto() {
        assertThat(reservaService).isInstanceOf(ReservaServiceOrquestado.class);
        assertThat(tarificador).isInstanceOf(TarificadorHttp.class);
        assertThat(repositorioReservas).isNotNull();
        assertThat(validador).isNotNull();
        assertThat(publicador).isInstanceOf(PublicadorKafka.class);
        assertThat(ofertas).isInstanceOf(RegistroOfertasRedis.class);
        assertThat(idempotencia).isInstanceOf(RegistroIdempotenciaRedis.class);
    }

    @Test
    @DisplayName("el padrón se consulta por HTTP y no contra la base local")
    void padronPorHttp(@Autowired
                       cl.losandes.mediturno.reservas.aplicacion.RepositorioPacientes padron) {
        assertThat(padron).isInstanceOf(RepositorioPacientesHttp.class);
    }
}
