package com.cry.mediaprojectiondemo

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.cry.acresult.ActivityResultRequest
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rxQuest = ActivityResultRequest.rxQuest(this, Intent(this@MainActivity, ScActivity::class.java))

        btn_start.setOnClickListener {
            rxQuest.subscribe {
                println(it)
            }
        }
    }
}
