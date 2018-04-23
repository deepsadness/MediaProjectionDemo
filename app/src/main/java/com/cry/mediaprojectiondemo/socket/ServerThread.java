package com.cry.mediaprojectiondemo.socket;

import android.os.HandlerThread;

/**
 * Created by a2957 on 4/23/2018.
 */

public class ServerThread extends HandlerThread {
    public ServerThread() {
        super("ServerThread");
    }
}
