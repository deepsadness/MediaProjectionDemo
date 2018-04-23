package com.cry.screenop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.v4.app.FragmentActivity;

import com.cry.acresult.ActivityResultRequest;
import com.cry.acresult.ResultEvent;

import io.reactivex.Observable;
import io.reactivex.functions.Function;


/**
 * Created by a2957 on 4/21/2018.
 */

public class MediaProjectionHelper {
    public static Intent getCaptureIntent(MediaProjectionManager systemService) {
        return systemService == null ? null : systemService.createScreenCaptureIntent();
    }

    private static MediaProjectionManager getMediaProjectionManager(Context context) {
        return (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @SuppressWarnings("ConstantConditions")
    public static Observable<MediaProjection> requestCapture(FragmentActivity activity) {
        MediaProjectionManager mediaProjectionManager = getMediaProjectionManager(activity);
        if (mediaProjectionManager == null) {
            return Observable.just(null);
        } else {
            return Observable
                    .just(getCaptureIntent(mediaProjectionManager))
                    .filter(it -> it != null)
                    .flatMap(it -> ActivityResultRequest.rxQuest(activity, it))
                    .filter(it -> it.resultCode == Activity.RESULT_OK && it.data != null)
                    .map(resultEvent -> mediaProjectionManager.getMediaProjection(resultEvent.resultCode, resultEvent.data));
        }
    }
}
