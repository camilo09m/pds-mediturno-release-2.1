package cl.losandes.mediturno.tarificacion;

/** Consulta de convenios de empresa vigentes (RN-TAR-08). */
public interface Convenios {
    boolean tieneConvenioVigente(String rut);
}
