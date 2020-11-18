package ru.mv.cv.quake;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController {

    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;

    @FXML
    private Button startButton;
    @FXML
    private ImageView currentFrame;

    private ScheduledExecutorService timer;
    private final VideoCapture capture = new VideoCapture();
    private boolean cameraActive = false;
    private final int cameraId = 0;

    @FXML
    protected void startCamera(ActionEvent event) {
        if (cameraActive) {
            cameraActive = false;
            startButton.setText("Start Camera");
            stopAcquisition();
            return;
        }

        capture.open(cameraId);
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, FRAME_WIDTH);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT);
        if (this.capture.isOpened()) {
            this.cameraActive = true;

            // grab a frame every 33 ms (30 frames/sec)
            Runnable frameGrabber = () -> {
                // effectively grab and process a single frame
                Mat frame = grabFrame();
                // convert and show the frame
                Image imageToShow = toImage(frame);
                updateImageView(currentFrame, imageToShow);
            };

            timer = Executors.newSingleThreadScheduledExecutor();
            timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

            // update the button content
            startButton.setText("Stop Camera");
        } else {
            // log the error
            System.err.println("Impossible to open the camera connection...");
        }
    }

    private Image toImage(Mat frame) {
        var start = System.nanoTime();
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        var result = new Image(new ByteArrayInputStream(buffer.toArray()));
        var end = System.nanoTime();
        System.out.println("toImage: " + TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS));
        return result;
    }

    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Mat} to show
     */
    private Mat grabFrame() {
        var start = System.nanoTime();
        // init everything
        Mat frame = new Mat();
        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current frame
                Mat currentFrame = new Mat();
                this.capture.read(currentFrame);
                // if the frame is not empty, process it
                if (!currentFrame.empty()) {
                    Imgproc.cvtColor(currentFrame, frame, Imgproc.COLOR_BGR2BGRA);
                    //Imgproc.cvtColor(currentFrame, frame, Imgproc.COLOR_BGR2GRAY);
                }
            } catch (Exception e) {
                // log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }
        System.out.println("grabFrame: " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));
        return frame;
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition() {
        if (!timer.isShutdown()) {
            try {
                // stop the timer
                timer.shutdown();
                timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (capture.isOpened()) {
            // release the camera
            capture.release();
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

    /**
     * On application close, stop the acquisition from the camera
     */
    protected void setClosed() {
        this.stopAcquisition();
    }
}
