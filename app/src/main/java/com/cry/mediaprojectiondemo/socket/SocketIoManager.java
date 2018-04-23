package com.cry.mediaprojectiondemo.socket;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

/**
 * Created by a2957 on 4/23/2018.
 */

public class SocketIoManager {

    private String localUrl;

    private SocketIoManager() {
    }

    private static class SINGLE_TON {
        private static SocketIoManager INSTANCE = new SocketIoManager();
    }

    public static SocketIoManager getInstance() {
        return SINGLE_TON.INSTANCE;
    }

    private Socket mSocket;
    private boolean mSocketReady;

    private Emitter.Listener fn = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
//            JSONObject data = (JSONObject) args[0];
            System.out.println(args);
        }
    };
    private Emitter.Listener joinEmiiter = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            //建立成功~
            System.out.println(args[0]);
            //将自己的账户发送过去
//            mSocket.emit("user","sender");
            mSocketReady = true;

        }
    };

    private final String LOCAL_SOCKET_URL = "http://192.168.20.194:9000";

    public void startSocketIo() {
        try {
            mSocket = IO.socket(LOCAL_SOCKET_URL);
            //事件监听
            mSocket.on("event", fn);
            //进入监听
            mSocket.on("join", joinEmiiter);
            mSocket.connect();
        } catch (URISyntaxException e) {

        }
    }

    public void send(byte[] bitmapArray) {
        if (!mSocketReady) {
            return;
        }
        if (bitmapArray != null) {
            mSocket.emit("event", bitmapArray);
        }
    }

    public void release() {
        mSocket.disconnect();
        mSocket.off("event", fn);
        mSocket.off("join", joinEmiiter);
    }
}
