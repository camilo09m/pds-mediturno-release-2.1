package cl.losandes.mediturno.pacientes.api;

import java.time.LocalDateTime;

/** Cuerpo uniforme de todas las respuestas de error de la API. */
public record ErrorResponse(String codigo,
                            String mensaje,
                            int estado,
                            String ruta,
                            LocalDateTime instante) {
}
