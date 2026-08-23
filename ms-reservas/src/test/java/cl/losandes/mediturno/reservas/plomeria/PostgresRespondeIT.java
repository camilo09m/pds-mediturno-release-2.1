package cl.losandes.mediturno.reservas.plomeria;

import cl.losandes.mediturno.reservas.aplicacion.RepositorioReservas;
import cl.losandes.mediturno.reservas.dominio.Canal;
import cl.losandes.mediturno.reservas.dominio.Especialidad;
import cl.losandes.mediturno.reservas.dominio.Reserva;
import cl.losandes.mediturno.reservas.dominio.Tarifa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plomería: PostgreSQL guarda y devuelve una reserva sin perder ningún campo.
 *
 * <p>No comprueba ninguna regla: comprueba que el mapeo de ida y vuelta entre el dominio
 * y la tabla está completo. Un campo que no se persiste produce fallas que parecen de
 * lógica y no lo son.</p>
 */
class PostgresRespondeIT extends BaseDePlomeria {

    @Autowired RepositorioReservas reservas;

    @Test
    @Transactional
    @DisplayName("una reserva sobrevive el viaje de ida y vuelta a la base")
    void idaYVuelta() {
        LocalDateTime atencion = LocalDateTime.of(2026, 10, 5, 9, 0);
        Reserva original = new Reserva("5.555.555-5", Especialidad.MEDICINA_GENERAL,
                atencion, Canal.WEB, new Tarifa(25_000L, 2_500L), atencion.minusDays(3));

        Reserva guardada = reservas.guardar(original);
        assertThat(guardada.id()).isNotNull();

        Reserva recuperada = reservas.porId(guardada.id()).orElseThrow();
        assertThat(recuperada.rutPaciente()).isEqualTo("5.555.555-5");
        assertThat(recuperada.especialidad()).isEqualTo(Especialidad.MEDICINA_GENERAL);
        assertThat(recuperada.fechaHoraAtencion()).isEqualTo(atencion);
        assertThat(recuperada.canal()).isEqualTo(Canal.WEB);
        assertThat(recuperada.estado()).isEqualTo(original.estado());
        assertThat(recuperada.arancelBruto()).isEqualTo(25_000L);
        assertThat(recuperada.copago()).isEqualTo(2_500L);
    }

    @Test
    @Transactional
    @DisplayName("la consulta con cerrojo devuelve lo mismo que la consulta simple")
    void consultaConCerrojo() {
        LocalDateTime atencion = LocalDateTime.of(2026, 10, 6, 11, 30);
        reservas.guardar(new Reserva("6.666.666-6", Especialidad.NUTRICION, atencion,
                Canal.WEB, new Tarifa(28_000L, 2_800L), atencion.minusDays(1)));

        assertThat(reservas.enBloqueParaActualizar(atencion, Especialidad.NUTRICION))
                .hasSameSizeAs(reservas.enBloque(atencion, Especialidad.NUTRICION))
                .isNotEmpty();
    }
}
