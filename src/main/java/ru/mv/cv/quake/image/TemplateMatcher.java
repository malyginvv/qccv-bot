package ru.mv.cv.quake.image;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class TemplateMatcher {

    private Mat template;
    private Mat mask;
    private double minMatchQuality;

    public TemplateMatcher() {
        try {
            template = Imgcodecs.imread("H:\\workspace\\cv-quake\\src\\main\\resources\\pointer.png", Imgcodecs.IMREAD_COLOR);
            mask = Imgcodecs.imread("H:\\workspace\\cv-quake\\src\\main\\resources\\mask.png", Imgcodecs.IMREAD_COLOR);
            minMatchQuality = 0.9;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Point findMatch(Mat frame) {
        int resultCols = frame.cols() - template.cols() + 1;
        int resultRows = frame.rows() - template.rows() + 1;
        Mat result = new Mat(resultRows, resultCols, CvType.CV_32FC1);
        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGRA2RGB);
        Imgproc.matchTemplate(rgb, template, result, Imgproc.TM_CCORR_NORMED, mask);
        //Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        var acceptable = mmr.maxVal > minMatchQuality;
        return acceptable ? mmr.maxLoc : null;
    }

    public void setMinMatchQuality(double minMatchQuality) {
        this.minMatchQuality = minMatchQuality;
    }
}
