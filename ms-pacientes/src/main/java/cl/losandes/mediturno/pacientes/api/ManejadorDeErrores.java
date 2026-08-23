package cl.losandes.mediturno.pacientes.api;

import cl.losandes.mediturno.pacientes.dominio.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/** Traduce las excepciones a la representación pública de error. */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Map<String, HttpStatus> ESTADOS = Map.of(
            "PAC-404", HttpStatus.NOT_FOUND,
            "PAC-DUPLICADO", HttpStatus.CONFLICT,
            "VAL-ENUM", HttpStatus.BAD_REQUEST,
            "VAL-TRAMO", HttpStatus.BAD_REQUEST);

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> regla(ReglaNegocioException e, HttpServletRequest req) {
        return construir(e.codigo(), e.getMessage(),
                ESTADOS.getOrDefault(e.codigo(), HttpStatus.UNPROCESSABLE_ENTITY), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e,
                                                    HttpServletRequest req) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return construir("VAL-CAMPOS", detalle, HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException e,
                                                        HttpServletRequest req) {
        return construir("VAL-JSON", "El cuerpo de la petición no es un JSON válido o "
                + "algún campo tiene un tipo incorrecto.", HttpStatus.BAD_REQUEST, req);
    }

    private ResponseEntity<ErrorResponse> construir(String codigo, String mensaje,
                                                    HttpStatus estado, HttpServletRequest req) {
        return ResponseEntity.status(estado).body(new ErrorResponse(codigo, mensaje,
                estado.value(), req.getRequestURI(), LocalDateTime.now()));
    }
}
