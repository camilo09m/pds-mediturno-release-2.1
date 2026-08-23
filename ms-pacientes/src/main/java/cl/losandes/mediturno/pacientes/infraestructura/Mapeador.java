package cl.losandes.mediturno.pacientes.infraestructura;

import cl.losandes.mediturno.pacientes.dominio.Paciente;
import cl.losandes.mediturno.pacientes.dominio.TipoPrevision;
import cl.losandes.mediturno.pacientes.dominio.TramoFonasa;

/** Traduce entre el dominio puro y la entidad JPA. */
final class Mapeador {

    private Mapeador() {
    }

    static Paciente aDominio(PacienteEntity e) {
        return new Paciente(e.getRut(), e.getNombre(), e.getFechaNacimiento(),
                TipoPrevision.valueOf(e.getPrevision()),
                e.getTramoFonasa() == null || e.getTramoFonasa().isBlank()
                        ? null : TramoFonasa.valueOf(e.getTramoFonasa()),
                e.isConvenioEmpresa());
    }

    static PacienteEntity aEntidad(Paciente p) {
        return new PacienteEntity(p.rut(), p.nombre(), p.fechaNacimiento(),
                p.prevision().name(),
                p.tramoFonasa() == null ? null : p.tramoFonasa().name(),
                p.convenioEmpresa());
    }
}
