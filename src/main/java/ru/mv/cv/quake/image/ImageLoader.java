package ru.mv.cv.quake.image;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;

public class ImageLoader {

    public Mat loadImage(File file) {
        return Imgcodecs.imread(file.getAbsolutePath());
    }
}
