package ru.mv.cv.quake.image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import ru.mv.cv.quake.capture.Capture;
import ru.mv.cv.quake.model.DebugData;
import ru.mv.cv.quake.model.EnemyData;
import ru.mv.cv.quake.model.FrameData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class ScanMatcher {

    private static final int BYTE_CONVERTER = 0xFF;

    private static final int START_ROW = 20;
    private static final int END_ROW = Capture.FRAME_HEIGHT - START_ROW;
    private static final int START_COL = 20;
    private static final int END_COL = Capture.FRAME_WIDTH - START_COL;

    // for 8 channel matrix, the value of Hue is halved
    private static final int TARGET_HUE = 300;
    private static final int TARGET_HUE_RANGE = 16;
    private static final int MIN_HUE = TARGET_HUE - TARGET_HUE_RANGE;
    private static final int MAX_HUE = TARGET_HUE + TARGET_HUE_RANGE;
    private static final int OUTLINE_HUE_RANGE = 86;
    private static final int OUTLINE_MIN_HUE = TARGET_HUE - OUTLINE_HUE_RANGE;
    private static final int OUTLINE_MAX_HUE = TARGET_HUE + OUTLINE_HUE_RANGE;
    private static final int TARGET_SATURATION = 255;
    private static final int TARGET_SATURATION_RANGE = 55;
    private static final int MIN_SATURATION = TARGET_SATURATION - TARGET_SATURATION_RANGE;
    private static final int OUTLINE_SATURATION_RANGE = 115;
    private static final int OUTLINE_MIN_SATURATION = TARGET_SATURATION - OUTLINE_SATURATION_RANGE;
    private static final int TARGET_VALUE = 255;
    private static final int TARGET_VALUE_RANGE = 55;
    private static final int MIN_VALUE = TARGET_VALUE - TARGET_VALUE_RANGE;
    private static final int OUTLINE_VALUE_RANGE = 115;
    private static final int OUTLINE_MIN_VALUE = TARGET_VALUE - OUTLINE_VALUE_RANGE;
    private static final int ROW_INCREMENT = 3;
    private static final int COL_INCREMENT = 3;
    private static final int ROW_SKIP_AFTER_MATCH = 100;
    private static final int COL_SKIP_AFTER_MATCH = 40;
    private static final int MAX_FLOOD_FILL_PIXELS = 46;
    private static final int FLOOD_FILL_TRIGGER = 26;
    private static final int MAX_FLOOD_FILL_OFFSET_X = 10;
    private static final int MAX_FLOOD_FILL_OFFSET_Y = 14;
    private static final int MAX_DISTANCE_TO_OUTLINE = 35;
    private static final int ENEMY_DEFAULT_HALF_WIDTH = 8;
    private static final int OUTLINE_THICKNESS = 3;

    public Collection<EnemyData> findEnemies(FrameData frameData, DebugData debugData) {
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
                debugData.addPoint(x, y);
                if (tipColorMatches(frameData.hsv, x, y, channels)) {
                    enemyData = findMatch(frameData.hsv, x, y, channels, debugData);
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

    private boolean tipColorMatches(Mat hsv, int x, int y, byte[] buffer) {
        return colorMatches(hsv, x, y, buffer, MIN_HUE, MAX_HUE, MIN_SATURATION, MIN_VALUE);
    }

    private boolean outlineColorMatches(Mat hsv, int x, int y, byte[] buffer) {
        return colorMatches(hsv, x, y, buffer, OUTLINE_MIN_HUE, OUTLINE_MAX_HUE, OUTLINE_MIN_SATURATION, OUTLINE_MIN_VALUE);
    }

    private boolean colorMatches(Mat hsv, int x, int y, byte[] buffer, int minHue, int maxHue, int minSaturation, int minValue) {
        hsv.get(y, x, buffer);
        var h = buffer[0] & BYTE_CONVERTER;
        int hue = h * 2; // the value of Hue is halved
        int saturation = buffer[1] & BYTE_CONVERTER;
        int value = buffer[2] & BYTE_CONVERTER;
        return hue >= minHue && hue <= maxHue && saturation >= minSaturation && value >= minValue;
    }

    private EnemyData findMatch(Mat hsv, int x, int y, byte[] buffer, DebugData debugData) {
        // try to step back
        int fromX = x - 1;
        while (fromX > x - COL_INCREMENT * 2 && tipColorMatches(hsv, fromX, y, buffer)) {
            fromX--;
            debugData.addPoint(fromX, y);
        }
        int fromY = y - 1;
        while (fromY > y - ROW_INCREMENT * 2 && tipColorMatches(hsv, fromX, fromY, buffer)) {
            fromY--;
            debugData.addPoint(fromX, fromY);
        }
        var tip = findTipUsingFloodFill(hsv, x, y, buffer, debugData);
        if (tip != null) {
            var startOfEnemyOutline = findStartOfEnemyOutline(hsv, tip, buffer, debugData);
            var enemyOutline = findEnemyOutline(hsv, startOfEnemyOutline, buffer, debugData);
            return new EnemyData(tip, enemyOutline);
        }
        return null;
    }

    private Point findTipUsingFloodFill(Mat hsv, int fromX, int fromY, byte[] buffer, DebugData debugData) {
        Queue<Point> points = new ArrayDeque<>(MAX_FLOOD_FILL_PIXELS);
        points.offer(new Point(fromX + 1, fromY));
        points.offer(new Point(fromX, fromY + 1));
        points.offer(new Point(fromX + 1, fromY + 1));
        Set<Point> visited = new HashSet<>();
        int filled = 0;
        int maxY = fromY;
        int minX = fromX;
        int maxX = fromX;
        while (!points.isEmpty() && filled <= MAX_FLOOD_FILL_PIXELS) {
            var point = points.poll();
            debugData.addPoint(point);
            if (tipColorMatches(hsv, (int) point.x, (int) point.y, buffer)) {
                if (visited.add(point)) {
                    filled++;
                }
                maxY = Math.max(maxY, (int) point.y);
                minX = Math.min(minX, (int) point.x);
                maxX = Math.max(maxX, (int) point.x);
                // try to flood only while moving right or down
                if (point.x <= fromX + MAX_FLOOD_FILL_OFFSET_X) {
                    points.offer(new Point(point.x + 1, point.y));
                }
                if (point.y <= fromY + MAX_FLOOD_FILL_OFFSET_Y) {
                    points.offer(new Point(point.x, point.y + 1));
                }
            }
        }
        return filled > FLOOD_FILL_TRIGGER
                ? new Point((minX + maxX) / 2.0, maxY)
                : null;
    }

    private Point findStartOfEnemyOutline(Mat hsv, Point start, byte[] buffer, DebugData debugData) {
        int x = (int) start.x;
        int y = (int) start.y;
        var yLimit = start.y + MAX_DISTANCE_TO_OUTLINE;
        // move down while color matches
        while (y < yLimit && outlineColorMatches(hsv, x, y, buffer)) {
            y++;
            debugData.addPoint(x, y);
        }
        if (y == yLimit) {
            // tall column of matching pixels, outline probably started at the top
            return start;
        }
        // move down until we hit an outline or travel farther than allowed
        while (y < yLimit && !outlineColorMatches(hsv, x, y, buffer)) {
            y++;
            debugData.addPoint(x, y);
        }
        if (y == yLimit) {
            // didn't find an outline, assume it's on the top
            return start;
        }
        return new Point(x, y);
    }

    private Rect findEnemyOutline(Mat hsv, Point start, byte[] buffer, DebugData debugData) {
        int startX = (int) start.x;
        int startY = (int) start.y + OUTLINE_THICKNESS;
        // (startX, startY) are coordinates of a point inside of the enemy outline
        // trace some lines from this point to different directions until we hit the outline or traver farther than allowed
        Collection<Point> outline = new ArrayList<>();
        outline.add(findOutline(hsv, startX, startY, buffer, 0, 2, debugData));  // straight down
        outline.add(findOutline(hsv, startX, startY, buffer, 1, 2, debugData));
        outline.add(findOutline(hsv, startX, startY, buffer, -1, 2, debugData));
        outline.add(findOutline(hsv, startX, startY, buffer, 1, 1, debugData));
        outline.add(findOutline(hsv, startX, startY, buffer, -1, 1, debugData));
        outline.add(findOutline(hsv, startX, startY, buffer, 2, 1, debugData));
        outline.add(findOutline(hsv, startX, startY, buffer, -2, 1, debugData));
        outline.add(findOutline(hsv, startX, startY, buffer, 2, 0, debugData));  // to the right
        outline.add(findOutline(hsv, startX, startY, buffer, -2, 0, debugData)); // to the left
        // find min and max x and y coordinates
        int minX = startX;
        int maxX = startX;
        int minY = startY;
        int maxY = startY;
        for (Point point : outline) {
            minX = (int) Math.min(minX, point.x);
            maxX = (int) Math.max(maxX, point.x);
            minY = (int) Math.min(minY, point.y);
            maxY = (int) Math.max(maxY, point.y);
        }
        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    private Point findOutline(Mat hsv, int fromX, int fromY, byte[] buffer, int deltaX, int deltaY, DebugData debugData) {
        var xUpperLimit = fromX + MAX_DISTANCE_TO_OUTLINE;
        var xLowerLimit = fromX - MAX_DISTANCE_TO_OUTLINE;
        var yLimit = fromY + MAX_DISTANCE_TO_OUTLINE;
        var x = fromX;
        var y = fromY;
        while (x <= xUpperLimit && x >= xLowerLimit && y <= yLimit && !outlineColorMatches(hsv, x, y, buffer)) {
            x += deltaX;
            y += deltaY;
            debugData.addPoint(x, y);
        }
        return new Point(x, y);
    }
}
