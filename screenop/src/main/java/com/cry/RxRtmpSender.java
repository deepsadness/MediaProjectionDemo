package com.cry;

import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.support.v4.app.FragmentActivity;

import com.cry.rtmp.RtmpClient;
import com.cry.screenop.ImageReaderAvailableObservable;
import com.cry.screenop.MediaProjectionHelper;
import com.cry.screenop.RxScreenShot;
import com.cry.x264.X264Encoder;
import com.cry.yuv.YuvUtils;

import java.nio.ByteBuffer;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;

public class RxRtmpSender {
    private static boolean isConnect = false;

    public static Observable<Object> wrap(String url, FragmentActivity activity) {
        return Observable
                .just(url)
                .observeOn(Schedulers.single())
                .map(s -> {
                    long l = RtmpClient.getInstance().openUrl(url);
                    isConnect = (l != 0);
                    return l;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(aLong -> boardCast(activity, url));
    }

    private static Observable<Object> boardCast(FragmentActivity activity, String url) {
        return MediaProjectionHelper
                .requestCapture(activity)
                .map(mediaProjection -> RxScreenShot.of(mediaProjection).createImageReader())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<RxScreenShot, ObservableSource<ImageReader>>) RxScreenShot::getImageReader)
                .observeOn(Schedulers.io())
                .map(new Function<ImageReader, byte[]>() {
                    @Override
                    public byte[] apply(ImageReader imageReader) throws Exception {
                        Image image = imageReader.acquireLatestImage();
                        if (image == null) {

                        } else {
                            int width = image.getWidth();

                            int height = image.getHeight();

                            final Image.Plane[] planes = image.getPlanes();

                            final ByteBuffer buffer = planes[0].getBuffer();

                            int pixelStride = planes[0].getPixelStride();

                            int rowStride = planes[0].getRowStride();

                            int rowPadding = rowStride - pixelStride * width;

                            byte[] bytes = copyToByteArray(buffer, width, height, rowPadding);
                            image.close();

//                            byte[] yuv = new byte[width * height * 3 / 2];
//                            YuvUtils.ConvertToI420(bytes, yuv, width, height);
                            return bytes;
                        }
                        return null;
                    }
                })
                .map(new Function<byte[], Object>() {
                    @Override
                    public Object apply(byte[] yuv) throws Exception {
                        X264Encoder instance = X264Encoder.getInstance();
                        if (!instance.isStart()) {
                            X264Encoder.start();
                            instance.setStart(true);
                        }
                        if (!isConnect) {
                            return null;
                        }
                        MediaCodec.BufferInfo bufferInfo = instance.encode(yuv);
                        ByteBuffer buffer = instance.getBuffer();
                        if (bufferInfo.flags == BUFFER_FLAG_CODEC_CONFIG) {
                            RtmpClient.getInstance().sendFormat(instance.getOutFormat());
                        } else {
                            RtmpClient.getInstance().sendData(bufferInfo, buffer);
                        }
                        return isConnect;
                    }
                })
                .doOnDispose(() -> {
                    X264Encoder instance = X264Encoder.getInstance();
                    if (instance.isStart()) {
                        X264Encoder.stop();
                    }
                    if (isConnect) {
                        RtmpClient.getInstance().closeUrl(url);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    private static Function<byte[], Object> boardCastMapper() {
        return yuv -> {
            X264Encoder instance = X264Encoder.getInstance();
            if (!instance.isStart()) {
                X264Encoder.start();
                instance.setStart(true);
            }
            if (!isConnect) {
                return null;
            }
            MediaCodec.BufferInfo bufferInfo = instance.encode(yuv);
            ByteBuffer buffer = instance.getBuffer();
            if (bufferInfo.flags == BUFFER_FLAG_CODEC_CONFIG) {
                RtmpClient.getInstance().sendFormat(instance.getOutFormat());
            } else {
                RtmpClient.getInstance().sendData(bufferInfo, buffer);
            }
            return null;
        };
    }


    private static byte[] copyToByteArray(ByteBuffer buffer, int width, int height, int rowPadding) {
        byte[] bytes = new byte[width * height * 4];
        int offset = 0;
        for (int i = 0; i < height; i++) {
            buffer.position(offset + i * rowPadding);
            buffer.get(bytes, offset, width * 4);
            offset += width * 4;
        }
        return bytes;
    }

    private static Observable<byte[]> castToYUV(ImageReaderAvailableObservable imageReaderAvailableObservable) {
        return imageReaderAvailableObservable
                .map(imageReader -> {
                    Image image = imageReader.acquireLatestImage();
                    if (image == null) {

                    } else {
                        int width = image.getWidth();

                        int height = image.getHeight();

                        final Image.Plane[] planes = image.getPlanes();

                        final ByteBuffer buffer = planes[0].getBuffer();

                        int pixelStride = planes[0].getPixelStride();

                        int rowStride = planes[0].getRowStride();

                        int rowPadding = rowStride - pixelStride * width;

                        byte[] bytes = copyToByteArray(buffer, width, height, rowPadding);
                        image.close();

                        byte[] yuv = new byte[width * height * 3 / 2];
                        YuvUtils.ConvertToI420(bytes, yuv, width, height);

                        return yuv;
                    }
                    return null;
                });
    }

}
