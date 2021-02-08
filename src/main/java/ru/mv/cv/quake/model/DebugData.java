package ru.mv.cv.quake.model;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Collection;

public class DebugData {

    public final Collection<Point> touchedPoints;

    public DebugData() {
        touchedPoints = new ArrayList<>();
    }

    public void addPoint(Point point) {
        touchedPoints.add(point);
    }

    public void addPoint(double x, double y) {
        touchedPoints.add(new Point(x, y));
    }
}
