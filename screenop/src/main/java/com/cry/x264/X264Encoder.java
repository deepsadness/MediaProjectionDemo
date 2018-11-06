package com.cry.x264;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class X264Encoder {
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("x264");
        System.loadLibrary("yuv");
//        initCacheBuffer();
        init(0x0100);
        setVideoSize(480, 720);
        setBitrate(480 * 720 * 3);
        setFrameFormat(0x0001);
        setFps(15);
    }

    private static final String PRESET = "veryfast";
    private static final String TUNE = "zerolatency";

    private static final String CSD_0 = "csd-0";
    private static final String CSD_1 = "csd-1";

    private static final int BUFFER_FLAG_KEY_FRAME = 1;
    private static final int BUFFER_FLAG_CODEC_CONFIG = 2;
    private static final int BUFFER_FLAG_END_OF_STREAM = 4;
    private static final int BUFFER_FLAG_PARTIAL_FRAME = 8;

    private boolean isStart;
    private ByteBuffer buffer = initBuffer();
    private int[] size = new int[1];
    private int[] type = new int[1];
    private long mTotalCost;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private MediaFormat outFormat;

    private ByteBuffer initBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(480 * 720);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    private int mFrameCount;

    public MediaFormat getOutFormat() {
        return outFormat;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }

    private X264Encoder() {
    }

    public MediaCodec.BufferInfo encode(byte[] src) {
        final long time = System.currentTimeMillis();
        ++mFrameCount;
        buffer.clear();
        buffer.position(0);

        boolean result = encode(src, buffer.array(), size, type);
        if (!result) {
            Log.e("Encoder", "Encode failed. size = ${size[0]}");
            return null;
        }
        final long cost = System.currentTimeMillis() - time;
        mTotalCost += cost;
        if (0 == mFrameCount % 20) {
            Log.d("Encoder", "x264 frame size = ${size[0]}, cost ${cost}ms, arg cost ${mTotalCost / mFrameCount}ms");
        }
        return wrapBufferInfo(size[0]);
    }

    private MediaCodec.BufferInfo wrapBufferInfo(int size) {
        if (mBufferInfo == null) {
            mBufferInfo = new MediaCodec.BufferInfo();
        }
        switch (type[0]) {
            case -1:
                mBufferInfo.flags = BUFFER_FLAG_CODEC_CONFIG;
                break;
            case 1:
                mBufferInfo.flags = BUFFER_FLAG_KEY_FRAME;//X264_TYPE_IDR
                break;
            case 2:
                mBufferInfo.flags = BUFFER_FLAG_KEY_FRAME;//X264_TYPE_I
                break;
            default:
                mBufferInfo.flags = 0;
        }
        mBufferInfo.size = size;

        if (BUFFER_FLAG_CODEC_CONFIG == mBufferInfo.flags) {
            //获取SPS，PPS
            getOutFormat(mBufferInfo, buffer);
            return mBufferInfo;
        } else {
            buffer.position(0);
            buffer.limit(size);
            return mBufferInfo;
        }
    }

    private void getOutFormat(MediaCodec.BufferInfo info, ByteBuffer data) {
        data.position(0);
        byte[] specialData = new byte[info.size];
        data.get(specialData, 0, specialData.length);
        outFormat = new MediaFormat();
        outFormat.setString(MediaFormat.KEY_MIME, "video/avc");
        outFormat.setInteger(MediaFormat.KEY_WIDTH, 480);
        outFormat.setInteger(MediaFormat.KEY_HEIGHT, 720);
        outFormat.setInteger(MediaFormat.KEY_BIT_RATE, 480 * 720 * 3);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outFormat.setInteger(MediaFormat.KEY_COLOR_RANGE, 2);
            outFormat.setInteger(MediaFormat.KEY_COLOR_STANDARD, 4);
            outFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER, 3);
        }

        ArrayList<byte[]> spsAndPps = parseSpecialData(specialData);
        if (spsAndPps == null) {
            throw new RuntimeException("Special data is empty");
        }
        int ppsLength = spsAndPps.get(0).length;
        outFormat.setByteBuffer(CSD_0, ByteBuffer.wrap(spsAndPps.get(0)));
        outFormat.setByteBuffer(CSD_1, ByteBuffer.wrap(spsAndPps.get(1)));
    }

    private ArrayList<byte[]> parseSpecialData(byte[] specialData) {
        int index = 0;
        for (int i = 4; i < specialData.length - 4; i++) {
            boolean flag = isFlag(specialData, i);
            if (flag) {
                index = i;
                break;
            }
        }
        if (index == 0) {
            return null;
        }
        byte[] sps = new byte[index];
        byte[] pps = new byte[specialData.length - index];
        System.arraycopy(specialData, 0, sps, 0, index);
        System.arraycopy(specialData, index, pps, 0, pps.length);
        ArrayList<byte[]> result = new ArrayList<>(2);
        result.add(sps);
        result.add(pps);
        return result;
    }

    private boolean isFlag(byte[] specialData, int index) {
        return 0 == specialData[index]
                && 0 == specialData[index + 1]
                && 0 == specialData[index + 2]
                && 1 == specialData[index + 3];
    }

    private static class HOLDER {
        private static X264Encoder SINGLE = new X264Encoder();
    }

    public static X264Encoder getInstance() {
        return HOLDER.SINGLE;
    }

    private static native void init(int fmt);

    private static native void setVideoSize(int width, int height);

    private static native void setBitrate(int bitrate);

    private static native void setFrameFormat(int format);

    private static native void setFps(int fps);

    private static native void setProfile(String profile);

    private static native void setLevel(int level);

    public static native void start();

    public static native void stop();

    public static native boolean encode(byte[] src, byte[] dest, int[] size, int[] type);

}
