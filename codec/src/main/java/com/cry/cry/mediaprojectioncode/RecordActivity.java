package com.cry.cry.mediaprojectioncode;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.media.MediaCodecInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cry.cry.mediaprojectioncode.codec.MediaCodecHelper;
import com.cry.cry.mediaprojectioncode.codec.MediaCodecLogHelper;
import com.cry.cry.mediaprojectioncode.sender.Sender;
import com.cry.cry.mediaprojectioncode.surface.MediaCodecSurface;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class RecordActivity extends AppCompatActivity {

    private MediaProjectionManager mPrMgr;
    private SurfaceFactory factory;
    private int width = 480;
    private int height = 720;
    private MediaProjection mPr;
    private Button mBtn;
    private int mPrCode;
    private Intent mPrBundle;
    //    String url = "rtmp://localhost/live/";
//    String url = "rtmp://localhost/live/STREAM_NAME";
//    String url = "rtmp://169.254.8.220/live/STREAM_NAME";
    String url = "rtmp://192.168.1.163/live/STREAM_NAME";
    private Timer timer;
    private TextView mCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_main);

        //先请求permission
        int writePm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int InternetPm = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        if (writePm != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }


        mBtn = (Button) findViewById(R.id.btn);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPr == null && mPrCode == 0 && mPrBundle == null) {
                    Sender.getInstance().open(url, width, height);
                    mPrMgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                    Intent screenCaptureIntent = mPrMgr.createScreenCaptureIntent();
                    startActivityForResult(screenCaptureIntent, 1);
                } else {
                    if (mPr != null) {
                        releaseProjection();
                        releaseTimer();
                    }
                }
            }
        });

        factory = new MediaCodecSurface();

        String mimeType = MediaCodecHelper.VIDEO_AVC;


        ArrayList<MediaCodecInfo> adaptiveEncoderCodec = MediaCodecHelper.getAdaptiveEncoderCodec(mimeType);
        String s = MediaCodecLogHelper.printVideoCodecCap(adaptiveEncoderCodec, mimeType);
        TextView textView = (TextView) findViewById(R.id.tv);
        textView.setText(s);


        mCountText = findViewById(R.id.count_);
        mCountText.setVisibility(View.GONE);
    }

    private void startTimer() {
        timer = new Timer();
        long startTime = System.currentTimeMillis();
        mCountText.setVisibility(View.VISIBLE);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    long curTime = System.currentTimeMillis();
                    long l = (curTime - startTime) / 1000;
                    mCountText.setText("" + l);
                });
            }
        }, 0, 1000);
    }

    private void releaseTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            mCountText.setVisibility(View.GONE);
        }
    }

    private void releaseProjection() {
        mPr.stop();
        mPr = null;
        mPrCode = 0;
        mPrBundle = null;
        factory.stop();
        mBtn.setText("start");
        Sender.getInstance().close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission not granted!!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.mPrCode = resultCode;
        this.mPrBundle = data;
        mPr = mPrMgr.getMediaProjection(this.mPrCode, this.mPrBundle);
        if (mPr != null) {
            Surface surface = factory.createSurface(width, height);
            if (surface == null) {
                releaseProjection();
                releaseTimer();
                Toast.makeText(this, "Can not create surface", Toast.LENGTH_SHORT).show();
            } else {
                startTimer();
                mPr.createVirtualDisplay("display-", width, height, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
//            mediaProjection.registerCallback();
                mBtn.setText("stop");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Sender.getInstance().close();
        releaseTimer();

    }
}
