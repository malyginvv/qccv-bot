package ru.mv.cv.quake.processor;

import org.opencv.core.Mat;
import ru.mv.cv.quake.image.PointRenderer;
import ru.mv.cv.quake.image.TemplateMatcher;

public class ImageProcessor {

    private final TemplateMatcher templateMatcher;
    private final PointRenderer pointRenderer;

    public ImageProcessor() {
        this.templateMatcher = new TemplateMatcher();
        this.pointRenderer = new PointRenderer();
    }

    public Mat process(Mat frame) {
        var match = templateMatcher.findMatch(frame);
        return match != null ? pointRenderer.render(frame, match) : frame;
    }

    public void setMatchQuality(double matchQuality) {
        templateMatcher.setMinMatchQuality(matchQuality);
    }
}
