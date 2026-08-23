package cl.losandes.mediturno.pacientes.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteJpaRepository extends JpaRepository<PacienteEntity, String> {
}
