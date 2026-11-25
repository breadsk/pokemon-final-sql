package cl.nicolas.pokemon;

import cl.nicolas.pokemon.repository.ConexionBD;
import cl.nicolas.pokemon.repository.PersistenciaException;
import cl.nicolas.pokemon.repository.PokemonDAO;
import cl.nicolas.pokemon.repository.PokemonDAOImpl;
import cl.nicolas.pokemon.model.Pokemon;
import cl.nicolas.pokemon.model.PokemonAgua;
import cl.nicolas.pokemon.model.PokemonFuego;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "POKEMON_TEST_MODE", matches = "mysql|sin-conexion")
class FlujoPokemonTest {
    @BeforeAll
    static void iniciarJavaFx() throws InterruptedException {
        CountDownLatch iniciado = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            iniciado.countDown();
        });
        assertTrue(iniciado.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void cerrarJavaFx() {
        Platform.exit();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "POKEMON_TEST_MODE", matches = "mysql")
    void registraEditaReabreYEliminaDesdeLaVentana() throws Exception {
        String prefijo = "Prueba O'" + UUID.randomUUID().toString().substring(0, 8);
        PokemonDAO dao = new PokemonDAOImpl();
        var idsIniciales = dao.obtenerTodos().stream().map(Pokemon::getId).toList();

        enVentana(pantalla -> {
            assertEquals(idsIniciales, pantalla.tabla().getItems().stream().map(Pokemon::getId).toList());
            assertEquals("", pantalla.mensaje());
            pantalla.registrar("PokemonAgua", prefijo + " A", "43", "6,5", "Marill", false, true);
            pantalla.registrar("PokemonAgua", prefijo + " B", "20", "9.1", "Squirtle", true, false);
            pantalla.registrar("PokemonFuego", prefijo + " C", "55", "19", "", true, false);
            pantalla.registrar("PokemonFuego", prefijo + " D", "12", "8", "", false, false);
            assertEquals("$19.500", pantalla.costo(prefijo + " A"));
            assertEquals("$15.000", pantalla.costo(prefijo + " B"));
            assertEquals("$13.800", pantalla.costo(prefijo + " C"));
            assertEquals("$12.000", pantalla.costo(prefijo + " D"));

            long id = pantalla.pokemon(prefijo + " A").getId();
            assertTrue(id > 0);
            pantalla.tabla().getSelectionModel().select(pantalla.pokemon(prefijo + " A"));
            pantalla.control("btnEditar", Button.class).fire();
            assertEquals("Marill", pantalla.control("txtEspecie", TextField.class).getText());
            assertTrue(pantalla.control("chkIntercambio", CheckBox.class).isSelected());
            pantalla.texto("txtNombre", prefijo + " A editado");
            pantalla.texto("txtNivel", "44");
            pantalla.texto("txtPeso", "7.5");
            pantalla.texto("txtEspecie", "Azumarill");
            pantalla.control("chkEspecifico", CheckBox.class).setSelected(true);
            pantalla.control("chkIntercambio", CheckBox.class).setSelected(false);
            pantalla.control("btnGuardar", Button.class).fire();
            assertEquals("Pokémon actualizado correctamente.", pantalla.mensaje());
            assertEquals(id, pantalla.pokemon(prefijo + " A editado").getId());
            assertEquals("$15.000", pantalla.costo(prefijo + " A editado"));

            PokemonFuego fuego = (PokemonFuego) pantalla.pokemon(prefijo + " C");
            pantalla.tabla().getSelectionModel().select(fuego);
            pantalla.control("btnEditar", Button.class).fire();
            assertTrue(pantalla.control("chkEspecifico", CheckBox.class).isSelected());
            pantalla.control("chkEspecifico", CheckBox.class).setSelected(false);
            pantalla.control("btnGuardar", Button.class).fire();
            assertEquals(fuego.getId(), pantalla.pokemon(prefijo + " C").getId());
            assertEquals("$12.000", pantalla.costo(prefijo + " C"));
        });

        enVentana(pantalla -> {
            assertEquals(idsIniciales.size() + 4, pantalla.tabla().getItems().size());
            PokemonAgua agua = (PokemonAgua) pantalla.pokemon(prefijo + " A editado");
            assertEquals(44, agua.getNivel());
            assertEquals(7.5, agua.getPeso());
            assertEquals("Azumarill", agua.getEspecie());
            assertTrue(agua.isEntrenado());
            assertFalse(agua.estaDisponibleParaIntercambio());
            Pokemon eliminar = pantalla.pokemon(prefijo + " D");
            pantalla.tabla().getSelectionModel().select(eliminar);
            pantalla.control("btnEliminar", Button.class).fire();
            assertEquals("Pokémon eliminado correctamente.", pantalla.mensaje());
            assertFalse(pantalla.tabla().getItems().stream().anyMatch(p -> p.getId() == eliminar.getId()));
            assertFalse(dao.eliminar(eliminar));
        });

        enVentana(pantalla -> {
            assertEquals(idsIniciales.size() + 3, pantalla.tabla().getItems().size());
            assertFalse(pantalla.tabla().getItems().stream().anyMatch(p -> p.getNombre().equals(prefijo + " D")));
            assertEquals(dao.obtenerTodos().stream().map(Pokemon::getId).toList(),
                    pantalla.tabla().getItems().stream().map(Pokemon::getId).toList());
        });

        var registros = dao.obtenerTodos().stream().filter(p -> p.getNombre().startsWith(prefijo)).toList();
        assertEquals(3, registros.size());
        try (var conexion = ConexionBD.getConnection();
             var consulta = conexion.prepareStatement("SELECT COUNT(*) FROM pokemon WHERE nombre LIKE ?")) {
            consulta.setString(1, prefijo + "%");
            try (var filas = consulta.executeQuery()) {
                assertTrue(filas.next());
                assertEquals(3, filas.getInt(1));
            }
        }
        registros.forEach(p -> assertTrue(dao.eliminar(p)));
        assertEquals(idsIniciales, dao.obtenerTodos().stream().map(Pokemon::getId).toList());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "POKEMON_TEST_MODE", matches = "sin-conexion")
    void sinConexionInformaElProblemaYLaVentanaSigueActiva() throws Exception {
        enVentana(pantalla -> {
            assertTrue(pantalla.tabla().getItems().isEmpty());
            assertTrue(pantalla.mensaje().startsWith("No se pudo cargar los Pokémon."));
            pantalla.control("btnRecargar", Button.class).fire();
            assertTrue(pantalla.mensaje().contains("Comprueba que MySQL esté disponible"));
            pantalla.texto("txtNombre", "Marill");
            pantalla.texto("txtNivel", "15");
            pantalla.texto("txtPeso", "5.5");
            pantalla.control("btnGuardar", Button.class).fire();
            assertTrue(pantalla.mensaje().startsWith("No se pudo registrar el Pokémon."));
            assertEquals("Marill", pantalla.control("txtNombre", TextField.class).getText());
            assertTrue(pantalla.escenario().isShowing());
        });
        Pokemon objeto = new PokemonFuego("Prueba", 1, 1, false);
        objeto.setId(1);
        PokemonDAO dao = new PokemonDAOImpl();
        assertTrue(assertThrows(PersistenciaException.class, () -> dao.actualizar(objeto))
                .getMessage().startsWith("No se pudo actualizar el Pokémon."));
        assertTrue(assertThrows(PersistenciaException.class, () -> dao.eliminar(objeto))
                .getMessage().startsWith("No se pudo eliminar el Pokémon."));
    }

    private static void enVentana(java.util.function.Consumer<Pantalla> prueba) throws Exception {
        FutureTask<Void> tarea = new FutureTask<>(() -> {
            FXMLLoader cargador = new FXMLLoader(Main.class.getResource("view.fxml"));
            Parent raiz = cargador.load();
            Stage escenario = new Stage();
            try {
                Scene escena = new Scene(raiz, 650, 600);
                escena.getStylesheets().add(Main.class.getResource("css/styles.css").toExternalForm());
                escenario.setScene(escena);
                escenario.show();
                prueba.accept(new Pantalla(escenario, cargador));
            } finally {
                escenario.close();
            }
            return null;
        });
        Platform.runLater(tarea);
        tarea.get(45, TimeUnit.SECONDS);
    }

    private record Pantalla(Stage escenario, FXMLLoader cargador) {
        <T> T control(String id, Class<T> tipo) { return tipo.cast(cargador.getNamespace().get(id)); }

        @SuppressWarnings("unchecked")
        TableView<Pokemon> tabla() { return control("tblPokemones", TableView.class); }

        Pokemon pokemon(String nombre) {
            return tabla().getItems().stream().filter(p -> p.getNombre().equals(nombre)).findFirst().orElseThrow();
        }

        @SuppressWarnings("unchecked")
        String costo(String nombre) {
            TableColumn<Pokemon, String> columna = control("colCosto", TableColumn.class);
            return columna.getCellData(pokemon(nombre));
        }

        String mensaje() { return control("lblMensaje", Label.class).getText(); }

        void texto(String id, String valor) { control(id, TextField.class).setText(valor); }

        @SuppressWarnings("unchecked")
        void registrar(String tipo, String nombre, String nivel, String peso, String especie,
                       boolean especifico, boolean intercambio) {
            control("cmbTipo", ComboBox.class).setValue(tipo);
            texto("txtNombre", nombre);
            texto("txtNivel", nivel);
            texto("txtPeso", peso);
            texto("txtEspecie", especie);
            control("chkEspecifico", CheckBox.class).setSelected(especifico);
            control("chkIntercambio", CheckBox.class).setSelected(intercambio);
            control("btnGuardar", Button.class).fire();
            assertEquals("Pokémon registrado correctamente.", mensaje());
        }
    }
}
