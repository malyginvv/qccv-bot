package ru.mv.cv.quake.model;

import org.opencv.core.Mat;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;

public class FrameData {

    private final Mat frame;
    private final Temporal temporal;

    public FrameData(Mat frame) {
        this.frame = frame;
        this.temporal = LocalDateTime.now();
    }

    public Mat getFrame() {
        return frame;
    }

    public Temporal getTemporal() {
        return temporal;
    }
}
