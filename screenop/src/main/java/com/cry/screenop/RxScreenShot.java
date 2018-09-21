package com.cry.screenop;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * 截取屏幕的单利
 * Created by a2957 on 4/21/2018.
 */
public class RxScreenShot {
    private String TAG = "RxScreenShot";

    private Handler mCallBackHandler = new CallBackHandler();
    private MediaCallBack mMediaCallBack = new MediaCallBack();
    private MediaProjection mediaProjection;
    SurfaceFactory mSurfaceFactory;
    ImageReader mImageReader;

    public int width = 480;
    public int height = 720;
    public int dpi = 1;

    private RxScreenShot(MediaProjection mediaProjection) {
        this.mediaProjection =
                mediaProjection;
    }

    public static RxScreenShot of(MediaProjection mediaProjection) {
        return new RxScreenShot(mediaProjection);
    }

    public RxScreenShot createImageReader() {
        //注意这里使用RGB565报错提示，只能使用RGBA_8888
        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 5);
        mSurfaceFactory = new ImageReaderSurface(mImageReader);
        createProject();
        return this;
    }

    private void createProject() {
        mediaProjection.registerCallback(mMediaCallBack, mCallBackHandler);
        mediaProjection.createVirtualDisplay(TAG + "-display", width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mSurfaceFactory.getInputSurface(), null, null);
    }

    public Observable<Object> startCapture() {
        return ImageReaderAvailableObservable.of(mImageReader)
                .map(new Function<ImageReader, Object>() {
                    @Override
                    public Object apply(ImageReader imageReader) throws Exception {
                        String mImageName = System.currentTimeMillis() + ".png";
                        Log.e(TAG, "image name is : " + mImageName);
                        Bitmap bitmap = null;
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

                            bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);

                            bitmap.copyPixelsFromBuffer(buffer);

                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);

                            image.close();
                        }


                        return bitmap == null ? new Object() : bitmap;
                    }
                });
    }

    public Observable<Object> startCaptureWithHW(int topStart, int totalHeight) {
        return ImageReaderAvailableObservable.of(mImageReader, mCallBackHandler)
                .observeOn(Schedulers.io())
                .map(new Function<ImageReader, Object>() {
                    @Override
                    public Object apply(ImageReader imageReader) throws Exception {
//                        String name = Thread.currentThread().getName();
//                        String mImageName = System.currentTimeMillis() + ".png";
//                        Log.e(TAG, "image name is : " + mImageName);
//                        Log.e(TAG, "Thread currentThread is : " + name);

                        Image image = imageReader.acquireLatestImage();

                        int width = image.getWidth();

                        int height = image.getHeight();

                        final Image.Plane[] planes = image.getPlanes();

                        final ByteBuffer buffer = planes[0].getBuffer();

                        int pixelStride = planes[0].getPixelStride();

                        int rowStride = planes[0].getRowStride();

                        int rowPadding = rowStride - pixelStride * width;

                        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);

                        bitmap.copyPixelsFromBuffer(buffer);

                        bitmap = Bitmap.createBitmap(bitmap, 0, topStart, width, totalHeight);

                        image.close();

                        return bitmap == null ? new Object() : bitmap;
                    }
                });
    }

    public static Observable<Object> shoot(FragmentActivity activity) {
        return MediaProjectionHelper
                .requestCapture(activity)
                .map(mediaProjection -> RxScreenShot.of(mediaProjection).createImageReader())
                .flatMap(RxScreenShot::startCapture)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    class MediaCallBack extends MediaProjection.Callback {
        @Override
        public void onStop() {
            super.onStop();
        }
    }

    static class CallBackHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    public interface SurfaceFactory {
        Surface getInputSurface();
    }

    class ImageReaderSurface implements SurfaceFactory {

        private ImageReader imageReader;

        public ImageReaderSurface(ImageReader imageReader) {
            this.imageReader = imageReader;
        }

        @Override
        public Surface getInputSurface() {
            return imageReader.getSurface();
        }
    }
}
