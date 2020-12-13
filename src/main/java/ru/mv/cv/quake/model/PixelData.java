package ru.mv.cv.quake.model;

public class PixelData {

    private final int[] rgb;
    private final int[] hsv;

    public PixelData(int[] rgb, int[] hsv) {
        this.rgb = rgb;
        this.hsv = hsv;
    }

    public PixelData(byte[] rgb, byte[] hsv) {
        this.rgb = convert(rgb);
        this.hsv = convert(hsv);
        this.hsv[0] *= 2;
    }

    private int[] convert(byte[] bytes) {
        var ints = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            ints[i] = bytes[i] & 0xFF;
        }
        return ints;
    }

    public int[] getRgb() {
        return rgb;
    }

    public int[] getHsv() {
        return hsv;
    }
}
