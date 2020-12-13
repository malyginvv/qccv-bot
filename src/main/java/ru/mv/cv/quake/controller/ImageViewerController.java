package ru.mv.cv.quake.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import org.opencv.core.Mat;
import ru.mv.cv.quake.file.DirectoryBrowser;
import ru.mv.cv.quake.image.ImageConverter;
import ru.mv.cv.quake.image.ImageLoader;
import ru.mv.cv.quake.processor.ImageProcessor;

import java.io.File;
import java.util.Arrays;

public class ImageViewerController extends AbstractController {

    @FXML
    private Button openDirButton;
    @FXML
    public Label fileLabel;
    @FXML
    public ImageView imageView;
    @FXML
    public Button backButton;
    @FXML
    public Button nextButton;
    @FXML
    public Slider minQualitySlider;
    @FXML
    public Label minQualityLabel;
    @FXML
    public Label imageInfoLabel;

    private final DirectoryBrowser directoryBrowser = new DirectoryBrowser();
    private final ImageLoader imageLoader = new ImageLoader();
    private final ImageProcessor imageProcessor = new ImageProcessor();
    private final ImageConverter imageConverter = new ImageConverter();
    private File current;
    private Mat currentMat;
    private Mat currentProcessed;
    private Image currentImage;

    @FXML
    public void initialize() {
        minQualitySlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            imageProcessor.setMatchQuality(newValue.doubleValue() / 100);
            minQualityLabel.setText(Double.toString(newValue.intValue()));
            processAndUpdateImage();
        });
        imageView.setOnMouseClicked(this::onMouseClick);
    }

    @FXML
    public void openDir(ActionEvent event) {
        var directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a directory with Quake Champions screenshots");
        directoryChooser.setInitialDirectory(new File("H:\\workspace\\cv-quake-img\\training\\positive\\img"));
        var file = directoryChooser.showDialog(stage);
        if (file != null) {
            directoryBrowser.openDirectory(file);
            updateButtonStates();
            if (directoryBrowser.hasNext()) {
                nextFile();
            }
        }
    }

    @FXML
    public void onNext(ActionEvent event) {
        nextFile();
    }

    @FXML
    public void onPrevious(ActionEvent event) {
        current = directoryBrowser.previous();
        updateMatsAndButtons();
    }

    public void onMouseClick(MouseEvent event) {
        Platform.runLater(() -> {
            var pixelData = imageProcessor.getPixelData(currentMat, (int) event.getX(), (int) event.getY());
            imageInfoLabel.setText("RGB: " + Arrays.toString(pixelData.getRgb())
                    + " HSV: " + Arrays.toString(pixelData.getHsv()));
        });
    }

    private void nextFile() {
        current = directoryBrowser.next();
        updateMatsAndButtons();
    }

    private void updateMatsAndButtons() {
        fileLabel.setText(current.getAbsolutePath());
        currentMat = imageLoader.loadImage(current);
        processAndUpdateImage();
        updateButtonStates();
    }

    private void processAndUpdateImage() {
        currentProcessed = imageProcessor.process(currentMat);
        currentImage = imageConverter.convert(currentProcessed);
        imageView.setImage(currentImage);
    }

    private void updateButtonStates() {
        backButton.setDisable(!directoryBrowser.hasPrevious());
        nextButton.setDisable(!directoryBrowser.hasNext());
    }
}
