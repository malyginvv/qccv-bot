package ru.mv.cv.quake.model;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;

public class FrameData {

    /**
     * Original frame, probably BGR colorspace.
     */
    public final Mat frame;
    /**
     * Frame in RGB colorspace.
     */
    public final Mat rgb;
    /**
     * Frame in HSV colorspace.
     */
    public final Mat hsv;
    /**
     * Capture time.
     */
    public final Temporal temporal;

    public FrameData(Mat frame) {
        this.frame = frame;
        this.rgb = frame;
        this.hsv = new Mat();
        this.temporal = LocalDateTime.now();

        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_BGR2HSV);
    }
}
