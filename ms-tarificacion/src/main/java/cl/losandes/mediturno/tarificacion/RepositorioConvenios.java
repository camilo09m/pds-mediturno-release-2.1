package cl.losandes.mediturno.tarificacion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convenios de empresa vigentes (RN-TAR-08).
 *
 * <p>Los convenios se leen del archivo {@code datos/convenios.csv}, que contiene un RUT
 * por línea. El archivo se lee una sola vez y se mantiene en memoria.</p>
 *
 * <p>Release 2.1 — el servicio ahora se despliega en contenedor, donde el directorio de
 * trabajo no siempre es la raíz del repositorio. Si el archivo no está en la ruta
 * indicada, se recurre al mismo archivo empaquetado en el classpath. Si tampoco está
 * ahí, se falla: un convenio no leído produciría copagos silenciosamente erróneos.</p>
 */
public class RepositorioConvenios implements Convenios {

    private static final Path ARCHIVO_POR_DEFECTO = Path.of("datos", "convenios.csv");
    private static final String RECURSO_CLASSPATH = "convenios.csv";

    private final Path archivo;

    private Set<String> rutsConConvenio;

    public RepositorioConvenios() {
        this(ARCHIVO_POR_DEFECTO);
    }

    public RepositorioConvenios(Path archivo) {
        this.archivo = archivo;
    }

    @Override
    public boolean tieneConvenioVigente(String rut) {
        if (rutsConConvenio == null) {
            cargar();
        }
        return rutsConConvenio.contains(normalizar(rut));
    }

    private void cargar() {
        rutsConConvenio = new HashSet<>();
        for (String linea : leerLineas()) {
            String limpia = linea.trim();
            if (limpia.isEmpty() || limpia.startsWith("#")) {
                continue;
            }
            rutsConConvenio.add(normalizar(limpia.split(";")[0]));
        }
    }

    private List<String> leerLineas() {
        try {
            if (Files.exists(archivo)) {
                return Files.readAllLines(archivo, StandardCharsets.UTF_8);
            }
            try (InputStream in = RepositorioConvenios.class.getClassLoader()
                    .getResourceAsStream(RECURSO_CLASSPATH)) {
                if (in == null) {
                    throw new IOException("No se encontró " + archivo.toAbsolutePath()
                            + " ni el recurso de classpath " + RECURSO_CLASSPATH + ".");
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el padrón de convenios.", e);
        }
    }

    private String normalizar(String rut) {
        return rut == null ? "" : rut.replace(".", "").replace("-", "").toUpperCase();
    }
}
