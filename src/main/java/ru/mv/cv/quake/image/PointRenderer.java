package ru.mv.cv.quake.image;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Collection;

public class PointRenderer {

    private final Scalar boxColor;

    public PointRenderer() {
        boxColor = new Scalar(25, 25, 255, 255);
    }

    public Mat render(Mat frame, Point point) {
        var rendered = frame.clone();
        Imgproc.rectangle(rendered, point, new Point(point.x + 16, point.y + 16), boxColor, 2);
        return rendered;
    }

    public Mat render(Mat frame, Collection<Point> points) {
        var rendered = frame.clone();
        for (Point point : points) {
            Imgproc.rectangle(rendered, point, new Point(point.x + 16, point.y + 16), boxColor, 2);
        }
        return rendered;
    }
}
