package com.mobilehelp.videoeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobilehelp.videoeditor.fragments.OptiMasterProcessorFragment


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_container, OptiMasterProcessorFragment()).commit()

    }
}
