package ru.mv.cv.quake.capture;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import ru.mv.cv.quake.image.TemplateMatcher;

import java.util.Queue;

public class CaptureProcessor {

    private final Capture capture;
    private final CascadeClassifier cascadeClassifier;
    private final Queue<Mat> frameQueue;
    private final int renderCoolDown;
    private long lastRender;
    private final TemplateMatcher templateMatcher;

    public CaptureProcessor(Capture capture, Queue<Mat> frameQueue, int renderCoolDown) {
        this.capture = capture;
        this.cascadeClassifier = new CascadeClassifier();
        //this.cascadeClassifier.load(CaptureProcessor.class.getResource("pointer_haar.xml").getPath().substring(1));
        this.cascadeClassifier.load("H:\\workspace\\cv-quake\\src\\main\\resources\\pointer_haar.xml");
        this.frameQueue = frameQueue;
        this.renderCoolDown = renderCoolDown;
        this.templateMatcher = new TemplateMatcher();
    }

    public void process() {
        var frame = capture.grabFrame();
        if (frame == null) {
            return;
        }

        var pointers = new MatOfRect();
        try {
            cascadeClassifier.detectMultiScale(frame, pointers);
        } catch (Exception e) {
            e.printStackTrace();
        }
        var pointersCount = pointers.total();
        if (needsRender()) {
            // prepare Mat for render
            var frameToRender = frame.clone();
            if (pointersCount > 0) {
                var rects = pointers.toArray();
                for (Rect rect : rects) {
                    Imgproc.rectangle(frameToRender, rect, new Scalar(0, 255, 0));
                }
            }
            frameQueue.add(frameToRender);
            lastRender = System.nanoTime() / 1_000_000;
        }
    }

    public void processMatcher() {
        var frame = capture.grabFrame();
        if (frame == null) {
            return;
        }

        try {
            Point match = templateMatcher.findMatch(frame);
            if (needsRender()) {
                var frameToRender = frame.clone();
                if (match != null && match.x > 50 && match.x < 670 && match.y > 50 && match.y < 1230) {
                    System.out.println(match);
                    Imgproc.rectangle(frameToRender, match, new Point(match.x + 16, match.y + 16), new Scalar(255, 255, 0), 1);
                }
                frameQueue.add(frameToRender);
                lastRender = System.nanoTime() / 1_000_000;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean needsRender() {
        var currentMillis = System.nanoTime() / 1_000_000;
        return currentMillis > lastRender + renderCoolDown;
    }
}
