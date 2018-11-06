package com.cry.yuv;

public class YuvUtils {
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("yuv");
    }
    public static native boolean ConvertToI420(byte[] input, byte[] output, int width, int height);
}
