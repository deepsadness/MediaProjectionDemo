package com.cry.mediaprojectiondemo;

import android.graphics.Bitmap;
import android.net.LocalServerSocket;
import android.net.LocalSocket;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import kotlin.jvm.Throws;

/**
 * Created by a2957 on 4/21/2018.
 */

public class LocalServer {
    LocalServerSocket serverSocket = null;
    LocalSocket socket = null;

    public void start() throws IOException {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    serverSocket = new LocalServerSocket("puppet-ver1");
                    while (true) {
                        socket = serverSocket.accept();
                        System.out.println("accept!!!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();


    }

    public void write(Bitmap bitmap) throws IOException {
        int VERSION = 2;
        BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
        while (true) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
            outputStream.write(2);
            writeInt(outputStream, byteArrayOutputStream.size());
            outputStream.write(byteArrayOutputStream.toByteArray());
            outputStream.flush();
        }
    }

    private void writeInt(OutputStream outputStream, int v) throws IOException {
        outputStream.write(v >> 24);
        outputStream.write(v >> 16);
        outputStream.write(v >> 8);
        outputStream.write(v);
    }

}
