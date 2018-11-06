package com.cry.mediaprojectiondemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.cry.RxRtmpSender;
import com.cry.screenop.RxScreenShot;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class MainImageRecorderActivity extends AppCompatActivity {
    public Disposable dispose;
    private boolean isStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_image_recorder);

        findViewById(R.id.btn_start)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isStart) {
                            isStart = true;
                            RxRtmpSender
                                    .wrap("rtmp://192.168.31.17/live/READER", MainImageRecorderActivity.this)
                                    .subscribe(new Observer<Object>() {

                                        @Override
                                        public void onSubscribe(Disposable d) {
                                            dispose = d;
                                        }

                                        @Override
                                        public void onNext(Object o) {
//                                            moveTaskToBack(true);
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            e.printStackTrace();
                                            isStart = false;
                                        }

                                        @Override
                                        public void onComplete() {

                                        }
                                    });
                        } else {
                            dispose();
                        }


                    }
                });
    }

    private void dispose() {
        if (dispose != null && !dispose.isDisposed()) {
            dispose.dispose();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispose();
    }
}
