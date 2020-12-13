package ru.mv.cv.quake.image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import ru.mv.cv.quake.capture.Capture;

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

    public Point findEnemy(Mat frame) {
        var start = System.nanoTime();

        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGRA2RGB);
        Mat hsv = new Mat();
        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV);
        var cols = Math.min(hsv.cols(), END_COL);
        var rows = Math.min(hsv.rows(), END_ROW);
        var type = hsv.type();
        var channels = new byte[CvType.channels(type)];
        int rowMatch = -1;
        int totalMatches = 0;
        int x = START_COL;
        Point point = null;
        while (x < cols) {
            int consecutiveMatches = 0;
            // start from starting row or from previous matching row if it's higher
            int startRow = Math.max(START_ROW, rowMatch - ROW_INCREMENT * (CONSECUTIVE_MATCH_COUNT - 1));
            for (int y = startRow; y < rows; y += ROW_INCREMENT) {
                hsv.get(y, x, channels);
                var h = channels[0] & BYTE_CONVERTER;
                int hue = h * 2;
                int saturation = channels[1] & BYTE_CONVERTER;
                if (hue >= MIN_HUE && hue <= MAX_HUE && saturation >= MIN_SATURATION) {
                    consecutiveMatches++;
                } else {
                    consecutiveMatches = consecutiveMatches > 0 ? consecutiveMatches - 1 : 0;
                }
                if (consecutiveMatches == CONSECUTIVE_MATCH_COUNT) {
                    // n consecutive matches - there is a candidate for a total match
                    rowMatch = y;
                    break;
                }
                //System.out.println(i + " " + Arrays.toString(channels));
            }
            if (rowMatch > 0) {
                // found a match - now we need to check the next column
                totalMatches++;
                x++;
            } else {
                // no match - can skip a couple of columns
                x += COL_INCREMENT;
                totalMatches = totalMatches > 0 ? totalMatches - 1 : 0;
            }
            if (totalMatches == TOTAL_MATCH_COUNT) {
                point = new Point(x, rowMatch);
                break;
            }
        }

        System.out.println("ScanMatcher: " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));
        return point;
    }
}
