package ru.mv.cv.quake.processor;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import ru.mv.cv.quake.capture.Capture;
import ru.mv.cv.quake.image.ImageLogger;
import ru.mv.cv.quake.image.PointRenderer;
import ru.mv.cv.quake.image.ScanMatcher;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

public class CaptureProcessor {

    private static final int TRIGGER_BOX_WIDTH = 10;
    private static final int TRIGGER_BOX_HEIGHT = 20;
    private static final int TRIGGER_BOX_Y_OFFSET = 40;
    private static final int MAX_MOUSE_MOVEMENT = 30;
    private static final int MOUSE_RANDOM_RANGE = 3;
    private static final int MOUSE_MIN_RANDOM_VALUE = -MOUSE_RANDOM_RANGE / 2;
    private static final int SCREEN_CENTER_X = Capture.FRAME_WIDTH / 2;
    private static final int SCREEN_CENTER_Y = Capture.FRAME_HEIGHT / 2;

    private final Capture capture;
    private final AtomicReference<Mat> renderReference;
    private final int renderCoolDown;
    private long lastRender;
    private final ScanMatcher scanMatcher;
    private final PointRenderer pointRenderer;
    private final GameCommander gameCommander;
    private final Rect triggerBox;
    private final Random random;
    private final ImageLogger imageLogger;

    public CaptureProcessor(Capture capture, AtomicReference<Mat> renderReference, int renderCoolDown) {
        this.capture = capture;
        this.renderReference = renderReference;
        this.renderCoolDown = renderCoolDown;
        this.scanMatcher = new ScanMatcher();
        this.pointRenderer = new PointRenderer();
        this.gameCommander = new GameCommander();
        this.triggerBox = new Rect(SCREEN_CENTER_X - TRIGGER_BOX_WIDTH / 2, SCREEN_CENTER_Y - TRIGGER_BOX_HEIGHT / 2,
                TRIGGER_BOX_WIDTH, TRIGGER_BOX_HEIGHT);
        this.random = new Random();
        imageLogger = new ImageLogger();
    }

    public void processMatcher() {
        var frame = capture.grabFrame();
        if (frame == null) {
            return;
        }

        try {
            Point match = scanMatcher.findTargets(frame);
            if (match != null) {
                Point roughEnemyPosition = new Point(match.x + 8, match.y + TRIGGER_BOX_Y_OFFSET);
                if (roughEnemyPosition.inside(triggerBox)) {
                    // if enemy is somewhere near the screen center, fire
                    gameCommander.sendClick();
                } else {
                    // if enemy is not in the center, try to aim
                    double deltaX = roughEnemyPosition.x - SCREEN_CENTER_X;
                    double deltaY = roughEnemyPosition.y - SCREEN_CENTER_Y;
                    int moveX = Math.min((int) deltaX, MAX_MOUSE_MOVEMENT);
                    int moveY = Math.min((int) deltaY, MAX_MOUSE_MOVEMENT);
                    gameCommander.moveCursor(moveX, moveY);
                    //gameCommander.sendClick();
                }
            }
            if (needsRender()) {
                ForkJoinPool.commonPool().execute(() -> {
                    var frameToRender = frame;
                    if (match != null) {
                        frameToRender = pointRenderer.render(frame, match);
                        //imageLogger.logLater(new FrameData(frameToRender));
                    }
                    renderReference.set(frameToRender);
                    lastRender = System.nanoTime() / 1_000_000;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getMouseRandomNoise() {
        return MOUSE_MIN_RANDOM_VALUE + random.nextInt(MOUSE_RANDOM_RANGE);
    }

    private boolean needsRender() {
        var currentMillis = System.nanoTime() / 1_000_000;
        return currentMillis > lastRender + renderCoolDown;
    }
}
