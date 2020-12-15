package ru.mv.cv.quake.image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import ru.mv.cv.quake.capture.Capture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class ScanMatcher {

    private static final int BYTE_CONVERTER = 0xFF;

    private static final int START_ROW = 20;
    private static final int END_ROW = Capture.FRAME_HEIGHT - START_ROW;
    private static final int START_COL = 20;
    private static final int END_COL = Capture.FRAME_WIDTH - START_COL;

    // for 8 channel matrix, the value H is halved
    private static final int TARGET_HUE = 300;
    private static final int TARGET_HUE_RANGE = 3;
    private static final int MIN_HUE = TARGET_HUE - TARGET_HUE_RANGE;
    private static final int MAX_HUE = TARGET_HUE + TARGET_HUE_RANGE;
    private static final int TARGET_SATURATION = 240;
    private static final int TARGET_SATURATION_RANGE = 15;
    private static final int MIN_SATURATION = TARGET_SATURATION - TARGET_SATURATION_RANGE;
    private static final int MAX_SATURATION = TARGET_SATURATION + TARGET_SATURATION_RANGE;
    private static final int CONSECUTIVE_MATCH_COUNT = 3;
    private static final int TOTAL_MATCH_COUNT = 2;
    private static final int ROW_INCREMENT = 3;
    private static final int COL_INCREMENT = 3;

    private final TemplateMatcher templateMatcher;

    public ScanMatcher() {
        templateMatcher = new TemplateMatcher();
    }

    public Point findTargets(Mat frame) {
        var start = System.nanoTime();

        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGRA2RGB);
        Mat hsv = new Mat();
        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV);
        var cols = Math.min(hsv.cols(), END_COL);
        var rows = Math.min(hsv.rows(), END_ROW);
        var type = hsv.type();
        var channels = new byte[CvType.channels(type)];
        int x = START_COL;
        Collection<Point> points = new ArrayList<>();
        Point point = null;
        while (x < cols) {
            for (int y = START_ROW; y < rows; y += ROW_INCREMENT) {
                hsv.get(y, x, channels);
                var h = channels[0] & BYTE_CONVERTER;
                int hue = h * 2;
                int saturation = channels[1] & BYTE_CONVERTER;
                if (hue >= MIN_HUE && hue <= MAX_HUE && saturation >= MIN_SATURATION) {
                     point = findMatch(rgb, y, x);
                    if (point != null) {
                        break;
                    }
                }
            }
            if (point != null) {
                break;
            }
            x += COL_INCREMENT;
        }

        System.out.println("ScanMatcher: " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));
        return point;
    }

    private Point findMatch(Mat rgb, int row, int col) {
        var roi = new Rect(col - 16, row - 16, 32, 32);
        var region = new Mat(rgb, roi);
        var match = templateMatcher.findMatch(region);
        if (match != null) {
            return new Point(match.x + col - 16, match.y + row - 16);
        }
        return null;
    }
}