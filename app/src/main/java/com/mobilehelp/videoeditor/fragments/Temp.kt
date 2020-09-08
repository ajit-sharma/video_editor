package com.mobilehelp.videoeditor.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mobilehelp.videoeditor.OptiVideoEditor
import com.mobilehelp.videoeditor.R
import com.mobilehelp.videoeditor.alphaVideo.AlphaMovieView
import com.mobilehelp.videoeditor.interfaces.OptiDialogueHelper
import com.mobilehelp.videoeditor.interfaces.OptiFFMpegCallback
import com.mobilehelp.videoeditor.utils.OptiConstant
import com.mobilehelp.videoeditor.utils.OptiUtils
import com.otaliastudios.cameraview.*
import com.otaliastudios.cameraview.controls.Facing
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class Temp : BottomSheetDialogFragment(), OptiDialogueHelper,
    OptiFFMpegCallback {


    private var tagName: String = Temp::class.java.simpleName
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null
    private var mContext: Context? = null
    private lateinit var rootView: View
    private var permissionList: ArrayList<String> = ArrayList()
    private lateinit var preferences: SharedPreferences


    private var camera: CameraView? = null
    private var saveVideo: FloatingActionButton? = null
    private var fabVideo: FloatingActionButton? = null
    private var fabPreview: FloatingActionButton? = null
    private var fabPicture: FloatingActionButton? = null
    private var fabFront: FloatingActionButton? = null
    private var fabSave: FloatingActionButton? = null
    private var videoFileOne: File? = null
    private var videoFileTwo: File? = null
    private var videoUri: Uri? = null
    private lateinit var overlayVideo: AlphaMovieView
    private var videoResult: WeakReference<VideoResult>? = null
    private var backGroundVideoResult: VideoResult? = null

    companion object {
        fun newInstance() = Temp()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.temp, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE)

        camera = rootView?.findViewById(R.id.camera)
        saveVideo = rootView?.findViewById(R.id.save_overlay_view)
        camera!!.setLifecycleOwner(this)

        camera!!.videoMaxDuration = 120 * 1000 // max 2mins

        camera!!.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {

            }

            override fun onVideoTaken(result: VideoResult) {
                super.onVideoTaken(result)



                result?.let { file ->

                    backGroundVideoResult = file
                    videoResult = result?.let { WeakReference(file) }

                    OptiVideoPreviewFragment.newInstance().apply {
                        setVideoResult(
                            file, videoUri, videoFileTwo
                        )

                    }.show(requireFragmentManager(), "OptiVideoPreviewFragment")
                }


                // refresh gallery
                MediaScannerConnection.scanFile(
                    activity,
                    arrayOf(result.file.toString()),
                    null
                ) { filePath: String, uri: Uri ->
                    Log.i("ExternalStorage", "Scanned $filePath:")
                    Log.i("ExternalStorage", "-> uri=$uri")
                }
            }
        })

        fabVideo = rootView?.findViewById(R.id.fab_video)
        fabPreview = rootView?.findViewById(R.id.fab_preview)
        fabPicture = rootView?.findViewById(R.id.fab_picture)
        fabFront = rootView?.findViewById(R.id.fab_front)
        fabSave = rootView?.findViewById(R.id.save_overlay_view)
        overlayVideo = rootView?.findViewById(R.id.watermark)!!
        overlayVideo.visibility = View.GONE

        mContext = context
        preferences =
            requireActivity().getSharedPreferences("fetch_permission", Context.MODE_PRIVATE)


