package com.cry.mediaprojectiondemo

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.cry.screenop.MediaProjectionHelper
import com.cry.screenop.RxScreenShooter
import io.reactivex.android.schedulers.AndroidSchedulers

import kotlinx.android.synthetic.main.activity_sc.*
import kotlinx.android.synthetic.main.content_sc.*
import android.util.DisplayMetrics
import com.cry.mediaprojectiondemo.socket.ServerThread
import com.cry.mediaprojectiondemo.socket.SocketIoManager
import java.io.ByteArrayOutputStream


class ScActivity : AppCompatActivity() {

    private var isBack: Boolean = false
    private var serverThread: ServerThread? = null
    private var serverHandler: Handler? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sc)
//        setSupportActionBar(toolbar)

        serverThread = ServerThread()
        serverThread!!.start()
        serverHandler = object : Handler(serverThread!!.looper) {
            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                sendBitmap(msg?.obj as Bitmap)
            }
        }

        Thread {
            SocketIoManager.getInstance().startSocketIo()
        }.start()



        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            val outRect = IntArray(2)
            video.getLocationOnScreen(outRect)
            val metric = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metric)
            val videoTop = outRect[0]
            val videoHeight = video.height * 1f / metric.heightPixels * 1f * 720

            Log.e("ZZX", " videoTop: " + videoTop)
            Log.e("ZZX", "videoHeight is : " + videoHeight)

            RxScreenShooter
                    .shoot(this@ScActivity)
                    .subscribe({ it ->
                        if (it is Bitmap) {
                            if (!isBack) {
                                img.setImageBitmap(it)
                                moveTaskToBack(true)
                                isBack = true
                            } else {
                                val obtainMessage = serverHandler!!.obtainMessage()
                                obtainMessage.obj = it
                                serverHandler!!.sendMessage(obtainMessage)
                            }
                        }

                    }, { e -> e.printStackTrace() })

        }
    }

    private fun sendBitmap(it: Bitmap) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        it.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        SocketIoManager.getInstance().send(byteArray)
    }


    override fun onDestroy() {
        super.onDestroy()
        SocketIoManager.getInstance().release()
        serverThread!!.quitSafely()
        println("On Destory!!")
    }
}
