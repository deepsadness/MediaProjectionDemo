package com.cry.acresult;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.SparseArray;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 转发所有onActivityResult的Fragment
 * Created by a2957 on 4/21/2018.
 */

public class OnActivityResultDispatcherFragment extends Fragment {
    public static final String TAG = "com.cry.acresult.OnActivityResultDispatcherFragment";

    public static final AtomicInteger AUTO_REQ_CODE =new  AtomicInteger(1000);

    private SparseArray<OnResultListener> listeners = new SparseArray<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        OnResultListener onResultListener = listeners.get(requestCode);
        if (onResultListener != null) {
            listeners.remove(requestCode);
            onResultListener.onActivityResult(resultCode, data);
        }
    }

    public void startIntentForResult(Intent intent, OnResultListener listener,int requestCode) {
        listeners.put(requestCode, listener);
        startActivityForResult(intent, requestCode);
    }

    public void remove(int requestCode) {
        listeners.remove(requestCode);
    }

    public interface OnResultListener {
        void onActivityResult(int resultCode, Intent data);
    }
}
