package ru.redguy.gui.gui;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class HelloApplication extends Application {

    static ClientSocketThread clientSocketThread;
    @Override
    public void start(Stage stage) throws IOException {
        TableView<Person> table = new TableView(clientSocketThread.persons);
        table.setOnKeyReleased((event) -> {
            switch (event.getCode()) {
                case UP -> {
                    try {
                        clientSocketThread.up();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case DOWN -> {
                    try {
                        clientSocketThread.down();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        Scene scene = new Scene(table, 500, 500);
        scene.getRoot().setStyle("-fx-base:black");
        stage.setScene(scene);
        stage.show();

        TableColumn<Person, Integer> idColumn = new TableColumn<Person, Integer>("Id");
        idColumn.setCellValueFactory(new PropertyValueFactory<Person, Integer>("id"));
        table.getColumns().add(idColumn);

        TableColumn<Person, String> nameColumn = new TableColumn<Person, String>("First Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<Person, String>("firstName"));
        table.getColumns().add(nameColumn);

        TableColumn<Person, String> lastColumn = new TableColumn<Person, String>("Last Name");
        lastColumn.setCellValueFactory(new PropertyValueFactory<Person, String>("lastName"));
        table.getColumns().add(lastColumn);

        TableColumn<Person, Integer> ageColumn = new TableColumn<Person, Integer>("Age");
        ageColumn.setCellValueFactory(new PropertyValueFactory<Person, Integer>("age"));
        table.getColumns().add(ageColumn);

        table.setOnSort((event) -> {
            disableAll(scene.getRoot());
            try {
                if(table.getSortOrder().get(0) == null) {
                    clientSocketThread.sort("id", "asc");
                } else {
                    clientSocketThread.sort(switch (table.getSortOrder().get(0).getText()) {
                        case "First Name" -> "firstName";
                        case "Last Name" -> "lastName";
                        case "Age" -> "age";
                        default -> "id";
                    }, switch (table.getSortOrder().get(0).getSortType()) {
                        case ASCENDING -> "asc";
                        case DESCENDING -> "desc";
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                enableAll(scene.getRoot());
            }
        });
    }

    private void disableAll(Node node) {
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(this::disableAll);
        }
        node.setDisable(true);
    }

    private void enableAll(Node node) {
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(this::enableAll);
        }
        node.setDisable(false);
    }

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 3562);
        clientSocketThread = new ClientSocketThread(socket);
        new Thread(clientSocketThread).start();

        clientSocketThread.sendClientInfo();
        launch();
    }
}