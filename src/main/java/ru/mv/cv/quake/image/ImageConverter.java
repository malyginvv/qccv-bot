package ru.mv.cv.quake.image;

import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;

public class ImageConverter {

    public Image convert(Mat frame) {
        var start = System.nanoTime();
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        var result = new Image(new ByteArrayInputStream(buffer.toArray()));
        var end = System.nanoTime();
        //System.out.println("toImage: " + TimeUnit.MILLISECONDS.convert(end - start, TimeUnit.NANOSECONDS));
        return result;
    }
}
