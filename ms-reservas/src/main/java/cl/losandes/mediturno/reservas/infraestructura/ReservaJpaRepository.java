package cl.losandes.mediturno.reservas.infraestructura;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaJpaRepository extends JpaRepository<ReservaEntity, Long> {

    List<ReservaEntity> findByRutPacienteOrderByFechaHoraAtencionAsc(String rutPaciente);

    List<ReservaEntity> findByFechaHoraAtencionAndEspecialidad(LocalDateTime fechaHoraAtencion,
                                                              String especialidad);

    List<ReservaEntity> findAllByOrderByFechaHoraAtencionAsc();

    /**
     * Igual que {@link #findByFechaHoraAtencionAndEspecialidad} pero tomando un cerrojo
     * de escritura sobre las filas leídas ({@code SELECT ... FOR UPDATE}).
     *
     * <p>Serializa a dos transacciones que compitan por el mismo bloque mientras exista
     * al menos una reserva en él. Requiere transacción activa.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaEntity r "
            + "where r.fechaHoraAtencion = :fechaHora and r.especialidad = :especialidad")
    List<ReservaEntity> bloquearBloque(@Param("fechaHora") LocalDateTime fechaHora,
                                       @Param("especialidad") String especialidad);
}
