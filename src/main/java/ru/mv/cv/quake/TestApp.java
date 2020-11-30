package ru.mv.cv.quake;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nu.pattern.OpenCV;

import java.io.IOException;

public class TestApp extends Application {

    public static void main(String[] args) {
        OpenCV.loadLocally();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        var fxmlLoader = new FXMLLoader(TestApp.class.getResource("sample.fxml"));
        Parent parent = fxmlLoader.load();
        MainController controller = fxmlLoader.getController();

        var scene = new Scene(parent);
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(scene);
        primaryStage.setOnHidden(windowEvent -> {
            controller.shutdown();
            Platform.exit();
        });
        primaryStage.show();
    }
}
