package ru.mv.cv.quake.processor;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import ru.mv.cv.quake.capture.Capture;
import ru.mv.cv.quake.image.ImageLogger;
import ru.mv.cv.quake.image.PointRenderer;
import ru.mv.cv.quake.image.ScanMatcher;

import java.util.ArrayList;
import java.util.Collection;
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
            Collection<Point> matches = scanMatcher.findTargets(frame);
            if (!matches.isEmpty()) {
                Collection<Point> roughEnemyPositions = new ArrayList<>();
                boolean enemyInTriggerBox = false;
                Point closestEnemy = null;
                double minDistance = Integer.MAX_VALUE;
                for (Point match : matches) {
                    // calculate enemy position
                    var roughEnemyPosition = new Point(match.x + 8, match.y + TRIGGER_BOX_Y_OFFSET);
                    // add position to enemies collection
                    roughEnemyPositions.add(roughEnemyPosition);
                    // can fire?
                    enemyInTriggerBox |= roughEnemyPosition.inside(triggerBox);
                    // calculate distance to the center of the screen
                    var dx = roughEnemyPosition.x - SCREEN_CENTER_X;
                    var dy = roughEnemyPosition.y - SCREEN_CENTER_Y;
                    double distanceToCenter = Math.sqrt(dx * dx + dy * dy);
                    if (distanceToCenter < minDistance) {
                        minDistance = distanceToCenter;
                        closestEnemy = roughEnemyPosition;
                    }
                }
                if (enemyInTriggerBox) {
                    // if enemy is somewhere near the screen center, fire
                    gameCommander.sendClick();
                } else if (closestEnemy != null) {
                    // if enemy is not in the center, try to aim
                    double deltaX = closestEnemy.x - SCREEN_CENTER_X;
                    double deltaY = closestEnemy.y - SCREEN_CENTER_Y;
                    int moveX = Math.min((int) deltaX, MAX_MOUSE_MOVEMENT);
                    int moveY = Math.min((int) deltaY, MAX_MOUSE_MOVEMENT);
                    gameCommander.moveCursor(moveX, moveY);
                    //gameCommander.sendClick();
                }
            }
            if (needsRender()) {
                ForkJoinPool.commonPool().execute(() -> {
                    var frameToRender = frame;
                    frameToRender = pointRenderer.render(frame, matches);
                    //imageLogger.logLater(new FrameData(frameToRender));
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
