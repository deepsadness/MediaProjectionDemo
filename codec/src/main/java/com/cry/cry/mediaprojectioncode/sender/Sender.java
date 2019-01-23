package com.cry.cry.mediaprojectioncode.sender;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.cry.cry.rtmp.rtmp.RESFlvData;
import com.cry.cry.rtmp.rtmp.RESRtmpSender;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Sender extends HandlerThread {
    private final Handler mSenderLooper;
    RESRtmpSender resRtmpSender;
    LinkedBlockingQueue<RESFlvData> blockingQueue = new LinkedBlockingQueue<RESFlvData>();
    private LoopSender loopSender;

    private static class HOLDER {
        private static Sender SINGLE = new Sender(new RESRtmpSender());
    }

    public static Sender getInstance() {
        return HOLDER.SINGLE;
    }

    private Sender(RESRtmpSender resRtmpSender) {
        super("RTMP_Sender");
        start();
        Looper looper = getLooper();
        mSenderLooper = new Handler(looper);
        this.resRtmpSender = resRtmpSender;
    }

    public void open(String url, int width, int height) {
//        mSenderLooper.post(new Runnable() {
//            @Override
//            public void run() {
                long open = resRtmpSender.rtmpOpen(url, width, height);
                Log.d("zzx", "open result=" + open);
//            }
//        });
    }

    public void close() {
        mSenderLooper.post(() -> {
            resRtmpSender.rtmpClose();
            Log.d("zzx", "rtmpClose");
            if (loopSender != null) {
                loopSender.interrupt();
            }
            quitSafely();
        });

    }

    public void rtmpSendFormat(MediaFormat newFormat) {
        //发送格式的话，还是直接发送吧
        mSenderLooper.post(() -> {
            resRtmpSender.rtmpSendFormat(newFormat);
            Log.d("zzx", "rtmpSendFormat");
            //输入成功之后，就需要开启发送的循环
            loopSender = new LoopSender();
            loopSender.start();
        });
    }

    public void rtmpSend(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
        RESFlvData realData = resRtmpSender.getRealData(info, outputBuffer);
        mSenderLooper.post(() -> {
            //加入队列
            try {
                blockingQueue.put(realData);
                Log.d("zzx", "加入队列");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        });
    }

    public class LoopSender extends Thread {
        @Override
        public void run() {
            super.run();

            while (true) {
                try {
                    RESFlvData take = blockingQueue.take();
                    //加入队列当中
                    resRtmpSender.rtmpPublish(take);
                    Log.d("zzx", "rtmpPublish");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}
