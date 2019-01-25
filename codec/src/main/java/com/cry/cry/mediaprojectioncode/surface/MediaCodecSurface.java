package com.cry.cry.mediaprojectioncode.surface;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import com.cry.cry.mediaprojectioncode.SurfaceFactory;
import com.cry.cry.mediaprojectioncode.sender.Sender;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR;
import static android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ;
import static android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR;

/**
 * 关于
 * 关键帧。
 * FRAME_RATE = 15;  IFRAME_INTERVAL = 2;    BIT_RATE = 2 * 1024 * 1024 * 3
 *
 * 在华为P20上，
 * 滑动的时候是  500ms 一个关键字。
 * 如果基本不滑动的时候 是 14000 ms~15000ms 一起。
 *
 * 当设置 FRAME_RATE 30,其他不变时。
 * 滑动的时候是  1000ms 一个关键字。
 * 如果基本不滑动的时候 是 24000 ms~25000ms 一起。
 *
 * 当设置 IFRAME_INTERVAL 1,其他不变时。
 * 滑动的时候是  250ms 一个关键字。
 * 如果基本不滑动的时候 是 7000 ms~8000ms 一起。
 *
 * 也就是说 内部做了优化，只有图像变化巨大的时候，才会进行编码I帧吗？
 *
 */
public class MediaCodecSurface implements SurfaceFactory {
    private static final String TAG = "MediaCodecSurface";
    private static final boolean VERBOSE = true;

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 15;               // 30fps
    private static final int IFRAME_INTERVAL = 2;           //  seconds between I-frames
    //    private static final int BIT_RATE = 800_000;
//    private static final int BIT_RATE = 480 * 720 * 3;
//    private static final int BIT_RATE = 480 * 720 * 3 * FRAME_RATE;
    private static final int BIT_RATE = 2 * 1024 * 1024 * 3;           // 2M

    private Surface mInputSurface;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private volatile boolean mIsStopRequested;
    private Handler workHanlder;
    private long lastNesc;
    private long lastKeyFrameNesc;

    public MediaCodecSurface() {
        createEncoderThread();
    }

    private void createEncoderThread() {
        HandlerThread encoder = new HandlerThread("Encoder");
        encoder.start();
        Looper looper = encoder.getLooper();
        workHanlder = new Handler(looper);
    }

    @Override
    public @Nullable
    Surface createSurface(int width, int height) {
        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        //还需要对器进行插值。设置自己设置的一些变量
        //设置ColorFormat??
//        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
//        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        //对profile进行插值
//        if (codecProfileLevel != null && codecProfileLevel.profile != 0 && codecProfileLevel.level != 0) {
//            format.setInteger(MediaFormat.KEY_PROFILE, codecProfileLevel.profile);
//            format.setInteger("level", codecProfileLevel.level);
//        }
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //使用BITRATE
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, BITRATE_MODE_VBR);

        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mEncoder.createInputSurface();
            mEncoder.start();

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                //异步的方式
//                mEncoder.setCallback(new MediaCodec.Callback() {
//                    @Override
//                    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
//                        // not important for us, since we're using Surface
//
//                    }
//
//                    @Override
//                    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
//                        // not important for us, since we're using Surface
//
//                    }
//
//                    @Override
//                    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
//
//                    }
//
//                    @Override
//                    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
//
//                    }
//                }, workHanlder);
//            } else {
            workHanlder.postDelayed(() -> doExtract(mEncoder, new FrameCallback() {
                @Override
                public void render(MediaCodec.BufferInfo info, ByteBuffer outputBuffer) {
                    //这里的Buffer，应该使用队列来进行缓存
                    Sender.getInstance().rtmpSend(info, outputBuffer);
                }

                @Override
                public void formatChange(MediaFormat mediaFormat) {
                    //应该这个mediaFormat直接发送了。
                    Sender.getInstance().rtmpSendFormat(mediaFormat);
                }
            }), 1000);
            return mInputSurface;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void stop() {
        mIsStopRequested = true;
        workHanlder.post(() -> {
            mEncoder.signalEndOfInputStream();
            mEncoder.stop();
        });
    }

    /**
     * 不断循环获取，直到我们手动结束.同步的方式
     *
     * @param encoder       编码器
     * @param frameCallback 获取的回调
     */
    private void doExtract(MediaCodec encoder,
                           FrameCallback frameCallback) {
        final int TIMEOUT_USEC = 10000;
        long firstInputTimeNsec = -1;
        boolean outputDone = false;
        while (!outputDone) {
//            if (VERBOSE) Log.d(TAG, "loop");
            if (mIsStopRequested) {
                Log.d(TAG, "Stop requested");
                return;
            }

            int decoderStatus = encoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
//                if (VERBOSE) Log.d(TAG, "no output from decoder available");
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not important for us, since we're using Surface
//                if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = encoder.getOutputFormat();
                if (VERBOSE) Log.d(TAG, "decoder output format changed: " + newFormat);
                if (frameCallback != null) {
                    frameCallback.formatChange(newFormat);
                }
                //设置
//                Bundle param = new Bundle();
//                param.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, BIT_RATE);
//                mEncoder.setParameters(param);
            } else if (decoderStatus < 0) {
                throw new RuntimeException(
                        "unexpected result from decoder.dequeueOutputBuffer: " +
                                decoderStatus);
            } else { // decoderStatus >= 0
                if (firstInputTimeNsec != 0) {
                    // Log the delay from the first buffer of input to the first buffer
                    // of output.
                    long nowNsec = System.nanoTime();
                    Log.d(TAG, "startup lag " + ((nowNsec - firstInputTimeNsec) / 1000000.0) + " ms");
                    firstInputTimeNsec = 0;
                }
                if (VERBOSE) Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                        " (size=" + mBufferInfo.size + ")");
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "output EOS");
                    outputDone = true;
                }


//                requestInterval();
//                boolean keyFrame = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                    long nowNsec = System.nanoTime();
                    if (lastNesc == 0) {
                        lastNesc = nowNsec;
                    }
                    long diff = nowNsec - lastNesc;
                    lastNesc = nowNsec;
                    //这种是关键帧
                    Log.d("zzx", "MediaCodec.BUFFER_FLAG_KEY_FRAME ,diff time = " + (diff / 1000000.0) + " ms");
                }

                boolean doRender = (mBufferInfo.size != 0);

                if (doRender && frameCallback != null) {
                    ByteBuffer outputBuffer = encoder.getOutputBuffer(decoderStatus);
                    frameCallback.render(mBufferInfo, outputBuffer);
                }
                encoder.releaseOutputBuffer(decoderStatus, doRender);
            }
        }
    }

    //在真机上是可以用的。。。泪目。。
    //通过这个方式后，上面的设置就没用了。
    private void requestInterval() {
        long nowNsec = System.nanoTime();
        if (lastKeyFrameNesc == 0) {
            lastKeyFrameNesc = nowNsec;
        }
        long diff = nowNsec - lastKeyFrameNesc;
        if (diff / 1000000.0 > 1000 * 5) { //5s
            Log.d("zzx", "PARAMETER_KEY_REQUEST_SYNC_FRAME ,diff =" + (diff / 1000000.0));
            Bundle bundle = new Bundle();
            bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
            mEncoder.setParameters(bundle);
            lastKeyFrameNesc = nowNsec;
        }
    }

    public interface FrameCallback {
        void render(MediaCodec.BufferInfo info, ByteBuffer outputBuffer);

        void formatChange(MediaFormat mediaFormat);
    }
}
