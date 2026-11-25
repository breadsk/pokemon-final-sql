
# Centro Pokémon · persistencia MySQL

Versión de **práctica** construida sobre el proyecto original `breadsk/pokemon-fnal`. Adapta al Centro Pokémon los criterios técnicos de la evaluación DSY1102 EP3: JDBC, MVC, DAO y persistencia de registros. No es la solución oficial de la Forma F de viviendas. La evaluación declara que no se admite el uso de IA; no uses esta adaptación como entrega de esa evaluación.

## Cómo ejecutar en Laragon 6 (Windows)

1. Instala **JDK 21** y configura `JAVA_HOME` para que apunte a ese JDK. Inicia MySQL en Laragon con **Start All**. El esquema está diseñado para **MySQL 8.0.16 o superior**; puedes comprobar el motor que está realmente activo con `SELECT VERSION();`.
2. Abre la [Terminal de Laragon](https://laragon.org/docs/terminal) con el intérprete `cmd.exe` y sitúate en la carpeta del proyecto, donde está `pom.xml`. Si tu instalación local usa `root` sin contraseña y el puerto `3306`, ejecuta:

   ```cmd
   mysql -h 127.0.0.1 -P 3306 -u root < scripts\pokemon_mysql.sql
   mvnw.cmd clean javafx:run
   ```

   También puedes ejecutar `scripts/pokemon_mysql.sql` desde HeidiSQL con una cuenta con permisos de creación. El script crea la base `pokemon`, las tablas y el catálogo de tipos. La tabla de Pokémon empieza vacía. Si quieres recuperar el Squirtle que venía en el proyecto original, ejecuta **una sola vez** `scripts/datos_originales_mysql.sql`. No se necesitan archivos JSON.

3. Si tu Laragon usa otra configuración, añade `-p` al comando `mysql` si la cuenta tiene contraseña y ajusta `-u` y `-P` según corresponda. Para Java, define las variables que necesites en la misma terminal o en el perfil de ejecución de tu IDE:

   | Variable | Valor |
   |---|---|
   | `POKEMON_DB_URL` | Por defecto `jdbc:mysql://127.0.0.1:3306/pokemon`. Cámbiala si usas otro servidor o puerto. |
   | `POKEMON_DB_USER` | `root` por defecto únicamente con la URL local anterior. Con una URL distinta, define el usuario explícitamente. |
   | `POKEMON_DB_PASSWORD` | Vacía por defecto. Si tu cuenta tiene contraseña, defínela fuera del código y no la guardes en el repositorio. |

   Ejemplo en `cmd.exe`, si MySQL está en el puerto `3307`:

   ```cmd
   set "POKEMON_DB_URL=jdbc:mysql://127.0.0.1:3307/pokemon"
   set "POKEMON_DB_USER=root"
   mvnw.cmd clean javafx:run
   ```

   La configuración `root` sin contraseña es solo para el entorno de desarrollo local que ya la utilice. Para otros entornos, usa una cuenta con contraseña y permisos `SELECT`, `INSERT`, `UPDATE` y `DELETE` sobre `pokemon`. En Linux puedes iniciar con `bash mvnw clean javafx:run`. El primer arranque de Maven requiere descargar las dependencias si no están en caché. Si MySQL no responde, la ventana informa el problema; usa **Recargar** cuando se recupere la conexión.

## Verificación de la práctica

Usa una base local de pruebas. La aplicación conserva las reglas de curación del proyecto original:

| Tipo | Apodo de ejemplo | Nivel | Peso | Datos del tipo | Curación |
|---|---|---:|---:|---|---:|
| Agua | Agua-01 | 43 | 6,5 | Marill, no entrenado, con intercambio | $19.500 |
| Agua | Agua-02 | 20 | 9,1 | Squirtle, entrenado, sin intercambio | $15.000 |
| Fuego | Fuego-01 | 55 | 19 | Shiny | $13.800 |
| Fuego | Fuego-02 | 12 | 8 | No shiny | $12.000 |

1. Registra los cuatro. Edita **Agua-01**, marca *Está entrenado* y guarda. El costo debe pasar a **$15.000**.
2. Cierra y vuelve a abrir la aplicación. Debes ver los cuatro registros y el cambio en **Agua-01**. Elimina **Fuego-02** y vuelve a abrir: ese registro no debe aparecer.
3. Contrasta los registros con `SELECT * FROM pokemon.pokemon ORDER BY id;` en tu cliente MySQL.
4. Con MySQL detenido o una dirección de prueba inaccesible, abre la aplicación. Debe mostrar **No se pudo cargar los Pokémon** sin cerrar la ventana.

### Pruebas automatizadas

Ejecuta cada modo en una sesión con entorno gráfico y una base desechable. En Linux sin pantalla, agrega `xvfb-run -a` antes de `bash`.

- `POKEMON_TEST_MODE=mysql bash mvnw test` usa la conexión ya configurada. Prueba arranque, los cuatro registros, edición de ambos subtipos, reapertura de la ventana, borrado y comparación con MySQL. Elimina los registros que creó si completa el flujo.
- `POKEMON_TEST_MODE=sin-conexion POKEMON_DB_URL=jdbc:mysql://127.0.0.1:1/pokemon POKEMON_DB_USER=sin_conexion POKEMON_DB_PASSWORD= bash mvnw test` simula un servidor no disponible y comprueba los mensajes y que la ventana siga abierta. En PowerShell, define las mismas variables con `$env:NOMBRE = 'valor'` antes de ejecutar `mvnw.cmd test`.

## Cambios respecto del original

| Criterio adaptado | Implementación |
|---|---|
| Conector, esquema y conexión | `pom.xml`, `scripts/pokemon_mysql.sql` y `repository/ConexionBD.java`. La conexión y sus parámetros se administran en esa clase. |
| Lectura, inserción, edición y borrado | `PokemonDAOImpl` implementa `PokemonDAO`, reconstruye ambos tipos completos, usa `PreparedStatement` y cierra conexión, sentencia y resultado con `try-with-resources`. La lectura completa no recibe datos de usuario; las escrituras enlazan todos sus valores con `?`. |
| Identidad e integridad | MySQL genera el ID. Edición y borrado actúan por ID, no por apodo. La referencia al tipo y las restricciones distinguen los datos de Agua y Fuego. |
| Sustitución de la persistencia | La capa se llama `repository`. `PokemonDAO` declara `obtenerTodos`, `guardar`, `actualizar` y `eliminar`. Se retiraron la dependencia Gson, el DAO de archivos y los archivos JSON del proyecto. |
| Integración y avisos | El controlador solo usa el contrato DAO y no contiene SQL ni datos de conexión. La vista muestra el resultado o el mensaje de `PersistenciaException` y permite recargar. |

**Diferencia con la Forma F:** el repositorio Pokémon no traía scripts oficiales, un identificador persistente, edición, área de mensajes ni punto de integración marcado. Por eso se añadió un esquema propio de MySQL y se hicieron cambios puntuales al modelo base Pokémon, al controlador y a la vista. Se mantuvieron las clases de arranque y las reglas de los subtipos. No se usa ni se valida el esquema de viviendas, Oracle ni el servidor del laboratorio.

## Estructura

```
src/main/java/cl/nicolas/pokemon/
├── Main.java                  # JavaFX Application
├── Launcher.java              # Entry point alternativa
├── controller/MainController.java
├── repository/
│   ├── PokemonDAO.java
│   ├── PokemonDAOImpl.java
│   ├── ConexionBD.java
│   └── PersistenciaException.java
└── model/
    ├── Pokemon.java           # ¡Clase padre abstracta!
    ├── PokemonAgua.java       # (antes Perro)
    ├── PokemonFuego.java      # (antes Gato)
    └── Intercambiable.java    # (antes Adoptable)
src/main/resources/cl/nicolas/pokemon/
├── view.fxml
└── css/styles.css
```
