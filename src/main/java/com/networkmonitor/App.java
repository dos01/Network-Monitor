package com.networkmonitor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            scene = new Scene(loadFXML("layout"), 1000, 700);
            stage.setScene(scene);
            stage.setTitle("Network Monitor");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    @Override
    public void stop() {
        // Close database connection
        com.networkmonitor.service.DatabaseManager.getInstance().closeConnection();
        // Force exit to kill any background threads (like the executor in controller)
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
