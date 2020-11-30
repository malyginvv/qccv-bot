package ru.mv.cv.quake.capture;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import ru.mv.cv.quake.image.TemplateMatcher;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

public class CaptureProcessor {

    private final Capture capture;
    private final AtomicReference<Mat> renderReference;
    private final int renderCoolDown;
    private long lastRender;
    private final TemplateMatcher templateMatcher;
    private final Scalar boxColor;

    public CaptureProcessor(Capture capture, AtomicReference<Mat> renderReference, int renderCoolDown) {
        this.capture = capture;
        this.renderReference = renderReference;
        this.renderCoolDown = renderCoolDown;
        this.templateMatcher = new TemplateMatcher();
        boxColor = new Scalar(25, 25, 255, 255);
    }

    public void processMatcher() {
        var frame = capture.grabFrame();
        if (frame == null) {
            return;
        }

        try {
            Point match = templateMatcher.findMatch(frame);
            if (needsRender()) {
                ForkJoinPool.commonPool().execute(() -> {
                    var frameToRender = frame.clone();
                    if (match != null && match.x > 50 && match.x < 670 && match.y > 50 && match.y < 1230) {
                        Imgproc.rectangle(frameToRender, match, new Point(match.x + 16, match.y + 16), boxColor, 2);
                    }
                    renderReference.set(frameToRender);
                    lastRender = System.nanoTime() / 1_000_000;
                });
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
