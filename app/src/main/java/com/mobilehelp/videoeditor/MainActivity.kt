/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.mobilehelp.videoeditor

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.mobilehelp.videoeditor.fragments.SingleFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val abc = object : LoadBinaryResponseHandler() {
            override fun onFailure() {
                super.onFailure()
                Log.d("MainActivity", "onFailure")
            }

            override fun onStart() {
                super.onStart()
                Log.d("MainActivity", "onStart")
            }

            override fun onFinish() {
                super.onFinish()
                Log.d("MainActivity", "onFinish")
            }

            override fun onSuccess() {
                super.onSuccess()
                Log.d("MainActivity", "onSuccess")
            }
        }


        val ffmpeg = FFmpeg.getInstance(this)
        ffmpeg.loadBinary(abc)

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_container, SingleFragment()).commit()
    }
}
