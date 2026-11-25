package cl.nicolas.pokemon.repository;

/** Error de acceso a datos con un mensaje apto para la interfaz. */
public final class PersistenciaException extends RuntimeException {
    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
