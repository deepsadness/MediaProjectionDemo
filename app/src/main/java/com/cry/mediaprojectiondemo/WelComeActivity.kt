package com.cry.mediaprojectiondemo

import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.cry.cry.mediaprojectioncode.RecordActivity
import com.cry.mediaprojectiondemo.socket.ServerThread
import com.cry.mediaprojectiondemo.socket.SocketIoManager
import com.cry.screenop.RxScreenShot
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_wel.*
import java.io.ByteArrayOutputStream

class WelComeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wel)

        btn_1.setOnClickListener{
            _->
            val intent = Intent(this@WelComeActivity, MainActivity::class.java)
            startActivity(intent)
        }
        btn_2.setOnClickListener{
            _->
            val intent = Intent(this@WelComeActivity, RecordActivity::class.java)
            startActivity(intent)
        }

    }


}
