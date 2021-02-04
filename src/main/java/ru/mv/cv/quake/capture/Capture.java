package ru.mv.cv.quake.capture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import ru.mv.cv.quake.model.FrameData;

public class Capture {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final int FRAME_WIDTH = 1280;
    public static final int FRAME_HEIGHT = 720;

    private final VideoCapture videoCapture;

    public Capture() {
        videoCapture = new VideoCapture();
    }

    public boolean open(int cameraId) {
        try {
            videoCapture.open(cameraId);
            videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, FRAME_WIDTH);
            videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT);
            return videoCapture.isOpened();
        } catch (Exception e) {
            LOGGER.error("Error opening the camera", e);
        }
        return false;
    }

    /**
     * Get a frame from the opened video stream (if any).
     *
     * @return grabbed frame or {@code null} if the stream is not opened or an error occurred
     */
    public FrameData grabFrame() {
        if (!videoCapture.isOpened()) {
            return null;
        }

        FrameData frameData = null;
        try {
            Mat currentFrame = new Mat();
            videoCapture.read(currentFrame);
            if (!currentFrame.empty()) {
                frameData = new FrameData(currentFrame);
            }
        } catch (Exception e) {
            LOGGER.error("Error capturing a frame from the stream", e);
        }
        return frameData;
    }

    public void release() {
        try {
            if (videoCapture.isOpened()) {
                videoCapture.release();
            }
        } catch (Exception e) {
            LOGGER.error("Error releasing the camera", e);
        }
    }
}
