package cl.nicolas.pokemon.controller;

import cl.nicolas.pokemon.repository.PersistenciaException;
import cl.nicolas.pokemon.repository.PokemonDAO;
import cl.nicolas.pokemon.repository.PokemonDAOImpl;
import cl.nicolas.pokemon.model.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Locale;

public class MainController {
    @FXML private ComboBox<String> cmbTipo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtNivel;
    @FXML private TextField txtPeso;
    @FXML private TextField txtEspecie;
    @FXML private Label lblEspecie;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private CheckBox chkEspecifico;
    @FXML private CheckBox chkIntercambio;

    @FXML private TableView<Pokemon> tblPokemones;
    @FXML private TableColumn<Pokemon, String> colTipo;
    @FXML private TableColumn<Pokemon, String> colNombre;
    @FXML private TableColumn<Pokemon, Integer> colNivel;
    @FXML private TableColumn<Pokemon, Double> colPeso;
    @FXML private TableColumn<Pokemon, String> colCosto; // String para aplicar formato $
    @FXML private TableColumn<Pokemon, String> colDetalle;

    // EP3: la vista solo accede al almacenamiento mediante este contrato.
    private final PokemonDAO pokemonDAO = new PokemonDAOImpl();
    private final ObservableList<Pokemon> listaPokemones = FXCollections.observableArrayList();
    private long idEnEdicion;

