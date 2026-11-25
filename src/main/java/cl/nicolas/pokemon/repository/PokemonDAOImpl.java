package cl.nicolas.pokemon.repository;

import cl.nicolas.pokemon.model.Pokemon;
import cl.nicolas.pokemon.model.PokemonAgua;
import cl.nicolas.pokemon.model.PokemonFuego;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class PokemonDAOImpl implements PokemonDAO {
    private static final String LISTAR = """
            SELECT id, tipo, nombre, nivel, peso, especie, entrenado, disponible_intercambio, shiny
            FROM pokemon ORDER BY id
            """;
    private static final String INSERTAR = """
            INSERT INTO pokemon (tipo, nombre, nivel, peso, especie, entrenado, disponible_intercambio, shiny)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String ACTUALIZAR = """
            UPDATE pokemon SET tipo = ?, nombre = ?, nivel = ?, peso = ?, especie = ?,
            entrenado = ?, disponible_intercambio = ?, shiny = ? WHERE id = ?
            """;
    private static final String ELIMINAR = "DELETE FROM pokemon WHERE id = ?";

    @Override
    public List<Pokemon> obtenerTodos() {
        List<Pokemon> pokemones = new ArrayList<>();
        try (Connection conexion = ConexionBD.getConnection();
             PreparedStatement consulta = conexion.prepareStatement(LISTAR);
             ResultSet filas = consulta.executeQuery()) {
            while (filas.next()) {
                pokemones.add(reconstruirPokemon(filas));
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw error("cargar los Pokémon", e);
        }
        return pokemones;
    }

    @Override
    public boolean guardar(Pokemon pokemon) {
        String accion = "registrar el Pokémon";
        if (pokemon == null || pokemon.getId() != 0) {
            throw new PersistenciaException("No se pudo " + accion + ": se requiere un Pokémon nuevo.");
        }

        long id;
        try (Connection conexion = ConexionBD.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            asignarDatos(sentencia, pokemon);
            if (sentencia.executeUpdate() != 1) {
                throw new SQLException("La inserción no afectó exactamente una fila.");
            }
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next() || (id = claves.getLong(1)) <= 0) {
                    throw new SQLException("La base no devolvió un identificador válido.");
                }
            }
        } catch (SQLException e) {
            throw error(accion, e);
        }
        pokemon.setId(id);
        return true;
    }

    @Override
    public boolean actualizar(Pokemon pokemon) {
        String accion = "actualizar el Pokémon";
        exigirId(pokemon, accion);
        try (Connection conexion = ConexionBD.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(ACTUALIZAR)) {
            asignarDatos(sentencia, pokemon);
            sentencia.setLong(9, pokemon.getId());
            return sentencia.executeUpdate() == 1;
        } catch (SQLException e) {
            throw error(accion, e);
        }
    }

    @Override
    public boolean eliminar(Pokemon pokemon) {
        String accion = "eliminar el Pokémon";
        exigirId(pokemon, accion);
        try (Connection conexion = ConexionBD.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(ELIMINAR)) {
            sentencia.setLong(1, pokemon.getId());
            return sentencia.executeUpdate() == 1;
        } catch (SQLException e) {
            throw error(accion, e);
        }
    }

    private static Pokemon reconstruirPokemon(ResultSet fila) throws SQLException {
        String nombre = fila.getString("nombre");
        int nivel = fila.getInt("nivel");
        double peso = fila.getDouble("peso");
        Pokemon pokemon = switch (fila.getString("tipo")) {
            case "AGUA" -> {
                PokemonAgua agua = new PokemonAgua(nombre, nivel, peso,
                        fila.getString("especie"), fila.getBoolean("entrenado"));
                if (fila.getBoolean("disponible_intercambio")) {
                    agua.marcarDisponibleParaIntercambio();
                }
                yield agua;
            }
            case "FUEGO" -> new PokemonFuego(nombre, nivel, peso, fila.getBoolean("shiny"));
            default -> throw new SQLException("El tipo almacenado no pertenece al modelo Pokémon.");
        };
        pokemon.setId(fila.getLong("id"));
        return pokemon;
    }

    private static void asignarDatos(PreparedStatement sentencia, Pokemon pokemon) throws SQLException {
        sentencia.setString(2, pokemon.getNombre());
        sentencia.setInt(3, pokemon.getNivel());
        sentencia.setDouble(4, pokemon.getPeso());
        if (pokemon instanceof PokemonAgua agua) {
            sentencia.setString(1, "AGUA");
            sentencia.setString(5, agua.getEspecie());
            sentencia.setBoolean(6, agua.isEntrenado());
            sentencia.setBoolean(7, agua.estaDisponibleParaIntercambio());
            sentencia.setNull(8, Types.BOOLEAN);
        } else if (pokemon instanceof PokemonFuego fuego) {
            sentencia.setString(1, "FUEGO");
            sentencia.setNull(5, Types.VARCHAR);
            sentencia.setNull(6, Types.BOOLEAN);
            sentencia.setNull(7, Types.BOOLEAN);
            sentencia.setBoolean(8, fuego.isShiny());
        } else {
            throw new SQLException("Tipo de Pokémon no admitido.");
        }
    }

    private static void exigirId(Pokemon pokemon, String accion) {
        if (pokemon == null || pokemon.getId() <= 0) {
            throw new PersistenciaException("No se pudo " + accion + ": el registro no tiene identificador.");
        }
    }

    private static PersistenciaException error(String accion, Exception causa) {
        String detalle = "Revisa los datos ingresados o el esquema de la base de datos.";
        if (causa instanceof SQLException sql) {
            String estado = sql.getSQLState();
            if (sql.getErrorCode() == 1049 || sql.getErrorCode() == 1146) {
                detalle = "Comprueba que ejecutaste el esquema scripts/pokemon_mysql.sql en MySQL.";
            } else if (estado != null && (estado.startsWith("08") || estado.startsWith("28"))) {
                detalle = "Comprueba que MySQL esté disponible y que la conexión esté configurada.";
            }
        }
        return new PersistenciaException("No se pudo " + accion + ". " + detalle, causa);
    }
}
