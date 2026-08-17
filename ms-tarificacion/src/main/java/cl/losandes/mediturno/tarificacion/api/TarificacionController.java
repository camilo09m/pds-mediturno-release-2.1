package cl.losandes.mediturno.tarificacion.api;

import cl.losandes.mediturno.tarificacion.DetalleTarifa;
import cl.losandes.mediturno.tarificacion.SolicitudTarificacion;
import cl.losandes.mediturno.tarificacion.TarificadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tarificacion")
public class TarificacionController {

    private final TarificadorService tarificador;

    public TarificacionController(TarificadorService tarificador) {
        this.tarificador = tarificador;
    }

    @PostMapping("/calcular")
    public ResponseEntity<DetalleTarifa> calcular(@RequestBody SolicitudTarificacion solicitud) {
        return ResponseEntity.ok(tarificador.calcular(solicitud));
    }
}
