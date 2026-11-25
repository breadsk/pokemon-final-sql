package cl.nicolas.pokemon.repository;

import cl.nicolas.pokemon.model.Pokemon;
import java.util.List;

public interface PokemonDAO {
    /** Obtiene los Pokémon almacenados, con sus identificadores y atributos. */
    List<Pokemon> obtenerTodos();
    /** Inserta un Pokémon nuevo y le asigna el identificador generado por MySQL. */
    boolean guardar(Pokemon pokemon);
    /** Actualiza por identificador; devuelve false si el registro no existe. */
    boolean actualizar(Pokemon pokemon);
    /** Elimina por identificador; devuelve false si el registro no existe. */
    boolean eliminar(Pokemon pokemon);
}
