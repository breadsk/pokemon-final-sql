-- Opcional: carga la instantánea JSON incluida en el repositorio original.
-- Ejecuta solo una vez y únicamente si quieres conservar ese registro de ejemplo.
USE pokemon;
INSERT INTO pokemon (tipo, nombre, nivel, peso, especie, entrenado, disponible_intercambio, shiny)
VALUES ('AGUA', 'squirtle', 50, 5.0, 'agua', TRUE, TRUE, NULL);
