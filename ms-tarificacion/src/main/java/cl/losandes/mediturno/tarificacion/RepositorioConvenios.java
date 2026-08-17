package cl.losandes.mediturno.tarificacion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Convenios de empresa vigentes (RN-TAR-08).
 *
 * <p>Los convenios se leen del archivo {@code datos/convenios.csv}, que contiene un RUT
 * por línea. El archivo se lee una sola vez y se mantiene en memoria.</p>
 */
public class RepositorioConvenios implements Convenios {

    private static final Path ARCHIVO = Path.of("datos", "convenios.csv");

    private Set<String> rutsConConvenio;

    @Override
    public boolean tieneConvenioVigente(String rut) {
        if (rutsConConvenio == null) {
            cargar();
        }
        return rutsConConvenio.contains(normalizar(rut));
    }

    private void cargar() {
        rutsConConvenio = new HashSet<>();
        try {
            for (String linea : Files.readAllLines(ARCHIVO, StandardCharsets.UTF_8)) {
                String limpia = linea.trim();
                if (limpia.isEmpty() || limpia.startsWith("#")) {
                    continue;
                }
                rutsConConvenio.add(normalizar(limpia.split(";")[0]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ARCHIVO.toAbsolutePath(), e);
        }
    }

    private String normalizar(String rut) {
        return rut == null ? "" : rut.replace(".", "").replace("-", "").toUpperCase();
    }
}
