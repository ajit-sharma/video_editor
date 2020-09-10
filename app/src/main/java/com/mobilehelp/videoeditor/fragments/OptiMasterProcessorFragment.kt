/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.mobilehelp.videoeditor.fragments

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mobilehelp.videoeditor.OptiTrimmerActivity
import com.mobilehelp.videoeditor.OptiVideoEditor
import com.mobilehelp.videoeditor.R
import com.mobilehelp.videoeditor.adapter.OptiVideoOptionsAdapter
import com.mobilehelp.videoeditor.interfaces.OptiFFMpegCallback
import com.mobilehelp.videoeditor.interfaces.OptiVideoOptionListener
import com.mobilehelp.videoeditor.utils.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class OptiMasterProcessorFragment : Fragment(), OptiBaseCreatorDialogFragment.CallBacks,
    OptiVideoOptionListener,
    OptiFFMpegCallback {

    private var tagName: String = OptiMasterProcessorFragment::class.java.simpleName
    private lateinit var rootView: View
    private var videoUri: Uri? = null
    private var videoFile: File? = null
    private var permissionList: ArrayList<String> = ArrayList()
    private lateinit var preferences: SharedPreferences
    private lateinit var progressBar: ProgressBar
    private var tvVideoProcessing: TextView? = null
    private var handler: Handler = Handler()
    private var ibGallery: ImageButton? = null
    private var ibCamera: ImageButton? = null
    private var ibSelectVideo: TextView? = null
    private var masterVideoFile: File? = null
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0
    private var ePlayer: PlayerView? = null
    private var pbLoading: ProgressBar? = null
    private var exoPlayer: SimpleExoPlayer? = null
    private var playWhenReady: Boolean? = false
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var rvVideoOptions: RecyclerView
    private lateinit var optiVideoOptionsAdapter: OptiVideoOptionsAdapter
    private var videoOptions: ArrayList<String> = ArrayList()
    private var orientationLand: Boolean = false
    private var saveNShare: Boolean = false
    private var tvSave: ImageView? = null
    private var tvShare: ImageView? = null
    private var isLargeVideo: Boolean? = false
    private var mContext: Context? = null
    private var tvInfo: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.opti_video_processor_fragment, container, false)
        initView(rootView)
        return rootView
    }

    private fun initView(rootView: View?) {
        ePlayer = rootView?.findViewById(R.id.ePlayer)
        tvSave = rootView?.findViewById(R.id.tvSave)
        tvShare = rootView?.findViewById(R.id.tvShare)
        pbLoading = rootView?.findViewById(R.id.pbLoading)
        ibGallery = rootView?.findViewById(R.id.ibGallery)
        ibCamera = rootView?.findViewById(R.id.ibCamera)
        ibSelectVideo = rootView?.findViewById(R.id.ibSelectVideo)
        progressBar = rootView?.findViewById(R.id.progressBar)!!
        tvVideoProcessing = rootView.findViewById(R.id.tvVideoProcessing)
        tvInfo = rootView.findViewById(R.id.tvInfo)



        preferences =
            requireActivity().getSharedPreferences("fetch_permission", Context.MODE_PRIVATE)

        rvVideoOptions = rootView.findViewById(R.id.rvVideoOptions)!!
        linearLayoutManager = LinearLayoutManager(requireActivity().applicationContext)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvVideoOptions.layoutManager = linearLayoutManager

        mContext = context
        Temp.newInstance().apply {
            setHelper(this@OptiMasterProcessorFragment)
        }.show(requireFragmentManager(), "Temp")

        //add video editing options
        //videoOptions.add(OptiConstant.FLIRT)
        videoOptions.add(OptiConstant.TRIM)
        videoOptions.add(OptiConstant.MUSIC)
        videoOptions.add(OptiConstant.PLAYBACK)
        videoOptions.add(OptiConstant.TEXT)
        videoOptions.add(OptiConstant.OBJECT)
        videoOptions.add(OptiConstant.MERGE)
        //videoOptions.add(OptiConstant.TRANSITION)

        optiVideoOptionsAdapter =
            OptiVideoOptionsAdapter(
                videoOptions,
                requireActivity().applicationContext,
                this,
                orientationLand
            )
        rvVideoOptions.adapter = optiVideoOptionsAdapter
        optiVideoOptionsAdapter.notifyDataSetChanged()

//        checkStoragePermission(OptiConstant.PERMISSION_STORAGE)

        //load FFmpeg
        try {
            FFmpeg.getInstance(activity).loadBinary(object : FFmpegLoadBinaryResponseHandler {
                override fun onFailure() {
                    Log.v("FFMpeg", "Failed to load FFMpeg library.")
                }

                override fun onSuccess() {
                    Log.v("FFMpeg", "FFMpeg Library loaded!")
                }

                override fun onStart() {
                    Log.v("FFMpeg", "FFMpeg Started")
                }

                override fun onFinish() {
                    Log.v("FFMpeg", "FFMpeg Stopped")
                }
            })
        } catch (e: FFmpegNotSupportedException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ibGallery?.setOnClickListener {
//            openGallery()
        }

        ibCamera?.setOnClickListener {
//            openCamera()
        }


        ibSelectVideo?.setOnClickListener {
            Temp.newInstance().apply {
                setHelper(this@OptiMasterProcessorFragment)
            }.show(requireFragmentManager(), "Temp")
        }



        tvSave?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(OptiConstant.APP_NAME)
                .setMessage(getString(R.string.save_video))
                .setPositiveButton(getString(R.string.Continue)) { dialog, which ->
                    if (masterVideoFile != null) {
                        val outputFile = createSaveVideoFile()
                        OptiCommonMethods.copyFile(masterVideoFile, outputFile)
                        Toast.makeText(context, R.string.successfully_saved, Toast.LENGTH_SHORT)
                            .show()
                        OptiUtils.refreshGallery(outputFile.absolutePath, requireContext())
                        tvSave!!.visibility = View.GONE
                        tvShare!!.visibility = View.VISIBLE
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, which -> }
                .show()
        }

        tvShare?.setOnClickListener {
//            AlertDialog.Builder(context!!)
//                .setTitle(OptiConstant.APP_NAME)
//                .setMessage(getString(R.string.save_video))
//                .setPositiveButton(getString(R.string.Continue)) { dialog, which ->
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
                startActivity(Intent.createChooser(sharingIntent, "Share Image Using"))

//                ShareCompat.IntentBuilder.from(activity)
//                    .setStream(screenshotUri)
//                    .setType(URLConnection.guessContentTypeFromName("share Video"))
//                    .startChooser();

//                val outputFile = createSaveVideoFile()
//                val sharingIntent = Intent(Intent.ACTION_SEND)
//                val screenshotUri = Uri.fromFile(masterVideoFile)
//                sharingIntent.type = " *video/*"
//                sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri)
//                startActivity(Intent.createChooser(sharingIntent, "Share image using"))

                Toast.makeText(context, R.string.successfully_share, Toast.LENGTH_SHORT)
                    .show()
            }
//                }
//                .setNegativeButton(R.string.cancel) { dialog, which -> }
//                .show()


        }

        tvInfo?.setOnClickListener {
            OptiVideoOptionFragment.newInstance().apply {
                setHelper(this@OptiMasterProcessorFragment)
            }.show(requireFragmentManager(), "OptiVideoOptionFragment")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig!!)
        //for playing video in landscape mode
        if (newConfig!!.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.v(tagName, "orientation: ORIENTATION_LANDSCAPE")
            orientationLand = true
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.v(tagName, "orientation: ORIENTATION_PORTRAIT")
            orientationLand = false
        }
        optiVideoOptionsAdapter =
            OptiVideoOptionsAdapter(
                videoOptions,
                requireActivity().applicationContext,
                this,
                orientationLand
            )
        rvVideoOptions.adapter = optiVideoOptionsAdapter
        optiVideoOptionsAdapter.notifyDataSetChanged()
    }

    override fun onAudioFileProcessed(convertedAudioFile: File) {

    }

    override fun reInitPlayer() {
        initializePlayer()
    }

    override fun onDidNothing() {
        initializePlayer()
    }

    override fun onFileProcessed(file: File) {
        tvSave!!.visibility = View.VISIBLE
        masterVideoFile = file
        isLargeVideo = false
        ibSelectVideo!!.isClickable = true
        tvSave!!.visibility = View.GONE
        tvShare!!.visibility = View.VISIBLE


        val extension = OptiCommonMethods.getFileExtension(masterVideoFile!!.absolutePath)

        //check video format before playing into exoplayer
        if (extension == OptiConstant.AVI_FORMAT) {
            convertAviToMp4() //avi format is not supported in exoplayer
        } else {
            playbackPosition = 0
            currentWindow = 0
            initializePlayer()
        }

        if (saveNShare) {


            Log.v("Temp", "saveVideo" + saveNShare)

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
                startActivity(Intent.createChooser(sharingIntent, "Share Image Using"))
                Toast.makeText(context, R.string.successfully_share, Toast.LENGTH_SHORT)
                    .show()
            }

        } else {

            Log.v("onFileProcessed", "saveVideo" + saveNShare)
            if (masterVideoFile != null) {
                val outputFile = createSaveVideoFile()
                OptiCommonMethods.copyFile(masterVideoFile, outputFile)
                Toast.makeText(context, R.string.successfully_saved, Toast.LENGTH_SHORT)
                    .show()
                OptiUtils.refreshGallery(outputFile.absolutePath, requireContext())

            }

        }


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

    override fun getFile(): File? {
        return masterVideoFile
    }


    fun checkAllPermission(permission: Array<String>) {
        val blockedPermission = checkHasPermission(activity, permission)
        if (blockedPermission != null && blockedPermission.size > 0) {
            val isBlocked = isPermissionBlocked(activity, blockedPermission)
            if (isBlocked) {
                callPermissionSettings()
            } else {
                requestPermissions(permission, OptiConstant.RECORD_VIDEO)
            }
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            videoFile = OptiUtils.createVideoFile(requireContext())
            Log.v(tagName, "videoPath1: " + videoFile!!.absolutePath)
            videoUri = FileProvider.getUriForFile(
                requireContext(),
                "com.mobilehelp.videoeditor.provider", videoFile!!
            )
            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 240) //4 minutes
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoFile)
            startActivityForResult(cameraIntent, OptiConstant.RECORD_VIDEO)
        }
    }

    private fun checkStoragePermission(permission: Array<String>) {
        val blockedPermission = checkHasPermission(activity, permission)
        if (blockedPermission != null && blockedPermission.size > 0) {
            val isBlocked = isPermissionBlocked(activity, blockedPermission)
            if (isBlocked) {
                callPermissionSettings()
            } else {
                requestPermissions(permission, OptiConstant.ADD_ITEMS_IN_STORAGE)
            }
        } else {
            itemStorageAction()
        }
    }

    private fun itemStorageAction() {
        val sessionManager = OptiSessionManager()

        if (sessionManager.isFirstTime(requireActivity())) {
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_1,
                "sticker_1",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_2,
                "sticker_2",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_3,
                "sticker_3",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_4,
                "sticker_4",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_5,
                "sticker_5",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_6,
                "sticker_6",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_7,
                "sticker_7",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_8,
                "sticker_8",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_9,
                "sticker_9",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_10,
                "sticker_10",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_11,
                "sticker_11",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_12,
                "sticker_12",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_13,
                "sticker_13",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_14,
                "sticker_14",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_15,
                "sticker_15",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_16,
                "sticker_16",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_17,
                "sticker_17",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_18,
                "sticker_18",
                requireContext()
            )
            OptiUtils.copyFileToInternalStorage(
                R.drawable.sticker_19,
                "sticker_19",
                requireContext()
            )

            OptiUtils.copyFontToInternalStorage(
                R.font.roboto_black,
                "roboto_black",
                requireContext()
            )

            sessionManager.setFirstTime(requireActivity(), false)
        }
    }

    private var isFirstTimePermission: Boolean
        get() = preferences.getBoolean("isFirstTimePermission", false)
        set(isFirstTime) = preferences.edit().putBoolean("isFirstTimePermission", isFirstTime)
            .apply()

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

            OptiConstant.VIDEO_GALLERY -> {
                data?.let {
                    setFilePath(resultCode, it, OptiConstant.VIDEO_GALLERY)
                }
            }

            OptiConstant.RECORD_VIDEO -> {
                data?.let {
                    Log.v(tagName, "data: " + data.data)

                    if (resultCode == RESULT_OK) {
                        masterVideoFile = OptiCommonMethods.writeIntoFile(activity, data, videoFile)

                        val timeInMillis =
                            OptiUtils.getVideoDuration(requireContext(), masterVideoFile!!)
                        Log.v(tagName, "timeInMillis: $timeInMillis")
                        val duration = OptiCommonMethods.convertDurationInMin(timeInMillis)
                        Log.v(tagName, "videoDuration: $duration")

                        //check if video is more than 4 minutes
                        if (duration < OptiConstant.VIDEO_LIMIT) {
                            playbackPosition = 0
                            currentWindow = 0
                            initializePlayer()
                        } else {
                            Toast.makeText(
                                activity,
                                getString(R.string.error_select_smaller_video),
                                Toast.LENGTH_SHORT
                            ).show()

                            val uri = Uri.fromFile(masterVideoFile)
                            val intent = Intent(context, OptiTrimmerActivity::class.java)
                            intent.putExtra("VideoPath", masterVideoFile!!.absolutePath)
                            intent.putExtra(
                                "VideoDuration",
                                OptiCommonMethods.getMediaDuration(context, uri)
                            )
                            startActivityForResult(intent, OptiConstant.MAIN_VIDEO_TRIM)
                        }
                    }
                }
            }

            OptiConstant.MAIN_VIDEO_TRIM -> {
                if (resultCode == RESULT_OK) {
                    val startPosition = data!!.getIntExtra("startPosition", 0)
                    val endPosition = data.getIntExtra("endPosition", 0)

                    val startPos = VideoUtils.secToTime(startPosition.toLong())
                    val endPos = VideoUtils.secToTime(endPosition.toLong())
                    Log.v(tagName, "startPos: $startPos, endPos: $endPos")

                    val outputFile = OptiUtils.createVideoFile(requireContext())
                    Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

                    OptiVideoEditor.with(requireContext())
                        .setType(OptiConstant.VIDEO_TRIM)
                        .setFile(masterVideoFile!!)
                        .setOutputPath(outputFile.path)
                        .setStartTime(startPos)
                        .setEndTime(endPos)
                        .setCallback(this)
                        .main()

                    showLoading(true)
                }
            }
        }
    }

    private fun setFilePath(resultCode: Int, data: Intent, mode: Int) {

        if (resultCode == RESULT_OK) {
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
                    if (mode == OptiConstant.VIDEO_GALLERY) {
                        Log.v(tagName, "filePath: $filePath")
                        masterVideoFile = File(filePath)

                        val extension =
                            OptiCommonMethods.getFileExtension(masterVideoFile!!.absolutePath)

                        val timeInMillis =
                            OptiUtils.getVideoDuration(requireContext(), masterVideoFile!!)
                        Log.v(tagName, "timeInMillis: $timeInMillis")
                        val duration = OptiCommonMethods.convertDurationInMin(timeInMillis)
                        Log.v(tagName, "videoDuration: $duration")

                        //check if video is more than 4 minutes
                        if (duration < OptiConstant.VIDEO_LIMIT) {
                            //check video format before playing into exoplayer
                            if (extension == OptiConstant.AVI_FORMAT) {
                                convertAviToMp4() //avi format is not supported in exoplayer
                            } else {
                                playbackPosition = 0
                                currentWindow = 0
                                initializePlayer()
                            }
                        } else {
                            Toast.makeText(
                                activity,
                                getString(R.string.error_select_smaller_video),
                                Toast.LENGTH_SHORT
                            ).show()

                            isLargeVideo = true
                            val uri = Uri.fromFile(masterVideoFile)
                            val intent = Intent(context, OptiTrimmerActivity::class.java)
                            intent.putExtra("VideoPath", filePath)
                            intent.putExtra(
                                "VideoDuration",
                                OptiCommonMethods.getMediaDuration(context, uri)
                            )
                            startActivityForResult(intent, OptiConstant.MAIN_VIDEO_TRIM)
                        }
                    }
                }
            } catch (e: Exception) {

            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || exoPlayer == null) {
            masterVideoFile?.let {
                if (!isLargeVideo!!) { //for the larger video player shouldn't resume on cancel in trimming view
                    initializePlayer()
                }
            }
        }
    }

    /*override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            masterVideoFile?.let {
                initializePlayer()
            }
        }
    }*/


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
            tvInfo!!.visibility = View.GONE

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

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    fun checkPermission(requestCode: Int, permission: String) {
        requestPermissions(arrayOf(permission), requestCode)
    }

    override fun openGallery() {
        releasePlayer()
        checkPermission(OptiConstant.VIDEO_GALLERY, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    override fun openCamera() {
        releasePlayer()
        checkAllPermission(OptiConstant.PERMISSION_CAMERA)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {

            OptiConstant.VIDEO_GALLERY -> {
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
                                activity as Activity,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            //call the gallery intent
                            OptiUtils.refreshGalleryAlone(requireContext())
                            val i = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            )
                            i.type = "video/*"
                            i.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*"))
                            startActivityForResult(i, OptiConstant.VIDEO_GALLERY)
                        } else {
                            callPermissionSettings()
                        }
                    }
                }
                return
            }

            OptiConstant.AUDIO_GALLERY -> { //not used
                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity as Activity,
                            permission
                        )
                    ) {
                        Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                    } else {
                        if (ActivityCompat.checkSelfPermission(
                                activity as Activity,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            //call the gallery intent
                            OptiUtils.refreshGalleryAlone(requireContext())
                            val i = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            )
                            i.type = "video/*"
                            i.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*"))
                            startActivityForResult(i, OptiConstant.AUDIO_GALLERY)
                        } else {
                            callPermissionSettings()
                        }
                    }
                }
                return
            }

            OptiConstant.RECORD_VIDEO -> {
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
                            //call the camera intent
                            val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                            videoFile = OptiUtils.createVideoFile(requireContext())
                            Log.v(tagName, "videoPath1: " + videoFile!!.absolutePath)
                            videoUri = FileProvider.getUriForFile(
                                requireContext(),
                                "com.mobilehelp.videoeditor.provider", videoFile!!
                            )
                            cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 240) //4 minutes
                            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoFile)
                            startActivityForResult(cameraIntent, OptiConstant.RECORD_VIDEO)
                        } else {
                            callPermissionSettings()
                        }
                    }
                }
                return
            }

            OptiConstant.ADD_ITEMS_IN_STORAGE -> {
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
                            itemStorageAction()
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

    private fun createSaveVideoFile(): File {
        val timeStamp: String =
            SimpleDateFormat(OptiConstant.DATE_FORMAT, Locale.getDefault()).format(Date())
        val imageFileName: String = OptiConstant.APP_NAME + timeStamp + "_"

        val path =
            Environment.getExternalStorageDirectory()
                .toString() + File.separator + OptiConstant.APP_NAME + File.separator + OptiConstant.MY_VIDEOS + File.separator
        val folder = File(path)
        if (!folder.exists())
            folder.mkdirs()

        return File.createTempFile(imageFileName, OptiConstant.VIDEO_FORMAT, folder)
    }

    private fun showBottomSheetDialogFragment(bottomSheetDialogFragment: BottomSheetDialogFragment) {
        val bundle = Bundle()
        bottomSheetDialogFragment.arguments = bundle
        bottomSheetDialogFragment.show(requireFragmentManager(), bottomSheetDialogFragment.tag)
    }

    override fun videoOption(option: String) {
        //based on selected video editing option - helper, file is passed
        when (option) {
            OptiConstant.FLIRT -> {
                masterVideoFile?.let { file ->
                    val filterFragment = OptiFilterFragment()
                    filterFragment.setHelper(this@OptiMasterProcessorFragment)
                    filterFragment.setFilePathFromSource(file)
                    showBottomSheetDialogFragment(filterFragment)
                }

                if (masterVideoFile == null) {
                    OptiUtils.showGlideToast(
                        requireActivity(),
                        getString(R.string.error_filter)
                    )
                }
            }






          

        }
    }

    override fun onProgress(progress: String) {
        Log.v(tagName, "onProgress()")
        ibSelectVideo!!.isClickable = false
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

    fun onAddSaveNShareSubmit(saveOrShare: Boolean) {
        saveNShare = saveOrShare
        Log.v("onAddSaveNShareSubmit", "saveVideo" + saveOrShare)

    }
}