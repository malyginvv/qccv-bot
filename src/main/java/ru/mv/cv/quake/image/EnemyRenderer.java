package ru.mv.cv.quake.image;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import ru.mv.cv.quake.model.DebugData;
import ru.mv.cv.quake.model.EnemyData;
import ru.mv.cv.quake.model.GameState;

import java.util.Collection;

public class EnemyRenderer {

    private final Scalar boxColor;
    private final Scalar debugColor;

    public EnemyRenderer() {
        boxColor = new Scalar(25, 25, 255, 255);
        debugColor = new Scalar(25, 255, 25, 255);
    }

    public Mat render(Mat frame, Point point) {
        var rendered = frame.clone();
        Imgproc.rectangle(rendered, point, new Point(point.x + 16, point.y + 16), boxColor, 2);
        return rendered;
    }

    public Mat render(Mat frame, Collection<EnemyData> enemyData, DebugData debugData) {
        var rendered = frame.clone();
        for (Point touchedPoint : debugData.touchedPoints) {
            Imgproc.rectangle(rendered, touchedPoint, touchedPoint, debugColor);
        }
        for (EnemyData enemy : enemyData) {
            if (enemy.outline != null) {
                Imgproc.rectangle(rendered, enemy.outline, boxColor, 2);
            } else {
                Imgproc.rectangle(rendered, new Point(enemy.indicatorTip.x - 3, enemy.indicatorTip.y - 3),
                        new Point(enemy.indicatorTip.x + 3, enemy.indicatorTip.y + 3), boxColor, 2);
            }
        }
        return rendered;
    }

    public Mat render(Mat frame, GameState gameState) {
        return render(frame, gameState.enemyData, gameState.debugData);
    }
}
