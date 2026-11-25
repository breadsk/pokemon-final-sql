module cl.nicolas.pokemon {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens cl.nicolas.pokemon.controller to javafx.fxml;
    exports cl.nicolas.pokemon;
}
