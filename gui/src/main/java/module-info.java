module ru.redguy.gui.gui {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.jetbrains.annotations;

    opens ru.redguy.gui.gui to javafx.fxml;
    exports ru.redguy.gui.gui;
}