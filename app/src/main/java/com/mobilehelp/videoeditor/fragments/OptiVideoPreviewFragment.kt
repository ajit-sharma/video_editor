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
import android.widget.VideoView
import com.mobilehelp.videoeditor.R
import com.mobilehelp.videoeditor.alphaVideo.AlphaMovieView
import com.otaliastudios.cameraview.VideoResult
import java.io.File
import java.lang.ref.WeakReference


class OptiVideoPreviewFragment : OptiBaseCreatorDialogFragment() {


    private var videoView: VideoView? = null
    private var alphaMovieView: AlphaMovieView? = null
    private var imgCancel: ImageView? = null
    private var imgSave: ImageView? = null
    private var imgShare: ImageView? = null
    private var fabPlay: ImageView? = null
    private var overlayUri: Uri? = null
    private var videoFileTwo: File? = null
    private var filePath: String? = null
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null
    private var _xDelta = 0f
    private var _yDelta = 0f


    private var videoResult: WeakReference<VideoResult>? = null


    fun setVideoResult(
        result: VideoResult?,
        uri: Uri?,
        videoFileTwo: File?,
        _xDeltaTemp: Float,
        _yDeltaTemp: Float
    ) {
        videoResult = result?.let { WeakReference(it) }
        overlayUri = uri
        _xDelta = _xDeltaTemp
        _yDelta = _yDeltaTemp
        filePath = overlayUri!!.path
        this.videoFileTwo = videoFileTwo
        val path = this.videoFileTwo!!.absolutePath
    }

    companion object {
        fun newInstance() = OptiVideoPreviewFragment()
    }


//    OptiAddOverlayVideoFragment.newInstance().apply {
//        setHelper(this@OptiMasterProcessorFragment)
//    }.show(requireFragmentManager(), "OptiMergeFragment")


    private var tagName: String = OptiVideoPreviewFragment::class.java.simpleName
    private lateinit var rootView: View
    override fun permissionsBlocked() {
        TODO("Not yet implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    private fun initView(rootView: View?) {


        videoView = rootView?.findViewById(R.id.video)
        alphaMovieView = rootView?.findViewById(R.id.alphaMovie)
        fabPlay = rootView?.findViewById(R.id.play_video)
        imgCancel = rootView?.findViewById(R.id.cancel_video)
        imgSave = rootView?.findViewById(R.id.save_video)
        imgShare = rootView?.findViewById(R.id.share_video)


        fabPlay!!.setOnClickListener{

            playVideo()
        }

        fabPlay!!.setOnClickListener{

            playVideo()
        }

        fabPlay!!.setOnClickListener{

            playVideo()
        }




        val result: VideoResult? =
            if (videoResult == null) null else videoResult!!.get()



        if (result == null) {
            dialog!!.dismiss()
            return
        }



        val controller = MediaController(activity)
        controller.setAnchorView(videoView)
        controller.setMediaPlayer(videoView)
        videoView!!.setMediaController(controller)
        videoView!!.setVideoURI(Uri.fromFile(result.file))
        alphaMovieView!!.setVideoFromUri(requireContext(), overlayUri)

        alphaMovieView!!.animate()
            .x(_xDelta)
            .y(_yDelta)
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

            fabPlay!!.visibility = View.VISIBLE
//            dialog!!.dismiss()

        }


    }


    fun playVideo() {
        if (videoView!!.isPlaying) return
        fabPlay!!.visibility = View.GONE
        videoView!!.start()
        alphaMovieView!!.start()
    }


}