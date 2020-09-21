package com.mobilehelp.videoeditor.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mobilehelp.videoeditor.VideoEditor
import com.mobilehelp.videoeditor.R
import com.mobilehelp.videoeditor.alphaVideo.AlphaMovieView
import com.mobilehelp.videoeditor.interfaces.FFMpegCallback
import com.mobilehelp.videoeditor.utils.*
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import kotlinx.android.synthetic.main.view_switcher_camera_view.*
import org.jetbrains.anko.support.v4.longToast
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class SingleFragment : BaseFragment(),
    FFMpegCallback, View.OnTouchListener, BaseFragment.CallBacks {

    private var tagName: String = SingleFragment::class.java.simpleName

    private lateinit var rootView: View
    lateinit var viewSwitcher: ViewSwitcher
    private var camera: CameraView? = null
    private var fabVideo: ImageView? = null
    private var fabPicture: ImageView? = null
    private var fabPreview: FloatingActionButton? = null
    private var fabFront: FloatingActionButton? = null
    private var fabSave: FloatingActionButton? = null
    private lateinit var overlayVideo: AlphaMovieView
    var _root: RelativeLayout? = null
    private var backGroundVideoResult: VideoResult? = null


    private var videoView: VideoView? = null
    private var alphaMovieView: AlphaMovieView? = null
    private var fabPlay: ImageView? = null
    private var imgCancel: ImageView? = null
    private var imgSave: ImageView? = null
    private var imgShare: ImageView? = null


    private var display: Display? = null
    private var size: Point? = null
    private var mContext: Context? = null
    private lateinit var preferences: SharedPreferences
    private var permissionList: ArrayList<String> = ArrayList()


    private var videoFileOne: File? = null
    private var videoFileTwo: File? = null
    private var videoUri: Uri? = null


    private var _xDelta = 0f
    private var _yDelta = 0f
    private var _xDeltaTemp = 0f
    private var _yDeltaTemp = 0f
    private var location = IntArray(2)

    private var masterVideoFile: File? = null
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0
    private var ePlayer: PlayerView? = null
    private var pbLoading: ProgressBar? = null
    private var exoPlayer: SimpleExoPlayer? = null
    private var playWhenReady: Boolean? = false
    private var outputFile: File? = null
    private var mHandler: Handler? = null
    private val mInterval = 5000L


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.single_fragment_view_switcher, container, false)
        initView(rootView)
        return rootView
    }

    private fun initView(rootView: View?) {


        viewSwitcher = rootView?.findViewById(R.id.view_switcher)!!

        size = Point()
        display = requireActivity().windowManager.defaultDisplay
        display!!.getSize(size)
        _root = rootView?.findViewById(R.id.root)
        _root!!.setOnTouchListener(this)

        fabVideo = rootView?.findViewById(R.id.fab_video)
        fabPreview = rootView?.findViewById(R.id.fab_preview)
        fabPicture = rootView?.findViewById(R.id.fab_picture)
        fabFront = rootView?.findViewById(R.id.fab_front)
        fabSave = rootView?.findViewById(R.id.save_overlay_view)
        overlayVideo = rootView?.findViewById(R.id.edit_video_player)!!
        overlayVideo.visibility = View.GONE


        videoView = rootView!!.findViewById(R.id.video)
        alphaMovieView = rootView!!.findViewById(R.id.alphaMovie)
        fabPlay = rootView!!.findViewById(R.id.play_video)
        imgCancel = rootView!!.findViewById(R.id.cancel_video)
        imgSave = rootView!!.findViewById(R.id.save_video)
        imgShare = rootView!!.findViewById(R.id.share_video)
        mHandler = Handler()



        mContext = context
        preferences =
            requireActivity().getSharedPreferences("fetch_permission", Context.MODE_PRIVATE)

        viewSwitcher = rootView?.findViewById(R.id.view_switcher)!!
        camera = rootView.findViewById(R.id.camera)
        camera!!.setLifecycleOwner(this)


        camera!!.videoMaxDuration = 120 * 1000 // max 2mins

        camera!!.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {

            }


            override fun onVideoTaken(result: VideoResult) {
                super.onVideoTaken(result)
                result?.let { file ->
                    backGroundVideoResult = file
                    showPreview(
                        file,
                        videoUri,
                        videoFileTwo,
                        _xDeltaTemp,
                        _yDeltaTemp,
                        location[0],
                        location[1]
                    )
                    doStoreVideo()

                }
            }

        })

        fabFront!!.setOnClickListener {

            if (camera!!.isTakingPicture || camera!!.isTakingVideo) return@setOnClickListener
            when (camera!!.toggleFacing()) {
                Facing.BACK -> fabFront!!.setImageResource(R.drawable.ic_camera_front_black_24dp)
                Facing.FRONT -> fabFront!!.setImageResource(R.drawable.ic_camera_rear_black_24dp)
            }
        }


        fabVideo!!.setOnClickListener {

            openCamera()
        }

        fabPicture!!.setOnClickListener {

            openGallery()
        }

    }

    private fun showPreview(
        result: VideoResult,
        videoUri: Uri?,
        videoFileTwo: File?,
        _xDeltaTemp: Float,
        _yDeltaTemp: Float,
        x: Int,
        y: Int
    ) {

        viewSwitcher.showNext()
        var videoResult: WeakReference<VideoResult>? = null
        videoResult = result?.let { WeakReference(it) }

        val result: VideoResult? =
            if (videoResult == null) null else videoResult!!.get()

        val controller = MediaController(activity)
        controller.setAnchorView(videoView)
        controller.setMediaPlayer(videoView)
        videoView!!.setMediaController(controller)
        videoView!!.setVideoURI(Uri.fromFile(result!!.file))
        alphaMovieView!!.setVideoFromUri(requireContext(), videoUri)
        alphaMovieView!!.start()

        alphaMovieView!!.animate()
            .x(_xDeltaTemp)
            .y(_yDeltaTemp)
            .setDuration(0)
            .start()

        Log.e("OptiVideo", "PreviewFragment" + "-------" + _xDelta + "-----" + _yDelta)

        videoView!!.setOnPreparedListener { mp ->
            val lp = videoView!!.layoutParams
            val videoWidth = mp.videoWidth.toFloat()
            val videoHeight = mp.videoHeight.toFloat()
            val viewWidth = videoView!!.width.toFloat()
            lp.height = (viewWidth * (videoHeight / videoWidth)).toInt()
            videoView!!.layoutParams = lp
            if (videoView!!.isPlaying) return@setOnPreparedListener
            fabPlay!!.visibility = View.GONE
            videoView!!.start()
            if (result.isSnapshot) {
                // Log the real size for debugging reason.
                Log.e(
                    "VideoPreview",
                    "The video full size is " + videoWidth + "x" + videoHeight
                )
            }
        }
        videoView!!.setOnCompletionListener { mp ->

            fabPlay!!.visibility = View.VISIBLE
//            dialog!!.dismiss()

        }

        imgSave!!.setOnClickListener {

            doStoreVideo()
        }

        imgCancel!!.setOnClickListener {

            if (!isRunning()) {
                viewSwitcher.showPrevious()
                masterVideoFile = null
                outputFile = null
                overlayVideo.stop()
                overlayVideo.visibility = View.GONE
                videoView!!.stopPlayback()
                CameraView(mContext!!)
                viewSwitcher.reset()
                reInitView()

            } else {

                stopRunningProcess()
                viewSwitcher.showPrevious()
                masterVideoFile = null
                outputFile = null
                overlayVideo.stop()
                overlayVideo.visibility = View.GONE
                videoView!!.stopPlayback()
                CameraView(mContext!!)
                viewSwitcher.reset()
                reInitView()

            }
        }



        imgShare!!.setOnClickListener {
            if (masterVideoFile != null) {

                val screenshotUri = Uri.fromFile(masterVideoFile)

                val file = File("File Path")
                val apkURI = FileProvider.getUriForFile(
                    requireActivity(), requireActivity().packageName.toString() + ".provider",
                    masterVideoFile!!
                )
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                sharingIntent.type = "video/*"
                sharingIntent.putExtra(Intent.EXTRA_STREAM, apkURI)
                startActivity(Intent.createChooser(sharingIntent, "Share Video Using"))


            }
        }
    }

    private fun reInitView() {
        viewSwitcher = rootView?.findViewById(R.id.view_switcher)!!
        camera_view_layout.visibility = View.VISIBLE
        camera = rootView.findViewById(R.id.camera)
        camera!!.setLifecycleOwner(this)
    }


    private fun createSaveVideoFile(): File {
        val timeStamp: String =
            SimpleDateFormat(Constant.DATE_FORMAT, Locale.getDefault()).format(Date())
        val imageFileName: String = Constant.APP_NAME + timeStamp + "_"

        val path =
            Environment.getExternalStorageDirectory()
                .toString() + File.separator + Constant.APP_NAME + File.separator + Constant.MY_VIDEOS + File.separator
        val folder = File(path)
        if (!folder.exists())
            folder.mkdirs()

        return File.createTempFile(imageFileName, Constant.VIDEO_FORMAT, folder)
    }

    fun doStoreVideo() {

        if (!isRunning()) {

            if (outputFile != null) {

                longToast("File Already Saved!")

            } else if (masterVideoFile != null) {

                outputFile = createSaveVideoFile()
                CommonMethods.copyFile(masterVideoFile, outputFile)

                OptiUtils.refreshGallery(outputFile!!.absolutePath, requireContext())
                onComplete("Saved Successfully...", true)

            } else {

                if (videoFileOne != null && videoFileTwo != null) {
                    //output file is generated and send to video processing
                    val outputFile = OptiUtils.createVideoFile(requireContext())
                    Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

                    VideoEditor.with(requireContext())
                        .setType(Constant.VIDEO_CLIP_VIDEO_OVERLAY)
                        .setFile(videoFileOne!!)
                        .setFileTwo(videoFileTwo!!)
                        .setPosition(VideoEditor.TOP_LEFT)
                        .setVideoPosition(location[0], location[1])
                        .setOutputPath(outputFile.path)
                        .setCallback(this)
                        .main()


                } else {
                    longToast(R.string.error_merge)
                }

            }

        } else {

            if (masterVideoFile != null) {

                outputFile = createSaveVideoFile()
                CommonMethods.copyFile(masterVideoFile, outputFile)
                Toast.makeText(context, R.string.successfully_saved, Toast.LENGTH_SHORT)
                    .show()
                OptiUtils.refreshGallery(outputFile!!.absolutePath, requireContext())
                onComplete("Saved Successfully...", true)
            } else {

                showInProgressToast("Video In Process....")
                startRepeatingTask()

            }

        }


    }


    override fun onDidNothing() {
        initializePlayer()
    }

    override fun onFileProcessed(file: File) {
        masterVideoFile = file

        val extension = CommonMethods.getFileExtension(masterVideoFile!!.absolutePath)

        //check video format before playing into exoplayer
        if (extension == Constant.AVI_FORMAT) {
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
    }

    override fun showLoading(isShow: Boolean) {
    }

    fun openGallery() {
        checkPermission(Constant.VIDEO_MERGE_2, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun openCamera() {
//        checkAllPermission(OptiConstant.PERMISSION_CAMERA)
        captureVideoSnapshot()
    }

    private fun convertAviToMp4() {

        AlertDialog.Builder(requireContext())
            .setTitle(Constant.APP_NAME)
            .setMessage(getString(R.string.not_supported_video))
            .setPositiveButton(getString(R.string.yes)) { dialog, which ->
                //output file is generated and send to video processing
                outputFile = OptiUtils.createVideoFile(requireContext())
                Log.v(tagName, "outputFile: ${outputFile!!.absolutePath}")

                VideoEditor.with(requireContext())
                    .setType(Constant.CONVERT_AVI_TO_MP4)
                    .setFile(masterVideoFile!!)
                    .setOutputPath(outputFile!!.path)
                    .setCallback(this)
                    .main()

                showLoading(true)
            }
            .setNegativeButton(R.string.no) { dialog, which ->
                releasePlayer()
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    fun captureVideoSnapshot() {
        if (videoFileTwo !== null) {
//            startOverlayChromakey(videoFileTwo!!)
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

    fun startOverlayChromakey(videoFileTwo: File) {

        if (videoFileTwo != null) {

//            //output file is generated and send to video processing
            outputFile = OptiUtils.createVideoFile(requireContext())
            Log.v(tagName, "outputFile: ${outputFile!!.absolutePath}")

            VideoEditor.with(requireContext())
                .setType(Constant.VIDEO_CLIP_ART_OVERLAY)
                .setFile(videoFileTwo)
                .setPosition(VideoEditor.TOP_LEFT)
                .setVideoPosition(location[0], location[1])
                .setOutputPath(outputFile!!.path)
                .setCallback(this)
                .main()


        } else {
            longToast(R.string.error_merge)
        }

//        val outputFile = OptiUtils.createVideoFile(requireContext())
//        OptiVideoEditor.with(requireContext())
//            .setType(OptiConstant.MERGE_VIDEO)
//            .setFileTwo(videoFileTwo)
//            .setPosition(OptiVideoEditor.TOP_LEFT)
//            .setOutputPath(outputFile.path)
//            .setCallback(this)
//            .main()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_CANCELED) return

        when (requestCode) {

            Constant.VIDEO_MERGE_2 -> {
                data?.let {
                    videoUri = it.data
                    overlayVideo.visibility = View.VISIBLE
                    overlayVideo.setVideoFromUri(mContext, videoUri)
                    overlayVideo.start()
                    fabVideo!!.visibility = View.VISIBLE
                    setFilePath(resultCode, it, Constant.VIDEO_MERGE_2)
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
                    if (mode == Constant.VIDEO_MERGE_1) {
                        videoFileOne = File(filePath)
                        Log.v(tagName, "videoFileOne: " + videoFileOne!!.absolutePath)


                    } else if (mode == Constant.VIDEO_MERGE_2) {
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
            Constant.VIDEO_MERGE_1 -> {

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

            Constant.VIDEO_MERGE_2 -> {


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
                            startActivityForResult(i, Constant.VIDEO_MERGE_2)
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
                requestPermissions(permission, Constant.VIDEO_MERGE_1)
            }
        } else {
            captureVideoSnapshot()
        }
    }




    override fun onProgress(progress: String) {
//        Toast.makeText(mContext, "onProgress()", Toast.LENGTH_LONG).show()
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Toast.makeText(mContext, "Video onSuccess", Toast.LENGTH_LONG).show()
        onFileProcessed(convertedFile)
    }

    override fun onFailure(error: Exception) {
        Log.v(tagName, "onFailure() ${error.localizedMessage}")
        Toast.makeText(mContext, "Video processing failed", Toast.LENGTH_LONG).show()
    }

    override fun onNotAvailable(error: Exception) {
        Toast.makeText(mContext, "onNotAvailable", Toast.LENGTH_LONG).show()
    }

    override fun onFinish() {
        onComplete("Save Successfully!", false)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return if (v!!.id == v.id) {
            when (event!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    _xDelta = v.x - event.rawX
                    _yDelta = v.y - event.rawY
                    Log.e("ACTION_DOWN$_xDelta", "=====$_yDelta")

                }
                MotionEvent.ACTION_MOVE -> {
                    v.animate()
                        .x(event.rawX + _xDelta)
                        .y(event.rawY + _yDelta)
                        .setDuration(0)
                        .start()

                    _xDeltaTemp = event.rawX + _xDelta
                    _yDeltaTemp = event.rawY + _yDelta
                    Log.e(
                        "ACTION_MOVE", "ACTION_MOVE" + event.rawX + _xDelta +
                                "=====" + event.rawY + _yDelta
                    )



                    v.getLocationOnScreen(location)
//                    textView.getLocationOnScreen(location);

                    Log.e(
                        "ACTION_MOVE",
                        "ACTION_MOVE" + "--" + "X axis is " + location[0] + "----" + "and Y axis is " + location[1]
                    )

                }

                else -> return false
            }
            true
        } else {
            false
        }
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

    var mStatusChecker: Runnable = object : Runnable {


        override fun run() {
            try {

                if (masterVideoFile != null) {

                    outputFile = createSaveVideoFile()
                    CommonMethods.copyFile(masterVideoFile, outputFile)
                    Toast.makeText(context, R.string.successfully_saved, Toast.LENGTH_SHORT)
                        .show()
                    OptiUtils.refreshGallery(outputFile!!.absolutePath, requireContext())
                    onComplete("Saved Successfully...", true)

                }
                //this function can change value of mInterval.
            } finally {

                if (masterVideoFile != null) {

                    stopRepeatingTask()
                } else {
                    mHandler!!.postDelayed(this, mInterval)
                }
                // 100% guarantee that this always happens, even if
                // your update method throws an exception


            }
        }
    }

    override fun onResume() {
        super.onResume()
        reInitView()
    }

    fun startRepeatingTask() {
        mStatusChecker.run()
    }

    fun stopRepeatingTask() {
        mHandler!!.removeCallbacks(mStatusChecker)
    }


}