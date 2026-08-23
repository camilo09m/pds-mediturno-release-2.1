package cl.losandes.mediturno.reservas.aplicacion;
import cl.losandes.mediturno.reservas.dominio.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RepositorioReservas {

    Reserva guardar(Reserva reserva);

    Optional<Reserva> porId(Long id);

    List<Reserva> porPaciente(String rut);

    List<Reserva> enBloque(LocalDateTime fechaHora, Especialidad especialidad);

    /**
     * Igual que {@link #enBloque}, pero declarando intención de escritura.
     *
     * <p>Release 2.1 — con sobrecupo, dos solicitudes concurrentes pueden leer a la vez
     * el mismo último cupo libre y crear ambas la reserva. El adaptador de persistencia
     * real resuelve esto tomando un cerrojo sobre las filas del bloque; las
     * implementaciones en memoria de las pruebas unitarias no necesitan hacer nada, y por
     * eso el método trae comportamiento por defecto: las suites escritas contra la
     * versión 2.0 siguen compilando sin tocar una línea.</p>
     */
    default List<Reserva> enBloqueParaActualizar(LocalDateTime fechaHora, Especialidad especialidad) {
        return enBloque(fechaHora, especialidad);
    }

    List<Reserva> todas();
}