    @FXML
    public void initialize() {
        cmbTipo.setItems(FXCollections.observableArrayList("PokemonAgua", "PokemonFuego"));
        cmbTipo.getSelectionModel().selectFirst();

        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getClass().getSimpleName()));
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        colNivel.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getNivel()).asObject());
        colPeso.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getPeso()).asObject());

        // Muestra el costo con formato de moneda (ej: $15.000)
        colCosto.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format(Locale.forLanguageTag("es-CL"), "$%,.0f",
                        cell.getValue().calcularCostoCuracion()))
        );
        colDetalle.setCellValueFactory(cell -> new SimpleStringProperty(detalle(cell.getValue())));
        colDetalle.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacia) {
                super.updateItem(texto, vacia);
                setText(vacia ? null : texto);
                setTooltip(vacia || texto == null ? null : new Tooltip(texto));
            }
        });

        cmbTipo.setOnAction(e -> actualizarCamposSegunTipo());
        actualizarCamposSegunTipo();

        tblPokemones.setItems(listaPokemones);
        cargarDatos(null);
    }

    private void actualizarCamposSegunTipo() {
        boolean esAgua = "PokemonAgua".equals(cmbTipo.getValue());
        lblEspecie.setVisible(esAgua);
        lblEspecie.setManaged(esAgua);
        if (esAgua) {
            txtEspecie.setVisible(true);
            txtEspecie.setManaged(true);
            chkEspecifico.setText("¿Está Entrenado?");
            chkIntercambio.setVisible(true);
            chkIntercambio.setManaged(true);
        } else {
            txtEspecie.setVisible(false);
            txtEspecie.setManaged(false);
            chkEspecifico.setText("¿Es Shiny?");
            chkIntercambio.setVisible(false);
            chkIntercambio.setManaged(false);
        }
    }

    private static String detalle(Pokemon pokemon) {
        if (pokemon instanceof PokemonAgua agua) {
            String especie = agua.getEspecie();
            return (especie == null || especie.isBlank() ? "Sin especie" : especie)
                    + " · Entrenado: " + (agua.isEntrenado() ? "sí" : "no")
                    + " · Intercambio: " + (agua.estaDisponibleParaIntercambio() ? "sí" : "no");
        }
        return "Shiny: " + (((PokemonFuego) pokemon).isShiny() ? "sí" : "no");
    }

    private void cargarDatos(String confirmacion) {
        try {
            listaPokemones.setAll(pokemonDAO.obtenerTodos());
            mostrarMensaje(confirmacion == null ? "" : confirmacion, false);
        } catch (PersistenciaException e) {
            mostrarMensaje(confirmacion == null ? e.getMessage() : confirmacion + " " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleRecargar() {
        cargarDatos(null);
    }

    @FXML
    private void handleGuardar() {
        try {
            String nombre = txtNombre.getText().trim();
            int nivel = Integer.parseInt(txtNivel.getText().trim());
            double peso = Double.parseDouble(txtPeso.getText().trim().replace(',', '.'));
            if (!Double.isFinite(peso)) {
                throw new IllegalArgumentException("El peso debe ser un número finito.");
            }
            boolean especifico = chkEspecifico.isSelected();

            Pokemon nuevoPokemon;
            if ("PokemonAgua".equals(cmbTipo.getValue())) {
                String especie = txtEspecie.getText().trim();

                PokemonAgua agua = new PokemonAgua(nombre, nivel, peso, especie, especifico);

                if (chkIntercambio.isSelected()) {
                    agua.marcarDisponibleParaIntercambio();
                }
                nuevoPokemon = agua;
            } else {
                nuevoPokemon = new PokemonFuego(nombre, nivel, peso, especifico);
            }

            boolean editando = idEnEdicion != 0;
            nuevoPokemon.setId(idEnEdicion);
            boolean guardado = editando ? pokemonDAO.actualizar(nuevoPokemon) : pokemonDAO.guardar(nuevoPokemon);
            if (!guardado) {
                mostrarMensaje("No se pudo actualizar el Pokémon: el registro ya no está disponible.", true);
                return;
            }
            limpiarFormulario();
            cargarDatos(editando ? "Pokémon actualizado correctamente." : "Pokémon registrado correctamente.");
        } catch (NumberFormatException e) {
            mostrarMensaje("El nivel y el peso deben ser valores numéricos válidos.", true);
        } catch (IllegalArgumentException | PersistenciaException e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    @FXML
    private void handleEditar() {
        Pokemon seleccionado = tblPokemones.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Selecciona un Pokémon de la tabla para editar.", true);
            return;
        }

        idEnEdicion = seleccionado.getId();
        cmbTipo.setValue(seleccionado.getTipo());
        cmbTipo.setDisable(true);
        txtNombre.setText(seleccionado.getNombre());
        txtNivel.setText(Integer.toString(seleccionado.getNivel()));
        txtPeso.setText(Double.toString(seleccionado.getPeso()));
        if (seleccionado instanceof PokemonAgua agua) {
            txtEspecie.setText(agua.getEspecie() == null ? "" : agua.getEspecie());
            chkEspecifico.setSelected(agua.isEntrenado());
            chkIntercambio.setSelected(agua.estaDisponibleParaIntercambio());
        } else {
            txtEspecie.clear();
            chkEspecifico.setSelected(((PokemonFuego) seleccionado).isShiny());
            chkIntercambio.setSelected(false);
        }
        actualizarCamposSegunTipo();
        btnGuardar.setText("Guardar cambios");
        btnCancelar.setDisable(false);
        mostrarMensaje("Editando a " + seleccionado.getNombre() + ".", false);
    }

    @FXML
    private void handleCancelarEdicion() {
        limpiarFormulario();
        mostrarMensaje("Edición cancelada.", false);
    }

    @FXML
    private void handleEliminar() {
        Pokemon seleccionado = tblPokemones.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Selecciona un Pokémon de la tabla para eliminar.", true);
            return;
        }

        try {
            if (pokemonDAO.eliminar(seleccionado)) {
                if (idEnEdicion == seleccionado.getId()) {
                    limpiarFormulario();
                }
                cargarDatos("Pokémon eliminado correctamente.");
            } else {
                mostrarMensaje("No se pudo eliminar el Pokémon: el registro ya no está disponible.", true);
            }
        } catch (PersistenciaException e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    private void limpiarFormulario() {
        idEnEdicion = 0;
        cmbTipo.setDisable(false);
        btnGuardar.setText("Registrar Pokémon");
        btnCancelar.setDisable(true);
        txtNombre.clear();
        txtNivel.clear();
        txtPeso.clear();
        txtEspecie.clear();
        chkEspecifico.setSelected(false);
        chkIntercambio.setSelected(false);
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("error"), error);
    }
}
