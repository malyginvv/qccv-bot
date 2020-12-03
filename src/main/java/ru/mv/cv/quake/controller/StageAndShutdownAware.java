package ru.mv.cv.quake.controller;

import javafx.stage.Stage;

public interface StageAndShutdownAware {

    void setStage(Stage stage);

    default void shutdown() {};
}
