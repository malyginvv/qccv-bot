package ru.mv.cv.quake.capture;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import ru.mv.cv.quake.image.PointRenderer;
import ru.mv.cv.quake.image.TemplateMatcher;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

public class CaptureProcessor {

    private final Capture capture;
    private final AtomicReference<Mat> renderReference;
    private final int renderCoolDown;
    private long lastRender;
    private final TemplateMatcher templateMatcher;
    private final PointRenderer pointRenderer;

    public CaptureProcessor(Capture capture, AtomicReference<Mat> renderReference, int renderCoolDown) {
        this.capture = capture;
        this.renderReference = renderReference;
        this.renderCoolDown = renderCoolDown;
        this.templateMatcher = new TemplateMatcher();
        this.pointRenderer = new PointRenderer();
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
                    var frameToRender = frame;
                    if (match != null && match.x > 50 && match.x < 670 && match.y > 50 && match.y < 1230) {
                        frameToRender = pointRenderer.render(frame, match);
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
