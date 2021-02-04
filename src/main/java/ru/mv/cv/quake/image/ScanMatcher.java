package ru.mv.cv.quake.image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import ru.mv.cv.quake.capture.Capture;
import ru.mv.cv.quake.model.EnemyData;
import ru.mv.cv.quake.model.FrameData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

public class ScanMatcher {

    private static final int BYTE_CONVERTER = 0xFF;

    private static final int START_ROW = 20;
    private static final int END_ROW = Capture.FRAME_HEIGHT - START_ROW;
    private static final int START_COL = 20;
    private static final int END_COL = Capture.FRAME_WIDTH - START_COL;

    // for 8 channel matrix, the value of Hue is halved
    private static final int TARGET_HUE = 300;
    private static final int TARGET_HUE_RANGE = 3;
    private static final int MIN_HUE = TARGET_HUE - TARGET_HUE_RANGE;
    private static final int MAX_HUE = TARGET_HUE + TARGET_HUE_RANGE;
    private static final int TARGET_SATURATION = 255;
    private static final int TARGET_SATURATION_RANGE = 25;
    private static final int MIN_SATURATION = TARGET_SATURATION - TARGET_SATURATION_RANGE;
    private static final int TARGET_VALUE = 255;
    private static final int TARGET_VALUE_RANGE = 25;
    private static final int MIN_VALUE = TARGET_VALUE - TARGET_VALUE_RANGE;
    private static final int ROW_INCREMENT = 3;
    private static final int COL_INCREMENT = 3;
    private static final int ROW_SKIP_AFTER_MATCH = 100;
    private static final int COL_SKIP_AFTER_MATCH = 40;
    private static final int MAX_FLOOD_FILL_PIXELS = 40;
    private static final int FLOOD_FILL_TRIGGER = 36;
    private static final int MAX_FLOOD_FILL_OFFSET = 14;
    private static final int MAX_DISTANCE_TO_OUTLINE = 35;
    private static final int ENEMY_DEFAULT_HALF_WIDTH = 8;

    public Collection<EnemyData> findEnemies(FrameData frameData) {
        var cols = Math.min(frameData.hsv.cols(), END_COL);
        var rows = Math.min(frameData.hsv.rows(), END_ROW);
        var type = frameData.hsv.type();
        var channels = new byte[CvType.channels(type)];

        Collection<EnemyData> enemyDataCollection = new ArrayList<>();
        // the idea is simple: every enemy on screen in QC has a tip over its head, it looks like an inverted triangle
        // so we try to find these tips and enemy contours beneath them
        // check every nth pixel, when match is found skip some rows and columns
        // sometimes we can accidentally skip an enemy if it's too close to another enemy but that's ok
        // worst case: we check (Capture.FRAME_HEIGHT - START_ROW * Capture.FRAME_WIDTH - START_COL) / (ROW_INCREMENT * COL_INCREMENT) pixels
        // which is by default (700 * 1260) / (3 * 3) = 98000 pixels, shouldn't take more than 10 ms
        int x = START_COL;
        while (x < cols) {
            EnemyData enemyData = null;
            int y = START_ROW;
            while (y < rows) {
                if (colorMatch(frameData.hsv, x, y, channels)) {
                    enemyData = findMatch(frameData.hsv, y, x, channels);
                    if (enemyData != null) {
                        enemyDataCollection.add(enemyData);
                        y += ROW_SKIP_AFTER_MATCH;
                    }
                }
                y += ROW_INCREMENT;
            }
            if (enemyData != null) {
                x += COL_SKIP_AFTER_MATCH;
            }
            x += COL_INCREMENT;
        }

        return enemyDataCollection;
    }

    private boolean colorMatch(Mat hsv, int x, int y, byte[] buffer) {
        hsv.get(y, x, buffer);
        var h = buffer[0] & BYTE_CONVERTER;
        int hue = h * 2; // the value of Hue is halved
        int saturation = buffer[1] & BYTE_CONVERTER;
        int value = buffer[2] & BYTE_CONVERTER;
        return hue >= MIN_HUE && hue <= MAX_HUE && saturation >= MIN_SATURATION && value >= MIN_VALUE;
    }

    private EnemyData findMatch(Mat hsv, int row, int col, byte[] buffer) {
        // try to step back
        int x = col - 1;
        while (x > col - COL_INCREMENT && colorMatch(hsv, x, row, buffer)) {
            x--;
        }
        int y = row - 1;
        while (y > row - ROW_INCREMENT && colorMatch(hsv, x, y, buffer)) {
            y--;
        }
        var tip = findTipUsingFloodFill(hsv, x, y, buffer);
        if (tip != null) {
            var enemyOutline = findEnemyOutline(hsv, tip, buffer);
            return new EnemyData(tip, enemyOutline);
        }
        return null;
    }

    private Point findTipUsingFloodFill(Mat hsv, int fromX, int fromY, byte[] buffer) {
        Queue<Point> points = new ArrayDeque<>(MAX_FLOOD_FILL_PIXELS);
        points.offer(new Point(fromX + 1, fromY));
        points.offer(new Point(fromX, fromY + 1));
        points.offer(new Point(fromX + 1, fromY + 1));
        int filled = 0;
        int maxY = fromY;
        int minX = fromX;
        int maxX = fromX;
        while (!points.isEmpty() && filled <= MAX_FLOOD_FILL_PIXELS) {
            var point = points.poll();
            if (colorMatch(hsv, (int) point.x, (int) point.y, buffer)) {
                filled++;
                maxY = Math.max(maxY, (int) point.y);
                minX = Math.min(minX, (int) point.x);
                maxX = Math.max(maxX, (int) point.x);
                // try to flood while moving forward (right or down)
                if (point.x < fromX + MAX_FLOOD_FILL_OFFSET) {
                    points.offer(new Point(point.x + 1, point.y));
                }
                if (point.y < fromY + MAX_FLOOD_FILL_OFFSET) {
                    points.offer(new Point(point.x, point.y + 1));
                }
            }
        }
        return filled > FLOOD_FILL_TRIGGER
                ? new Point((minX + maxX) / 2.0, maxY)
                : null;
    }

    private Rect findEnemyOutline(Mat hsv, Point start, byte[] buffer) {
        int x = (int) start.x;
        int y = (int) start.y;
        var yLimit = start.y + MAX_DISTANCE_TO_OUTLINE;
        // move down while color matches
        while (y < yLimit && colorMatch(hsv, x, y, buffer)) {
            y++;
        }
        if (y == yLimit) {
            return new Rect(x - ENEMY_DEFAULT_HALF_WIDTH, (int) start.y, ENEMY_DEFAULT_HALF_WIDTH * 2, MAX_DISTANCE_TO_OUTLINE);
        }
        int colorEndY = y;
        // move down until we hit outline or travel farther than allowed
        while (y < yLimit && !colorMatch(hsv, x, y, buffer)) {
            y++;
        }
        return new Rect(x - ENEMY_DEFAULT_HALF_WIDTH, y == yLimit ? colorEndY : y, ENEMY_DEFAULT_HALF_WIDTH * 2, MAX_DISTANCE_TO_OUTLINE);
    }
}
