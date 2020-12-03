package ru.mv.cv.quake.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.opencv.core.Mat;
import ru.mv.cv.quake.capture.Capture;
import ru.mv.cv.quake.image.ImageConverter;
import ru.mv.cv.quake.image.ImageLoader;
import ru.mv.cv.quake.processor.CaptureProcessor;
import ru.mv.cv.quake.processor.ImageProcessor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MainController extends AbstractController {

    @FXML
    private Button startButton;
    @FXML
    private Button loadImageButton;
    @FXML
    private ImageView currentFrame;

    private ScheduledExecutorService captureExecutor;
    private final Capture capture;
    private boolean cameraActive = false;
    private final int cameraId = 0;
    private final int renderCoolDown = 100;
    private final CaptureProcessor captureProcessor;
    private final ScheduledExecutorService renderExecutor;
    private final ImageConverter imageConverter;
    private final ImageProcessor imageProcessor;
    private final ImageLoader imageLoader;

    public MainController() {
        capture = new Capture();
        AtomicReference<Mat> renderReference = new AtomicReference<>();
        imageConverter = new ImageConverter();
        imageProcessor = new ImageProcessor();
        imageLoader = new ImageLoader();
        captureProcessor = new CaptureProcessor(capture, renderReference, renderCoolDown);
        renderExecutor = Executors.newSingleThreadScheduledExecutor();
        renderExecutor.scheduleAtFixedRate(() -> {
            try {
                var mat = renderReference.get();
                if (mat == null) {
                    return;
                }
                var image = imageConverter.convert(mat);
                updateImageView(currentFrame, image);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, renderCoolDown, TimeUnit.MILLISECONDS);
    }

    @FXML
    protected void startCamera(ActionEvent event) {
        if (cameraActive) {
            cameraActive = false;
            startButton.setText("Start Camera");
            loadImageButton.setDisable(false);
            stopAcquisition();
            return;
        }

        if (capture.open(cameraId)) {
            this.cameraActive = true;

            captureExecutor = Executors.newSingleThreadScheduledExecutor();
            captureExecutor.scheduleAtFixedRate(captureProcessor::processMatcher, 0, 16, TimeUnit.MILLISECONDS);

            // update the button content
            startButton.setText("Stop Camera");
            loadImageButton.setDisable(true);
        } else {
            // log the error
            System.err.println("Impossible to open the camera connection...");
        }
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition() {
        if (captureExecutor != null && !captureExecutor.isShutdown()) {
            try {
                // stop the timer
                captureExecutor.shutdown();
                captureExecutor.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        capture.release();
    }

    @FXML
    public void loadImage(ActionEvent event) {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Select a screen from Quake Champions");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("image files", "jpg", "png"));
        var file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            var mat = imageLoader.loadImage(file);
            var processed = imageProcessor.process(mat);
            var image = imageConverter.convert(processed);
            updateImageView(currentFrame, image);
        }
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view  the {@link ImageView} to update
     * @param image the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image) {
        Platform.runLater(() -> view.setImage(image));
    }

    @Override
    public void shutdown() {
        this.stopAcquisition();
        renderExecutor.shutdown();
    }
}
