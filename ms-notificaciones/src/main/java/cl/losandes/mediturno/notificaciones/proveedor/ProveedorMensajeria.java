package cl.losandes.mediturno.notificaciones.proveedor;

/** Salida hacia el proveedor externo de mensajería. */
public interface ProveedorMensajeria {

    void enviar(String canal, String destinatario, String cuerpo);
}
