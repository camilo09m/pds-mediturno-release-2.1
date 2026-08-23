package cl.losandes.mediturno.pacientes.api;

import cl.losandes.mediturno.pacientes.dominio.Paciente;
import cl.losandes.mediturno.pacientes.dominio.ReglaNegocioException;
import cl.losandes.mediturno.pacientes.dominio.TipoPrevision;
import cl.losandes.mediturno.pacientes.dominio.TramoFonasa;
import cl.losandes.mediturno.pacientes.infraestructura.PadronService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PadronService padron;

    public PacienteController(PadronService padron) {
        this.padron = padron;
    }

    @GetMapping
    public List<PacienteResponse> listar() {
        return padron.todos().stream().map(PacienteResponse::de).toList();
    }

    /** Consulta puntual. Es la operación que consume ms-reservas en cada reserva. */
    @GetMapping("/{rut}")
    public PacienteResponse porRut(@PathVariable String rut) {
        return padron.porRut(rut)
                .map(PacienteResponse::de)
                .orElseThrow(() -> new ReglaNegocioException("PAC-404",
                        "No existe un paciente con RUT " + rut + "."));
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody CrearPacienteRequest req) {
        TipoPrevision prevision;
        try {
            prevision = TipoPrevision.valueOf(req.prevision().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("VAL-ENUM",
                    "Valor no admitido para prevision: '" + req.prevision() + "'.");
        }
        TramoFonasa tramo = null;
        if (req.tramoFonasa() != null && !req.tramoFonasa().isBlank()) {
            try {
                tramo = TramoFonasa.valueOf(req.tramoFonasa().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ReglaNegocioException("VAL-ENUM",
                        "Valor no admitido para tramoFonasa: '" + req.tramoFonasa() + "'.");
            }
        }
        if (prevision == TipoPrevision.FONASA && tramo == null) {
            throw new ReglaNegocioException("VAL-TRAMO",
                    "Un paciente Fonasa requiere tramoFonasa.");
        }
        Paciente creado = padron.registrar(new Paciente(req.rut().trim(), req.nombre().trim(),
                req.fechaNacimiento(), prevision, tramo, Boolean.TRUE.equals(req.convenioEmpresa())));
        return ResponseEntity.status(HttpStatus.CREATED).body(PacienteResponse.de(creado));
    }
}
