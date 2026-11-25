package cl.nicolas.pokemon.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexionBD {
    private static final String URL_LARAGON = "jdbc:mysql://127.0.0.1:3306/pokemon";

    private ConexionBD() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("POKEMON_DB_URL", URL_LARAGON);
        String usuario = System.getenv("POKEMON_DB_USER");
        String contrasena = System.getenv("POKEMON_DB_PASSWORD");

        if (URL_LARAGON.equals(url) && usuario == null) {
            usuario = "root";
        }
        if (usuario == null || usuario.isBlank()) {
            throw new SQLException("Falta configurar el usuario de MySQL.", "08001");
        }

        Properties propiedades = new Properties();
        propiedades.setProperty("user", usuario);
        propiedades.setProperty("password", contrasena == null ? "" : contrasena);
        propiedades.setProperty("connectTimeout", "3000");
        propiedades.setProperty("socketTimeout", "5000");
        return DriverManager.getConnection(url, propiedades);
    }
}
