/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.mobilehelp.videoeditor.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mobilehelp.videoeditor.OptiVideoEditor
import com.mobilehelp.videoeditor.R
import com.mobilehelp.videoeditor.interfaces.OptiDialogueHelper
import com.mobilehelp.videoeditor.interfaces.OptiFFMpegCallback
import com.mobilehelp.videoeditor.utils.OptiConstant
import com.mobilehelp.videoeditor.utils.OptiUtils
import java.io.File
import java.util.*

class OptiAddOverlayVideoFragment : BottomSheetDialogFragment(), OptiDialogueHelper,
    OptiFFMpegCallback {

    private var tagName: String = OptiAddOverlayVideoFragment::class.java.simpleName
    private lateinit var rootView: View
    private lateinit var ivClose: ImageView
    private lateinit var ivDone: ImageView
    private lateinit var ivVideoOne: SimpleDraweeView
    private lateinit var ivVideoTwo: SimpleDraweeView
    private var videoFileOne: File? = null
    private var videoFileTwo: File? = null
    private var bmThumbnailOne: Bitmap? = null
    private var bmThumbnailTwo: Bitmap? = null
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null
    private var mContext: Context? = null
    private var videoUri: Uri? = null
    private var permissionList: ArrayList<String> = ArrayList()
    private lateinit var preferences: SharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.opti_fragment_merge_dialog, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivClose = rootView.findViewById(R.id.iv_close)
        ivDone = rootView.findViewById(R.id.iv_done)
        ivVideoOne = rootView.findViewById(R.id.iv_video_one)
        ivVideoTwo = rootView.findViewById(R.id.iv_video_two)



        mContext = context
        preferences =
            requireActivity().getSharedPreferences("fetch_permission", Context.MODE_PRIVATE)


        ivClose.setOnClickListener {
            dismiss()
        }

        ivDone.setOnClickListener {

            if (videoFileOne != null && videoFileTwo != null) {
                dismiss()

                //output file is generated and send to video processing
                val outputFile = OptiUtils.createVideoFile(requireContext())
                Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

                OptiVideoEditor.with(requireContext())
                    .setType(OptiConstant.VIDEO_CLIP_VIDEO_OVERLAY)
                    .setFile(videoFileOne!!)
                    .setFileTwo(videoFileTwo!!)
                    .setPosition(OptiVideoEditor.TOP_LEFT)
                    .setOutputPath(outputFile.path)
                    .setCallback(this)
                    .main()

                helper?.showLoading(true)
            } else {
                OptiUtils.showGlideToast(requireActivity(), getString(R.string.error_merge))
            }
        }

        ivVideoOne.setOnClickListener {


            openCamera()
        }

        ivVideoTwo.setOnClickListener {

            openGallery()


        }
    }

    override fun setMode(mode: Int) {

    }

    override fun setFilePathFromSource(file: File) {

    }

    override fun setHelper(helper: OptiBaseCreatorDialogFragment.CallBacks) {
        this.helper = helper
    }

    override fun setDuration(duration: Long) {

    }

    fun checkPermission(requestCode: Int, permission: String) {
        requestPermissions(arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            OptiConstant.VIDEO_MERGE_1 -> {

                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity as Activity,
                            permission
                        )
                    ) {
                        Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                        break
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                requireContext(),
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {


//                            call the camera intent
                            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                            videoFileOne = OptiUtils.createVideoFile(requireContext())

                            Log.v(tagName, "videoPath1: " + videoFileOne!!.absolutePath)
                            videoUri = FileProvider.getUriForFile(
                                requireContext(),
                                "com.mobilehelp.videoeditor.provider", videoFileOne!!
                            )
                            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 240) //4 minutes
                            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
                            startActivityForResult(cameraIntent, OptiConstant.VIDEO_MERGE_1)


                        } else {
                            callPermissionSettings()
                        }
                    }
                }
                return

            }

            OptiConstant.VIDEO_MERGE_2 -> {


                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity as Activity,
                            permission
                        )
                    ) {
                        Toast.makeText(activity, "Permission Denied", Toast.LENGTH_SHORT).show()
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                activity as Activity,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            //call the gallery intent
                            val i = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            )
                            i.setType("video/*")
                            i.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*"))
                            startActivityForResult(i, OptiConstant.VIDEO_MERGE_2)
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

    private val isMarshmallow: Boolean
        get() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) or (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1)


    fun checkHasPermission(context: Activity?, permissions: Array<String>?): ArrayList<String> {
        permissionList = ArrayList()
        if (isMarshmallow && context != null && permissions != null) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionList.add(permission)
                }
            }
        }
        return permissionList
    }

    private var isFirstTimePermission: Boolean
        get() = preferences.getBoolean("isFirstTimePermission", false)
        set(isFirstTime) = preferences.edit().putBoolean("isFirstTimePermission", isFirstTime)
            .apply()


    fun isPermissionBlocked(context: Activity?, permissions: ArrayList<String>?): Boolean {
        if (isMarshmallow && context != null && permissions != null && isFirstTimePermission) {
            for (permission in permissions) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                    return true
                }
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_CANCELED) return

        when (requestCode) {
            OptiConstant.VIDEO_MERGE_1 -> {
                data?.let {
                    setFilePath(resultCode, it, OptiConstant.VIDEO_MERGE_1)
                }
            }

            OptiConstant.VIDEO_MERGE_2 -> {
                data?.let {
                    setFilePath(resultCode, it, OptiConstant.VIDEO_MERGE_2)
                }
            }
        }
    }

    private fun setFilePath(resultCode: Int, data: Intent, mode: Int) {

        if (resultCode == Activity.RESULT_OK) {
            try {
                val selectedImage = data.data
                //  Log.e("selectedImage==>", "" + selectedImage)
                val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
                val cursor = requireContext().contentResolver
                    .query(selectedImage!!, filePathColumn, null, null, null)
                if (cursor != null) {
                    cursor.moveToFirst()
                    val columnIndex = cursor
                        .getColumnIndex(filePathColumn[0])
                    val filePath = cursor.getString(columnIndex)
                    cursor.close()
                    if (mode == OptiConstant.VIDEO_MERGE_1) {
                        videoFileOne = File(filePath)
                        Log.v(tagName, "videoFileOne: " + videoFileOne!!.absolutePath)

                        //get thumbnail of selected video
                        bmThumbnailOne = ThumbnailUtils.createVideoThumbnail(
                            videoFileOne!!.absolutePath,
                            MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
                        )

                        ivVideoOne.setImageBitmap(bmThumbnailOne)
                    } else if (mode == OptiConstant.VIDEO_MERGE_2) {
                        videoFileTwo = File(filePath)
                        Log.v(tagName, "videoFileTwo: " + videoFileTwo!!.absolutePath)

                        //get thumbnail of selected video
                        bmThumbnailTwo = ThumbnailUtils.createVideoThumbnail(
                            videoFileTwo!!.absolutePath,
                            MediaStore.Video.Thumbnails.FULL_SCREEN_KIND
                        )

                        ivVideoTwo.setImageBitmap(bmThumbnailTwo)
                    }
                }
            } catch (e: Exception) {
                Log.e(tagName, "Exception: ${e.localizedMessage}")
            }
        }
    }

    companion object {
        fun newInstance() = OptiAddOverlayVideoFragment()
    }

    override fun onProgress(progress: String) {
        Log.v(tagName, "onProgress()")
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Log.v(tagName, "onSuccess()")
        helper?.showLoading(false)
        helper?.onFileProcessed(convertedFile)
    }

    override fun onFailure(error: Exception) {
        Log.v(tagName, "onFailure() ${error.localizedMessage}")
        Toast.makeText(mContext, "Video processing failed", Toast.LENGTH_LONG).show()
        helper?.showLoading(false)
    }

    override fun onNotAvailable(error: Exception) {
        Log.v(tagName, "onNotAvailable() ${error.localizedMessage}")
    }

    override fun onFinish() {
        Log.v(tagName, "onFinish()")
        helper?.showLoading(false)
        showsDialog

    }

    fun openGallery() {
        checkPermission(OptiConstant.VIDEO_MERGE_2, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun openCamera() {

        checkAllPermission(OptiConstant.PERMISSION_CAMERA)
    }


    fun checkAllPermission(permission: Array<String>) {
        val blockedPermission = checkHasPermission(activity, permission)
        if (blockedPermission != null && blockedPermission.size > 0) {
            val isBlocked = isPermissionBlocked(activity, blockedPermission)
            if (isBlocked) {
                callPermissionSettings()
            } else {
                requestPermissions(permission, OptiConstant.VIDEO_MERGE_1)
            }
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            videoFileOne = OptiUtils.createVideoFile(requireContext())
            Log.v(tagName, "videoPath1: " + videoFileOne!!.absolutePath)
            videoUri = FileProvider.getUriForFile(
                requireContext(),
                "com.mobilehelp.videoeditor.provider", videoFileOne!!
            )
            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 240) //4 minutes
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
            startActivityForResult(cameraIntent, OptiConstant.VIDEO_MERGE_1)
        }
    }
}
