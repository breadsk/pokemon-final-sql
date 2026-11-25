-- Esquema de práctica del Centro Pokémon para MySQL 8.0.16 o superior.
-- No elimina tablas ni datos y puede ejecutarse más de una vez.
CREATE DATABASE IF NOT EXISTS pokemon CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE pokemon;

CREATE TABLE IF NOT EXISTS tipo_pokemon (
                                            codigo VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    descripcion VARCHAR(30) NOT NULL
    ) ENGINE = InnoDB;

INSERT INTO tipo_pokemon (codigo, descripcion) VALUES ('AGUA', 'Agua'), ('FUEGO', 'Fuego')
    ON DUPLICATE KEY UPDATE descripcion = tipo_pokemon.descripcion;

CREATE TABLE IF NOT EXISTS pokemon (
                                       id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                       tipo VARCHAR(5) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    nivel INT NOT NULL,
    peso DOUBLE NOT NULL,
    especie VARCHAR(120),
    entrenado BOOLEAN,
    disponible_intercambio BOOLEAN,
    shiny BOOLEAN,
    CONSTRAINT fk_pokemon_tipo FOREIGN KEY (tipo) REFERENCES tipo_pokemon(codigo),
    CONSTRAINT ck_pokemon_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT ck_pokemon_nivel CHECK (nivel BETWEEN 1 AND 100),
    CONSTRAINT ck_pokemon_peso CHECK (peso > 0),
    CONSTRAINT ck_pokemon_subtipo CHECK (
    (tipo = 'AGUA' AND entrenado IS NOT NULL AND entrenado IN (0, 1)
    AND disponible_intercambio IS NOT NULL AND disponible_intercambio IN (0, 1) AND shiny IS NULL)
    OR (tipo = 'FUEGO' AND especie IS NULL AND entrenado IS NULL AND disponible_intercambio IS NULL
        AND shiny IS NOT NULL AND shiny IN (0, 1))
    )
    ) ENGINE = InnoDB;
