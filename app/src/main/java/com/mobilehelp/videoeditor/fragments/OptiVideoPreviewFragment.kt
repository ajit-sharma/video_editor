package com.mobilehelp.videoeditor.fragments


import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import com.mobilehelp.videoeditor.R
import com.mobilehelp.videoeditor.alphaVideo.AlphaMovieView
import com.mobilehelp.videoeditor.interfaces.OptiFFMpegCallback
import com.mobilehelp.videoeditor.interfaces.OptiVideoHelper
import com.otaliastudios.cameraview.VideoResult
import java.io.File
import java.lang.ref.WeakReference


class OptiVideoPreviewFragment : OptiBaseCreatorDialogFragment(), OptiVideoHelper,
    OptiFFMpegCallback {

    private var videoView: VideoView? = null
    private var alphaMovieView: AlphaMovieView? = null
    private lateinit var done: ImageView
    private lateinit var close: ImageView
    private var overlayUri: Uri? = null
    private var videoFileTwo: File? = null
    private var filePath: String? = null
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null


    private var videoResult: WeakReference<VideoResult>? = null


    fun setVideoResult(
        result: VideoResult?,
        uri: Uri?,
        videoFileTwo: File?
    ) {
        videoResult = result?.let { WeakReference(it) }
        overlayUri = uri

        filePath = overlayUri!!.path
        this.videoFileTwo = videoFileTwo
        val path = this.videoFileTwo!!.absolutePath
    }

    companion object {
        fun newInstance() = OptiVideoPreviewFragment()
    }

    private var tagName: String = OptiVideoPreviewFragment::class.java.simpleName
    private lateinit var rootView: View
    override fun permissionsBlocked() {
        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.activity_video_preview, container, false)
        initView(rootView)
        return rootView
    }

    private fun initView(rootView: View) {
        videoView = rootView.findViewById(R.id.video)
        alphaMovieView = rootView.findViewById(R.id.alphaMovie)

        done = rootView.findViewById(R.id.iv_done)
        close = rootView.findViewById(R.id.iv_close)

        videoView!!.setOnClickListener { playVideo() }

        val result: VideoResult? =
            if (videoResult == null) null else videoResult!!.get()

        if (result == null) {
            dialog!!.dismiss()
            return
        }

        done.setOnClickListener {
            dialog!!.dismiss()
        }

        close.setOnClickListener {
            dialog!!.dismiss()
        }
//        done?.setOnClickListener {
//
//            val outputFile = OptiUtils.createVideoFile(requireContext())
//            Log.v(tagName, "outputFile: ${outputFile.absolutePath}")
//
//
//            OptiVideoEditor.with(requireContext())
//                .setType(OptiConstant.VIDEO_CLIP_VIDEO_OVERLAY)
//                .setFile(result.file)
//                .setFileTwo(videoFileTwo!!)
//                .setPosition(OptiVideoEditor.TOP_LEFT)
//                .setOutputPath(outputFile.path)
//                .setCallback(this)
//                .main()
//
//            helper?.showLoading(true)
////
//
//        }


        val controller = MediaController(activity)
        controller.setAnchorView(videoView)
        controller.setMediaPlayer(videoView)
        videoView!!.setMediaController(controller)
        videoView!!.setVideoURI(Uri.fromFile(result.file))
        alphaMovieView!!.setVideoFromUri(requireContext(), overlayUri)
        videoView!!.setOnPreparedListener { mp ->
            val lp = videoView!!.layoutParams
            val videoWidth = mp.videoWidth.toFloat()
            val videoHeight = mp.videoHeight.toFloat()
            val viewWidth = videoView!!.width.toFloat()
            lp.height = (viewWidth * (videoHeight / videoWidth)).toInt()
            videoView!!.layoutParams = lp
            playVideo()
            if (result.isSnapshot) {
                // Log the real size for debugging reason.
                Log.e(
                    "VideoPreview",
                    "The video full size is " + videoWidth + "x" + videoHeight
                )
            }
        }
        videoView!!.setOnCompletionListener { mp ->
            dialog!!.dismiss()
        }
    }


    fun playVideo() {
        if (videoView!!.isPlaying) return
        videoView!!.start()
        alphaMovieView!!.start()
    }

    override fun setHelper(helper: CallBacks) {
        this.helper = helper
    }

    override fun setFilePathFromSource(backgroundUri: Uri, overlayUri: Uri) {
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
        Toast.makeText(activity, "Video processing failed", Toast.LENGTH_LONG).show()
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


    private fun setFilePath(uri: Uri?) {
        try {
            //  Log.e("selectedImage==>", "" + selectedImage)
            val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
            val cursor = requireContext().contentResolver
                .query(uri!!, filePathColumn, null, null, null)
            if (cursor != null) {
                cursor.moveToFirst()
                val columnIndex = cursor
                    .getColumnIndex(filePathColumn[0])
                val filePath = cursor.getString(columnIndex)
                cursor.close()
                videoFileTwo = File(filePath)
                Log.v(tagName, "videoFileOne: " + videoFileTwo!!.absolutePath)

            }
        } catch (e: Exception) {
            Log.e(tagName, "Exception: ${e.localizedMessage}")
        }
    }


}