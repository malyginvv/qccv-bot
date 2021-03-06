package ru.mv.cv.quake.image;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.TimeUnit;

public class TemplateMatcher {

    private static final Logger LOGGER = LogManager.getLogger();

    private Mat template;
    private Mat mask;
    private double minMatchQuality;

    public TemplateMatcher() {
        try {
            //FIXME: OpenCV cannot load resources from inside the jar, we need to use awt toolkit for proper loading
            // https://stackoverflow.com/a/2393302
            template = Imgcodecs.imread("H:\\workspace\\cv-quake\\src\\main\\resources\\pointer.png", Imgcodecs.IMREAD_COLOR);
            mask = Imgcodecs.imread("H:\\workspace\\cv-quake\\src\\main\\resources\\mask.png", Imgcodecs.IMREAD_COLOR);
            minMatchQuality = 0.9;
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public Point findMatch(Mat frame) {
        var start = System.nanoTime();
        int resultCols = frame.cols() - template.cols() + 1;
        int resultRows = frame.rows() - template.rows() + 1;
        Mat result = new Mat(resultRows, resultCols, CvType.CV_32FC1);
        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGRA2RGB);
        Imgproc.matchTemplate(rgb, template, result, Imgproc.TM_CCORR_NORMED, mask);
        //Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        var acceptable = mmr.maxVal > minMatchQuality;
        System.out.println("findMatch: " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS));
        return acceptable ? mmr.maxLoc : null;
    }

    public void setMinMatchQuality(double minMatchQuality) {
        this.minMatchQuality = minMatchQuality;
    }
}
