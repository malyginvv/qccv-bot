package ru.mv.cv.quake;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nu.pattern.OpenCV;
import ru.mv.cv.quake.controller.StageAndShutdownAware;

import java.io.IOException;

public class App extends Application {

    private static final String MODE_PARAM_NAME = "mode";
    private static final String MODE_STREAM = "stream";
    private static final String MODE_VIEWER = "viewer";

    public static void main(String[] args) {
        OpenCV.loadLocally();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        var mode = System.getProperty(MODE_PARAM_NAME, MODE_STREAM);
        String fxml;
        if (mode.equals(MODE_VIEWER)) {
            fxml = "image-viewer.fxml";
        } else {
            fxml = "main.fxml";
        }

        var fxmlLoader = new FXMLLoader(App.class.getResource(fxml));
        Parent parent = fxmlLoader.load();
        StageAndShutdownAware controller = fxmlLoader.getController();

        var scene = new Scene(parent);
        primaryStage.setTitle("Quake Champions Computer Vision Project");
        primaryStage.setScene(scene);
        primaryStage.setOnHidden(windowEvent -> {
            controller.shutdown();
            Platform.exit();
        });
        controller.setStage(primaryStage);
        primaryStage.show();
    }
}
