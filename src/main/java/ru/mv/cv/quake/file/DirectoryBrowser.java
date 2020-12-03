package ru.mv.cv.quake.file;

import java.io.File;

public class DirectoryBrowser {

    private File[] files;
    private int currentFileIndex;

    public void openDirectory(File directory) {
        if (!directory.isDirectory()) {
            return;
        }
        files = directory.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        if (files == null) {
            files = new File[0];
        }
        currentFileIndex = files.length - 1;
    }

    public boolean hasNext() {
        return currentFileIndex < files.length - 1;
    }

    public boolean hasPrevious() {
        return currentFileIndex > 0;
    }

    public File next() {
        return files[++currentFileIndex];
    }

    public File previous() {
        return files[--currentFileIndex];
    }
}
