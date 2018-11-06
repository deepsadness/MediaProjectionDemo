package com.cry.rtmp;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.cry.rtmp.sender.VideoSender;
import com.cry.rtmp.sender.tools.LogTools;

import java.nio.ByteBuffer;

public class RtmpClient {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private long jniRtmpPointer;
    private long startTime;

    public static native long open(String url);

    public static native int close(long rtmpPointer);

    public static native int write(long rtmpPointer, byte[] data, int size, int type, int ts);

    public static native String getIpAddr(long rtmpPointer);

    private RtmpClient() {
    }


    private static class HOLDER {
        private static RtmpClient SINGLE = new RtmpClient();
    }

    public static RtmpClient getInstance() {
        return HOLDER.SINGLE;
    }

    public long openUrl(String url) {
        jniRtmpPointer = open(url);
        return jniRtmpPointer;
    }

    public void closeUrl(String url) {
        close(jniRtmpPointer);
    }

    private void publish(RESFlvData flvData) {
        final int res = write(jniRtmpPointer, flvData.byteBuffer, flvData.byteBuffer.length, flvData.flvTagType, flvData.dts);
        LogTools.d("RtmpClient.write res=" + res + ", length =" + flvData.byteBuffer.length);
    }

    public void sendFormat(MediaFormat newFormat) {
        RESFlvData resFlvData = VideoSender.sendAVCDecoderConfigurationRecord(0, newFormat);
        publish(resFlvData);
    }

    public void sendData(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
        if (info.size == 0) {
            return;
        }
        if (startTime == 0) {
            startTime = info.presentationTimeUs / 1000;
        }
        //rtmp
        outputBuffer.position(info.offset + 4);
//        outputBuffer.position(info.offset);
        outputBuffer.limit(info.offset + info.size);
        RESFlvData resFlvData = VideoSender.sendRealData((info.presentationTimeUs / 1000) - startTime, outputBuffer);
        publish(resFlvData);
    }
}
