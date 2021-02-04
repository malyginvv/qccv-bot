package ru.mv.cv.quake.image;

import ru.mv.cv.quake.model.FrameData;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ImageLogger {

    private static final long PERIOD = 500;

    private final ConcurrentLinkedQueue<FrameData> queue;
    private final ImageSaver imageSaver;

    public ImageLogger() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this::processData, 0, PERIOD, TimeUnit.MILLISECONDS);
        queue = new ConcurrentLinkedQueue<>();
        imageSaver = new ImageSaver();
    }

    public void logLater(FrameData frameData) {
        queue.offer(frameData);
    }

    private void processData() {
        var frameData = queue.poll();
        if (frameData != null) {
            imageSaver.save(frameData.rgb, frameData.temporal);
        }
    }
}
