package ru.mv.cv.quake.controller;

import javafx.stage.Stage;

public abstract class AbstractController implements StageAndShutdownAware {

    protected Stage stage;

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
