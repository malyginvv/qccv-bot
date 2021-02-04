package ru.mv.cv.quake.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import ru.mv.cv.quake.capture.Capture;
import ru.mv.cv.quake.image.PointRenderer;
import ru.mv.cv.quake.model.EnemyData;
import ru.mv.cv.quake.model.GameState;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

public class MainProcessor {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TRIGGER_BOX_WIDTH = 10;
    private static final int TRIGGER_BOX_HEIGHT = 20;
    private static final int MAX_MOUSE_MOVEMENT = 30;
    private static final int SCREEN_CENTER_X = Capture.FRAME_WIDTH / 2;
    private static final int SCREEN_CENTER_Y = Capture.FRAME_HEIGHT / 2;

    private final Capture capture;
    private final StateRecognizer stateRecognizer;
    private final AtomicReference<Mat> renderReference;
    private final PointRenderer pointRenderer;
    private final GameCommander gameCommander;
    private final Rect triggerBox;

    public MainProcessor(Capture capture, AtomicReference<Mat> renderReference) {
        this.capture = capture;
        this.stateRecognizer = new StateRecognizer();
        this.renderReference = renderReference;
        this.pointRenderer = new PointRenderer();
        this.gameCommander = new GameCommander();
        this.triggerBox = new Rect(SCREEN_CENTER_X - TRIGGER_BOX_WIDTH / 2, SCREEN_CENTER_Y - TRIGGER_BOX_HEIGHT / 2,
                TRIGGER_BOX_WIDTH, TRIGGER_BOX_HEIGHT);
    }

    public void process() {
        var frameData = capture.grabFrame();
        if (frameData == null) {
            return;
        }

        try {
            // main process: perform image recognition -> decide what to do -> send input (or do nothing)
            GameState gameState = stateRecognizer.recognize(frameData);

            boolean enemyInTriggerBox = false;
            Point closestEnemy = null;
            double minDistance = Integer.MAX_VALUE;
            for (EnemyData match : gameState.enemyData) {
                // calculate enemy position
                var enemyCenter = new Point(match.outline.x + match.outline.width / 2f, match.outline.y + match.outline.height / 2f);
                // can fire?
                enemyInTriggerBox |= enemyCenter.inside(triggerBox);
                // calculate distance to the center of the screen
                var dx = enemyCenter.x - SCREEN_CENTER_X;
                var dy = enemyCenter.y - SCREEN_CENTER_Y;
                double distanceToCenter = Math.sqrt(dx * dx + dy * dy);
                if (distanceToCenter < minDistance) {
                    minDistance = distanceToCenter;
                    closestEnemy = enemyCenter;
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
            }

            ForkJoinPool.commonPool().execute(() -> {
                var frameToRender = pointRenderer.render(frameData.frame, gameState.enemyData);
                renderReference.set(frameToRender);
            });
        } catch (Exception e) {
            LOGGER.error("Main process error", e);
        }
    }
}
