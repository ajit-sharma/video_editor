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
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mobilehelp.videoeditor.OptiVideoEditor
import com.mobilehelp.videoeditor.R
import com.mobilehelp.videoeditor.alphaVideo.AlphaMovieView
import com.mobilehelp.videoeditor.interfaces.OptiFFMpegCallback
import com.mobilehelp.videoeditor.utils.*
import com.otaliastudios.cameraview.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class OptiCameraFragment : BottomSheetDialogFragment(), OptiBaseCreatorDialogFragment.CallBacks, OptiFFMpegCallback {

    companion object {
        private val LOG = CameraLogger.create("DemoApp")
        private const val USE_FRAME_PROCESSOR = true
        private const val DECODE_BITMAP = false
    }

    private lateinit var progressBar: ProgressBar
    private var tvVideoProcessing: TextView? = null
    private var handler: Handler = Handler()
    private var masterVideoFile: File? = null
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0
    private var ePlayer: PlayerView? = null
    private var pbLoading: ProgressBar? = null
    private var exoPlayer: SimpleExoPlayer? = null
    private var playWhenReady: Boolean? = false
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null

    private var tagName: String = OptiMasterProcessorFragment::class.java.simpleName
    private lateinit var rootView: View

    private var permissionList: ArrayList<String> = ArrayList()
    private lateinit var preferences: SharedPreferences


    private var mContext: Context? = null
    private var camera: CameraView? = null
    private var saveVideo: FloatingActionButton? = null
    private var fabVideo: FloatingActionButton? = null
    private var fabPreview: FloatingActionButton? = null
    private var fabPicture: FloatingActionButton? = null
    private var fabFront: FloatingActionButton? = null
    private var videoFileOne: File? = null
    private var videoFileTwo: File? = null
    private var videoUri: Uri? = null
    private var videoFile: File? = null
    private lateinit var overlayVideo: AlphaMovieView
    private var videoResult: WeakReference<VideoResult>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.activity_camera, container, false)
        initView(rootView)
        return rootView
    }

    private fun initView(rootView: View?) {

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

//                    videoResult = result?.let { WeakReference(file) }
//                    OptiVideoPreviewFragment.newInstance().apply {
//                        setHelper(this@OptiCameraFragment)
//                        setVideoResult(
//                            file, videoUri, videoFileTwo, _xDeltaTemp, _yDeltaTemp
//                        )
//
//                    }.show(requireFragmentManager(), "OptiVideoPreviewFragment")
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
        overlayVideo = rootView?.findViewById(R.id.watermark)!!

        progressBar = rootView?.findViewById(R.id.progressBar)!!
        tvVideoProcessing = rootView.findViewById(R.id.tvVideoProcessing)


        saveVideo!!.setOnClickListener{

            val outputFile = OptiUtils.createVideoFile(requireContext())
            Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

            val result: VideoResult? =
                if (videoResult == null) null else videoResult!!.get()
            OptiVideoEditor.with(requireContext())
                .setType(OptiConstant.VIDEO_CLIP_VIDEO_OVERLAY)
                .setFile(result!!.file)
                .setFileTwo(videoFileTwo!!)
                .setPosition(OptiVideoEditor.TOP_LEFT)
                .setOutputPath(outputFile.path)
                .setCallback(this)
                .main()

            helper?.showLoading(true)

        }
        fabPicture!!.setOnClickListener {

            openGallery()

        }

//
//        videoUri = Uri.parse(
//            "android.resource://" + requireActivity().packageName + "/" +
//                    R.raw.tiger
//        )
//        overlayVideo.setVideoFromUri(
//            activity, videoUri
//        )
//        overlayVideo.start()

        mContext = context
        preferences =
            requireActivity().getSharedPreferences("fetch_permission", Context.MODE_PRIVATE)


        fabVideo?.setOnClickListener {

            checkAllPermission(OptiConstant.PERMISSION_CAMERA)
//

        }

        fabPreview?.setOnClickListener {

//            previewVideo(videoResult, videoUri!!, videoFileTwo)
//

        }

    }

    fun previewVideo(
        videoResult: VideoResult,
        videoUri: Uri,
        overlayVideoFile: File?
    ) {

        videoResult?.let { file ->

//            OptiVideoPreviewFragment.newInstance().apply {
//                setHelper(this@OptiCameraFragment)
//                setVideoResult(
//                    file, videoUri, videoFileTwo, _xDeltaTemp, _yDeltaTemp
//                )
//
//            }.show(requireFragmentManager(), "OptiVideoPreviewFragment")
        }


    }

    fun captureVideoSnapshot() {
        overlayVideo.stop()
        overlayVideo.setVideoFromUri(activity, videoUri)
        overlayVideo.start()
        if (camera!!.isTakingVideo) {
            camera!!.stopVideo()
            fabVideo!!.setImageResource(R.drawable.ic_videocam_black_24dp)
            return
        }
        videoFileOne = OptiUtils.createVideoFile(requireContext())
//        val dateFormat =
//            SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.US)
//        val currentTimeStamp = dateFormat.format(Date())
//        val path =
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//                .toString() + File.separator + "CameraViewFreeDrawing"
//        val outputDir = File(path)
//        outputDir.mkdirs()
//        val saveTo =
//            File(path + File.separator + currentTimeStamp + ".mp4")
        camera!!.takeVideoSnapshot(videoFileOne!!)
        fabVideo!!.setImageResource(R.drawable.ic_stop_black_24dp)
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

////                            call the camera intent
//                            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
//                            videoFile = OptiUtils.createVideoFile(requireContext())
//
//                            Log.v(tagName, "videoPath1: " + videoFile!!.absolutePath)
//                            videoUri = FileProvider.getUriForFile(
//                                requireContext(),
//                                "com.mobilehelp.videoeditor.provider", videoFile!!
//                            )
//                            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 240) //4 minutes
//                            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
//                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
//                            startActivityForResult(cameraIntent, OptiConstant.VIDEO_MERGE_1)


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


            OptiConstant.VIDEO_MERGE_2 -> {
                data?.let {

                    videoUri = it.data
                    overlayVideo.setVideoFromUri(context, videoUri)
                    overlayVideo.start()
                    setFilePath(resultCode, it, OptiConstant.VIDEO_MERGE_2)
                }
            }
        }
    }


    private fun setFilePath(resultCode: Int, data: Intent, mode: Int) {

        try {
            //  Log.e("selectedImage==>", "" + selectedImage)
            val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
            val cursor = requireContext().contentResolver
                .query(videoUri!!, filePathColumn, null, null, null)
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


//                    if (mode == OptiConstant.VIDEO_MERGE_1) {
//                        videoFile = File(filePath)
//                        Log.v(tagName, "videoFile: " + videoFile!!.absolutePath)
//
//                        //get thumbnail of selected video
////                        bmThumbnailOne = ThumbnailUtils.createVideoThumbnail(
////                            videoFileOne!!.absolutePath,
////                            MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
////                        )
////
////                        ivVideoOne.setImageBitmap(bmThumbnailOne)
//                    }
            }
        } catch (e: Exception) {
            Log.e(tagName, "Exception: ${e.localizedMessage}")
        }
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
//            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
//            videoFileOne = OptiUtils.createVideoFile(requireContext())
//            Log.v(tagName, "videoPath1: " + videoFileOne!!.absolutePath)
//            videoUri = FileProvider.getUriForFile(
//                requireContext(),
//                "com.mobilehelp.videoeditor.provider", videoFileOne!!
//            )
//            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 240) //4 minutes
//            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
//            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
//            startActivityForResult(cameraIntent, OptiConstant.VIDEO_MERGE_1)
        }
    }

    private fun showBottomSheetDialogFragment(bottomSheetDialogFragment: BottomSheetDialogFragment) {
        val bundle = Bundle()
        bottomSheetDialogFragment.arguments = bundle
        bottomSheetDialogFragment.show(requireFragmentManager(), bottomSheetDialogFragment.tag)
    }

    override fun onDidNothing() {
        initializePlayer()
    }

    override fun onFileProcessed(file: File) {
//        tvSave!!.visibility = View.VISIBLE
        masterVideoFile = file
//        isLargeVideo = false

        val extension = OptiCommonMethods.getFileExtension(masterVideoFile!!.absolutePath)

        //check video format before playing into exoplayer
        if (extension == OptiConstant.AVI_FORMAT) {
            convertAviToMp4() //avi format is not supported in exoplayer
        } else {
            playbackPosition = 0
            currentWindow = 0
            initializePlayer()
        }
    }

    override fun getFile(): File? {
        return masterVideoFile
    }

    override fun reInitPlayer() {
        initializePlayer()
    }


    override fun onAudioFileProcessed(convertedAudioFile: File) {
        TODO("Not yet implemented")
    }

    override fun showLoading(isShow: Boolean) {
        if (isShow) {
            progressBar.visibility = View.VISIBLE
            tvVideoProcessing!!.visibility = View.VISIBLE
            setProgressValue()
        } else {
            progressBar.visibility = View.INVISIBLE
            tvVideoProcessing!!.visibility = View.INVISIBLE
        }
    }

    private fun setProgressValue() {
        var progressStatus = 1

        Thread(Runnable {
            while (progressStatus < 100) {
                progressStatus++
                handler.post {
                    progressBar.progress = progressStatus
                }
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }).start()
    }


    override fun openGallery() {
        checkPermission(OptiConstant.VIDEO_MERGE_2, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun checkPermission(requestCode: Int, permission: String) {
        requestPermissions(arrayOf(permission), requestCode)
    }


    override fun openCamera() {
        TODO("Not yet implemented")
    }


    private val playerListener = object : Player.EventListener {
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        }

        override fun onSeekProcessed() {
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray?,
            trackSelections: TrackSelectionArray?
        ) {
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            Log.v(tagName, "onPlayerError: ${error.toString()}")
            Toast.makeText(mContext, "Video format is not supported", Toast.LENGTH_LONG).show()
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            pbLoading?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        override fun onPositionDiscontinuity(reason: Int) {
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

            if (playWhenReady && playbackState == Player.STATE_READY) {
                // Active playback.
            } else if (playWhenReady) {
                // Not playing because playback ended, the player is buffering, stopped or
                // failed. Check playbackState and player.getPlaybackError for details.
            } else {
                // Paused by app.
            }
        }
    }

    private fun convertAviToMp4() {

        AlertDialog.Builder(requireContext())
            .setTitle(OptiConstant.APP_NAME)
            .setMessage(getString(R.string.not_supported_video))
            .setPositiveButton(getString(R.string.yes)) { dialog, which ->
                //output file is generated and send to video processing
                val outputFile = OptiUtils.createVideoFile(requireContext())
                Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

                OptiVideoEditor.with(requireContext())
                    .setType(OptiConstant.CONVERT_AVI_TO_MP4)
                    .setFile(masterVideoFile!!)
                    .setOutputPath(outputFile.path)
                    .setCallback(this)
                    .main()

                showLoading(true)
            }
            .setNegativeButton(R.string.no) { dialog, which ->
                releasePlayer()
            }
            .show()
    }

    private fun releasePlayer() {
        if (exoPlayer != null) {
            playbackPosition = exoPlayer?.currentPosition!!
            currentWindow = exoPlayer?.currentWindowIndex!!
            playWhenReady = exoPlayer?.playWhenReady
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    private fun initializePlayer() {
        try {
//            tvInfo!!.visibility = View.GONE

            ePlayer?.useController = true
            exoPlayer = ExoPlayerFactory.newSimpleInstance(
                activity,
                DefaultRenderersFactory(activity),
                DefaultTrackSelector(), DefaultLoadControl()
            )

            ePlayer?.player = exoPlayer

            exoPlayer?.playWhenReady = false

            exoPlayer?.addListener(playerListener)

            exoPlayer?.prepare(
                VideoUtils.buildMediaSource(
                    Uri.fromFile(masterVideoFile),
                    VideoFrom.LOCAL
                )
            )

            exoPlayer?.seekTo(0)

            exoPlayer?.seekTo(currentWindow, playbackPosition)
        } catch (exception: Exception) {
            Log.v(tagName, "exception: " + exception.localizedMessage)
        }
    }

    override fun onProgress(progress: String) {
        Log.v(tagName, "onProgress()")
        showLoading(true)
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Log.v(tagName, "onSuccess()")
        showLoading(false)
        onFileProcessed(convertedFile)
    }

    override fun onFailure(error: Exception) {
        Log.v(tagName, "onFailure() ${error.localizedMessage}")
        Toast.makeText(mContext, "Video processing failed", Toast.LENGTH_LONG).show()
        showLoading(false)
    }

    override fun onNotAvailable(error: Exception) {
        Log.v(tagName, "onNotAvailable() ${error.localizedMessage}")
    }

    override fun onFinish() {
        Log.v(tagName, "onFinish()")
        showLoading(false)
    }
}