//        ivClose.setOnClickListener {
//            dismiss()
//        }
//
//        ivDone.setOnClickListener {
//
//            if (videoFileOne != null && videoFileTwo != null) {
//                dismiss()
//
//                //output file is generated and send to video processing
//                val outputFile = OptiUtils.createVideoFile(requireContext())
//                Log.v(tagName, "outputFile: ${outputFile.absolutePath}")
//
//                OptiVideoEditor.with(requireContext())
//                    .setType(OptiConstant.VIDEO_CLIP_VIDEO_OVERLAY)
//                    .setFile(videoFileOne!!)
//                    .setFileTwo(videoFileTwo!!)
//                    .setPosition(OptiVideoEditor.TOP_LEFT)
//                    .setOutputPath(outputFile.path)
//                    .setCallback(this)
//                    .main()
//
//                helper?.showLoading(true)
//            } else {
//                OptiUtils.showGlideToast(requireActivity(), getString(R.string.error_merge))
//            }
//        }

        fabVideo!!.setOnClickListener {

            openCamera()
        }

        fabPicture!!.setOnClickListener {

            openGallery()
        }

        fabPreview!!.setOnClickListener {

            if (backGroundVideoResult != null && videoFileTwo != null) previewVideo(
                backGroundVideoResult!!,
                videoUri!!,
                videoFileTwo
            )
            else Toast.makeText(mContext, "No Video Create to display", Toast.LENGTH_LONG).show()
        }

        fabFront!!.setOnClickListener {

            if (camera!!.isTakingPicture || camera!!.isTakingVideo) return@setOnClickListener
            when (camera!!.toggleFacing()) {
                Facing.BACK -> fabFront!!.setImageResource(R.drawable.ic_camera_front_black_24dp)
                Facing.FRONT -> fabFront!!.setImageResource(R.drawable.ic_camera_rear_black_24dp)
            }
        }

        fabSave!!.setOnClickListener {

            if (videoFileOne != null && videoFileTwo != null) {
                AlertDialog.Builder(requireContext())
                    .setTitle(OptiConstant.APP_NAME)
                    .setMessage(getString(R.string.save_video))
                    .setPositiveButton(getString(R.string.Continue)) { dialog, which ->


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
                    }.setNegativeButton(R.string.cancel) { dialog, which -> }
                    .show()


            } else {
                OptiUtils.showGlideToast(requireActivity(), getString(R.string.error_merge))
            }


        }
    }


    fun openGallery() {
        checkPermission(OptiConstant.VIDEO_MERGE_2, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun openCamera() {
        checkAllPermission(OptiConstant.PERMISSION_CAMERA)
    }

    fun captureVideoSnapshot() {
        if (videoFileTwo !== null) {

            overlayVideo.stop()
            overlayVideo.setVideoFromUri(activity, videoUri)
            overlayVideo.start()
            if (camera!!.isTakingVideo) {
                camera!!.stopVideo()
                fabVideo!!.setImageResource(R.drawable.ic_videocam_black_24dp)
                return
            }
            videoFileOne = OptiUtils.createVideoFile(requireContext())
            camera!!.takeVideoSnapshot(videoFileOne!!)
            fabVideo!!.setImageResource(R.drawable.ic_stop_black_24dp)

        } else {

            Toast.makeText(mContext, "Please Select Overlay Video", Toast.LENGTH_LONG).show()
        }

    }

    fun previewVideo(
        videoResult: VideoResult,
        videoUri: Uri,
        overlayVideoFile: File?
    ) {

        videoResult?.let { file ->

            OptiVideoPreviewFragment.newInstance().apply {
                setVideoResult(
                    file, videoUri, overlayVideoFile
                )

            }.show(requireFragmentManager(), "OptiVideoPreviewFragment")
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_CANCELED) return

        when (requestCode) {

            OptiConstant.VIDEO_MERGE_2 -> {
                data?.let {
                    videoUri = it.data
                    overlayVideo.visibility = View.VISIBLE
                    overlayVideo.setVideoFromUri(mContext, videoUri)
                    overlayVideo.start()
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


                    } else if (mode == OptiConstant.VIDEO_MERGE_2) {
                        videoFileTwo = File(filePath)
                    }
                }
            } catch (e: Exception) {
                Log.e(tagName, "Exception: ${e.localizedMessage}")
            }
        }
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

                            captureVideoSnapshot()


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
            captureVideoSnapshot()
        }
    }


    override fun setHelper(helper: OptiBaseCreatorDialogFragment.CallBacks) {
        this.helper = helper
    }

    override fun setMode(mode: Int) {
        TODO("Not yet implemented")
    }

    override fun setFilePathFromSource(file: File) {
        TODO("Not yet implemented")
    }

    override fun setDuration(duration: Long) {
        TODO("Not yet implemented")
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
}


