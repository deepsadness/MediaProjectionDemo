package com.cry.cry.mediaprojectioncode.sender;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.cry.cry.rtmp.rtmp.RESFlvData;
import com.cry.cry.rtmp.rtmp.RESRtmpSender;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sender {
    RESRtmpSender resRtmpSender;
    private final ExecutorService executorService;

    private static class HOLDER {
        private static Sender SINGLE = new Sender(new RESRtmpSender(), Executors.newFixedThreadPool(1));
    }

    public static Sender getInstance() {
        return HOLDER.SINGLE;
    }

    public Sender(RESRtmpSender resRtmpSender, ExecutorService executorService) {
        this.resRtmpSender = resRtmpSender;
        this.executorService = executorService;
    }

    public void open(String url, int width, int height) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                long open = resRtmpSender.rtmpOpen(url, width, height);
                Log.d("zzx", "open result=" + open);
            }
        });
    }

    public void close() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                resRtmpSender.rtmpClose();
                Log.d("zzx", "rtmpClose");
            }
        });
    }

    public void rtmpSendFormat(MediaFormat newFormat) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                resRtmpSender.rtmpSendFormat(newFormat);
                Log.d("zzx", "rtmpSendFormat");
            }
        });
    }

    public void rtmpSend(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
        RESFlvData realData = resRtmpSender.getRealData(info, outputBuffer);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                resRtmpSender.rtmpPublish(realData);
                Log.d("zzx", "rtmpPublish");
            }
        });
    }
}
