package ru.mv.cv.quake.capture;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Capture {

    private static final int FRAME_WIDTH = 1280;
    private static final int FRAME_HEIGHT = 720;

    private final VideoCapture videoCapture;

    public Capture() {
        videoCapture = new VideoCapture();
    }

    public boolean open(int cameraId) {
        videoCapture.open(cameraId);
        videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, FRAME_WIDTH);
        videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT);
        return videoCapture.isOpened();
    }

    /**
     * Get a frame from the opened video stream (if any).
     *
     * @return grabbed frame or {@code null} if stream is not opened
     */
    public Mat grabFrame() {
        if (!videoCapture.isOpened()) {
            return null;
        }
        var start = System.nanoTime();
        // init everything
        Mat frame = new Mat();
        try {
            // read the current frame
            Mat currentFrame = new Mat();
            this.videoCapture.read(currentFrame);
            // if the frame is not empty, process it
            if (!currentFrame.empty()) {
                Imgproc.cvtColor(currentFrame, frame, Imgproc.COLOR_BGR2BGRA);
            }
        } catch (Exception e) {
            // log the error
            System.err.println("Exception during the image elaboration: " + e);
        }
        //System.out.println("grabFrame: " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));
        return frame;
    }

    public void release() {
        if (videoCapture.isOpened()) {
            videoCapture.release();
        }
    }
}
