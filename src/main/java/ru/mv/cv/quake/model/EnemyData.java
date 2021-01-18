package ru.mv.cv.quake.model;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class EnemyData {

    public final Point indicatorTip;
    public final Rect outline;

    public EnemyData(Point indicatorTip, Rect outline) {
        this.indicatorTip = indicatorTip;
        this.outline = outline;
    }
}
