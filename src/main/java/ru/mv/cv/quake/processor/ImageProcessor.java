package ru.mv.cv.quake.processor;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import ru.mv.cv.quake.image.PointRenderer;
import ru.mv.cv.quake.image.ScanMatcher;
import ru.mv.cv.quake.image.TemplateMatcher;
import ru.mv.cv.quake.model.PixelData;

public class ImageProcessor {

    private final TemplateMatcher templateMatcher;
    private final ScanMatcher scanMatcher;
    private final PointRenderer pointRenderer;

    public ImageProcessor() {
        this.templateMatcher = new TemplateMatcher();
        this.scanMatcher = new ScanMatcher();
        this.pointRenderer = new PointRenderer();
    }

    public Mat process(Mat frame) {
        //var match = templateMatcher.findMatch(frame);
        var match = scanMatcher.findEnemy(frame);
        return match != null ? pointRenderer.render(frame, match) : frame;
    }

    public void setMatchQuality(double matchQuality) {
        templateMatcher.setMinMatchQuality(matchQuality);
    }

    public PixelData getPixelData(Mat frame, int x, int y) {
        var channels = new byte[3];
        frame.get(y, x, channels);
        Mat hsv = new Mat();
        Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_RGB2HSV);
        var hsvChannels = new byte[3];
        hsv.get(y, x, hsvChannels);
        return new PixelData(channels, hsvChannels);
    }
}
