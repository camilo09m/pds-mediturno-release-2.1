package cl.losandes.mediturno.reservas.infraestructura;

import cl.losandes.mediturno.reservas.aplicacion.CrearReservaCmd;
import cl.losandes.mediturno.reservas.aplicacion.Notificador;
import cl.losandes.mediturno.reservas.aplicacion.Reloj;
import cl.losandes.mediturno.reservas.aplicacion.RepositorioPacientes;
import cl.losandes.mediturno.reservas.aplicacion.RepositorioReservas;
import cl.losandes.mediturno.reservas.aplicacion.ReservaService;
import cl.losandes.mediturno.reservas.aplicacion.TarificadorPort;
import cl.losandes.mediturno.reservas.aplicacion.ValidadorPrevision;
import cl.losandes.mediturno.reservas.aplicacion.eventos.ReservaEvento;
import cl.losandes.mediturno.reservas.aplicacion.eventos.TipoEventoReserva;
import cl.losandes.mediturno.reservas.dominio.Paciente;
import cl.losandes.mediturno.reservas.dominio.ReglaNegocioException;
import cl.losandes.mediturno.reservas.dominio.Reserva;
import cl.losandes.mediturno.reservas.dominio.TipoPrevision;
import cl.losandes.mediturno.reservas.infraestructura.mensajeria.EventoDeIntegracion;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Envoltura de {@link ReservaService} con lo que el Release 2.1 agrega alrededor de las
 * reglas de negocio: transacción, verificación previsional y emisión de eventos.
 *
 * <p>El servicio de aplicación se dejó intacto a propósito. Las reglas RN-RES-01 a
 * RN-RES-11 se siguen probando construyendo {@code ReservaService} a mano, sin Spring,
 * sin base de datos y sin broker — que es lo que hacen las suites de las entregas 1 y 2.
 * Todo lo que es infraestructura vive en esta clase.</p>
 */
@Service
@Primary
public class ReservaServiceOrquestado extends ReservaService {

    private final ApplicationEventPublisher local;
    private final ValidadorPrevision validador;
    private final RepositorioPacientes padron;
    private final Reloj reloj;

    public ReservaServiceOrquestado(RepositorioReservas reservas, RepositorioPacientes pacientes,
                                    TarificadorPort tarificador, Reloj reloj,
                                    Notificador notificador, ValidadorPrevision validador,
                                    ApplicationEventPublisher local) {
        super(reservas, pacientes, tarificador, reloj, notificador);
        this.local = local;
        this.validador = validador;
        this.padron = pacientes;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Reserva crear(CrearReservaCmd cmd) {
        verificarPrevision(cmd.rutPaciente());
        return emitir(TipoEventoReserva.CREADA, super.crear(cmd));
    }

    @Override
    @Transactional
    public Reserva confirmar(Long id) {
        return emitir(TipoEventoReserva.CONFIRMADA, super.confirmar(id));
    }

    @Override
    @Transactional
    public Reserva iniciar(Long id) {
        return emitir(TipoEventoReserva.INICIADA, super.iniciar(id));
    }

    @Override
    @Transactional
    public Reserva finalizar(Long id) {
        return emitir(TipoEventoReserva.FINALIZADA, super.finalizar(id));
    }

    @Override
    @Transactional
    public Reserva anular(Long id) {
        return emitir(TipoEventoReserva.ANULADA, super.anular(id));
    }

    @Override
    @Transactional
    public Reserva marcarInasistencia(Long id) {
        return emitir(TipoEventoReserva.INASISTENCIA, super.marcarInasistencia(id));
    }

    @Override
    @Transactional
    public Reserva modificar(Long id, LocalDateTime nuevaFechaHora) {
        return emitir(TipoEventoReserva.MODIFICADA, super.modificar(id, nuevaFechaHora));
    }

    @Override
    @Transactional
    public int vencerOfertas() {
        return super.vencerOfertas();
    }

    @Override
    @Transactional
    public Paciente registrarPaciente(Paciente paciente) {
        return super.registrarPaciente(paciente);
    }

    // ------------------------------------------------------------------ apoyo

    private Reserva emitir(TipoEventoReserva tipo, Reserva reserva) {
        local.publishEvent(new EventoDeIntegracion(
                ReservaEvento.de(tipo, reserva, reloj.ahora())));
        return reserva;
    }

    /**
     * RN-RES-12 — La previsión declarada se contrasta con el Validador de Previsión.
     *
     * <p>Un paciente particular no tiene afiliación que verificar, así que no se consulta.
     * Si el validador no responde, la reserva sigue: ver
     * {@code ValidadorPrevisionHttp} para el fundamento de esa degradación.</p>
     */
    private void verificarPrevision(String rut) {
        Optional<Paciente> paciente = padron.porRut(rut);
        if (paciente.isEmpty() || paciente.get().prevision() == TipoPrevision.PARTICULAR) {
            return;
        }
        ValidadorPrevision.Resultado resultado = validador.validar(rut);
        if (resultado.verificado() && !resultado.afiliado()) {
            throw new ReglaNegocioException("PREV-NO-AFILIADO",
                    "El Validador de Previsión no reconoce al RUT " + rut
                            + " como afiliado a " + paciente.get().prevision() + ".");
        }
    }
}
