package ru.mv.cv.quake;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import ru.mv.cv.quake.capture.Capture;
import ru.mv.cv.quake.capture.CaptureProcessor;
import ru.mv.cv.quake.image.ImageConverter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController {

    @FXML
    private Button startButton;
    @FXML
    private ImageView currentFrame;

    private ScheduledExecutorService captureExecutor;
    private final Capture capture;
    private boolean cameraActive = false;
    private final int cameraId = 0;
    private final int renderCoolDown = 100;
    private final CaptureProcessor captureProcessor;
    private final ScheduledExecutorService renderExecutor;

    public MainController() {
        capture = new Capture();
        Queue<Mat> renderQueue = new ConcurrentLinkedQueue<>();
        ImageConverter imageConverter = new ImageConverter();
        captureProcessor = new CaptureProcessor(capture, renderQueue, renderCoolDown);
        renderExecutor = Executors.newSingleThreadScheduledExecutor();
        renderExecutor.scheduleAtFixedRate(() -> {
            try {
                var mat = renderQueue.poll();
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
            stopAcquisition();
            return;
        }

        if (capture.open(cameraId)) {
            this.cameraActive = true;

            captureExecutor = Executors.newSingleThreadScheduledExecutor();
            captureExecutor.scheduleAtFixedRate(captureProcessor::processMatcher, 0, 16, TimeUnit.MILLISECONDS);

            // update the button content
            startButton.setText("Stop Camera");
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

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view  the {@link ImageView} to update
     * @param image the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image) {
        Platform.runLater(() -> view.setImage(image));
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    public void shutdown() {
        this.stopAcquisition();
        renderExecutor.shutdown();
    }
}
