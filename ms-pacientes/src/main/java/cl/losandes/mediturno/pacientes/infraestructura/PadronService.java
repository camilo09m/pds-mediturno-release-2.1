package cl.losandes.mediturno.pacientes.infraestructura;

import cl.losandes.mediturno.pacientes.dominio.Paciente;
import cl.losandes.mediturno.pacientes.dominio.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PadronService {

    private final PacienteJpaRepository jpa;

    public PadronService(PacienteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Transactional(readOnly = true)
    public Optional<Paciente> porRut(String rut) {
        return jpa.findById(rut).map(Mapeador::aDominio);
    }

    @Transactional(readOnly = true)
    public List<Paciente> todos() {
        return jpa.findAll().stream().map(Mapeador::aDominio).toList();
    }

    @Transactional
    public Paciente registrar(Paciente paciente) {
        if (jpa.existsById(paciente.rut())) {
            throw new ReglaNegocioException("PAC-DUPLICADO",
                    "Ya existe un paciente con RUT " + paciente.rut() + ".");
        }
        jpa.save(Mapeador.aEntidad(paciente));
        return paciente;
    }
}
