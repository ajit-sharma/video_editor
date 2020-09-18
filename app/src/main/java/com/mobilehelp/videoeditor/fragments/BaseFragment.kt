package com.mobilehelp.videoeditor.fragments

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.mobilehelp.videoeditor.R
import java.io.File

abstract class BaseFragment : Fragment() {

    private var progressDialog: Dialog? = null
    private var msg: TextView? = null

    interface CallBacks {

        fun onDidNothing()

        fun onFileProcessed(file: File)

        fun getFile(): File?

        fun reInitPlayer()

        fun onAudioFileProcessed(convertedAudioFile: File)

        fun showLoading(isShow: Boolean)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressDialog = Dialog(requireContext(), R.style.progress_dialog)
        progressDialog!!.setContentView(R.layout.progress_dialog)
        progressDialog!!.setCancelable(true)
        progressDialog!!.window
            ?.setBackgroundDrawableResource(android.R.color.transparent)
        msg =
            progressDialog!!.findViewById<View>(R.id.id_tv_loadingmsg) as TextView
    }

    private var permissionsRequired = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            130 -> {
                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity as Activity,
                            permission
                        )
                    ) {
                        //denied
                        Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                requireContext(),
                                permissionsRequired[0]
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            //SaveImage()
                        } else {
                            callPermissionSettings()
                        }
                    }
                }
                return
            }
        }
    }


    private fun callPermissionSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", requireContext().applicationContext.packageName, null)
        intent.data = uri
        startActivityForResult(intent, 300)
    }

    override fun onResume() {
        super.onResume()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                permissionsRequired[0]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(permissionsRequired, 130)
        }
    }

    fun stopRunningProcess() {
        FFmpeg.getInstance(activity).killRunningProcesses()
    }

    fun isRunning(): Boolean {
        return FFmpeg.getInstance(activity).isFFmpegCommandRunning
    }

    fun showInProgressToast(progressMessage: String) {

        msg!!.text = progressMessage
        progressDialog!!.show()


    }

    fun onComplete(progressMessage: String, saveButtonClick: Boolean) {

        if (progressDialog!!.isShowing) {
            msg!!.text = progressMessage
            Thread(Runnable {
                try {
                    Thread.sleep(500)

                    progressDialog!!.dismiss()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }).start()

        }else if(!progressDialog!!.isShowing && saveButtonClick){

            msg!!.text = progressMessage
            progressDialog!!.show()
            Thread(Runnable {
                try {
                    Thread.sleep(500)

                    progressDialog!!.dismiss()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }).start()

        }


    }
}