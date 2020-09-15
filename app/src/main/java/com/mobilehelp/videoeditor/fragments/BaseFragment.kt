package com.mobilehelp.videoeditor.fragments

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import java.io.File

abstract class BaseFragment : Fragment() {

    interface CallBacks {

        fun onDidNothing()

        fun onFileProcessed(file: File)

        fun getFile(): File?

        fun reInitPlayer()

        fun onAudioFileProcessed(convertedAudioFile: File)

        fun showLoading(isShow: Boolean)

    }
}