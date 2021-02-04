package ru.mv.cv.quake.image;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

public class ImageSaver {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DateTimeFormatter TEMPORAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm_ss.nnnnnn");
    private static final String FILE_NAME_PATTERN = "QC_%s.png";

    public void save(Mat frame, Temporal temporal) {
        try {
            var dateTime = TEMPORAL_FORMATTER.format(temporal);
            var filename = "H:\\workspace\\cv-quake-img\\logs\\" + String.format(FILE_NAME_PATTERN, dateTime);
            Imgcodecs.imwrite(filename, frame);
            System.out.println("saved file to " + filename);
        } catch (Exception e) {
            LOGGER.error("Error saving the frame", e);
        }
    }
}
