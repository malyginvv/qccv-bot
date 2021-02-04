package ru.mv.cv.quake.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import ru.mv.cv.quake.capture.Capture;
import ru.mv.cv.quake.image.ImageConverter;
import ru.mv.cv.quake.processor.MainProcessor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MainController extends AbstractController {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int FPS = 100;

    @FXML
    private Button startButton;
    @FXML
    private ImageView currentFrame;

    private ScheduledExecutorService captureExecutor;
    private final Capture capture;
    private boolean cameraActive = false;
    private final int cameraId = 0;
    private final MainProcessor mainProcessor;
    private final ScheduledExecutorService renderExecutor;
    private final ImageConverter imageConverter;

    public MainController() {
        capture = new Capture();
        AtomicReference<Mat> renderReference = new AtomicReference<>();
        imageConverter = new ImageConverter();
        mainProcessor = new MainProcessor(capture, renderReference);
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
                LOGGER.error("Image rendering error", e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    @FXML
    protected void startCamera(ActionEvent event) {
        if (cameraActive) {
            cameraActive = false;
            startButton.setText("Start Camera");
            stopAcquisition();
            return;
        }

        if (capture.open(cameraId)) {
            this.cameraActive = true;

            captureExecutor = Executors.newSingleThreadScheduledExecutor();
            captureExecutor.scheduleAtFixedRate(mainProcessor::process, 0, 1_000_000 / FPS, TimeUnit.MICROSECONDS);

            startButton.setText("Stop Camera");
        } else {
            // log the error
            LOGGER.error("Impossible to open the camera connection");
        }
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition() {
        try {
            capture.release();
            if (captureExecutor != null && !captureExecutor.isShutdown()) {
                captureExecutor.shutdown();
            }
        } catch (Exception e) {
            LOGGER.error("Error stopping the acquisition from the camera", e);
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
