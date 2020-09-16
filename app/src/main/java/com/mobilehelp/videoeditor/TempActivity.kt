package com.mobilehelp.videoeditor

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mobilehelp.videoeditor.alphaVideo.AlphaMovieView
import com.otaliastudios.cameraview.CameraLogger
import com.otaliastudios.cameraview.CameraView
import java.io.*
import java.util.*

/*
* Copyright 2017 Pavel Semak
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


class TempActivity : Fragment(), OnTouchListener {
    var uriFromCamera: Uri? = null
    var uriFromGallery: Uri? = null
    var _root: RelativeLayout? = null
    private var _xDelta = 0f
    private var _yDelta = 0f
    private var display: Display? = null
    private var size: Point? = null
    private var backgroundVideoPath: File? = null
    private var overlayVideoPath: File? = null
    private var backgroundVideoPlayer: VideoView? = null
    var editVideoPlayer: AlphaMovieView? = null
    private val imageViewBackground: ImageView? = null
    private var cameraView: CameraView? = null

    //    private val bgIndex: Int = com.alphamovie.example.MainActivity.Companion.FIRST_BG_INDEX
    private val GALLERY = 1
    private val CAMERA = 2
    var mediaController: MediaController? = null
    private var outputPath: String? = null
    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.tttt, container, false)
        initView(rootView)
        return rootView
    }

    private fun initView(rootView: View?) {


        requireActivity().window.setFormat(PixelFormat.TRANSLUCENT)

        _root = rootView?.findViewById(R.id.root)

        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE)
        cameraView = rootView?.findViewById(R.id.camera)

        cameraView!!.setLifecycleOwner(this)
        val videoMaxDuration = 120 * 1000
        cameraView!!.videoMaxDuration = videoMaxDuration


        editVideoPlayer = rootView?.findViewById(R.id.edit_video_player)
//        backgroundVideoPlayer = rootView?.findViewById(R.id.background_video_player)


        _root!!.setOnTouchListener(this)
        size = Point()
        display = requireActivity().windowManager.defaultDisplay
        display!!.getSize(size)


//        imageViewBackground = (ImageView) findViewById(R.id.image_background);


        editVideoPlayer!!.setVideoFromAssets(FILENAME)
        editVideoPlayer!!.start()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


//        mediaController = new MediaController(this);
//        mediaController.setAnchorView(alphaMovieView);


//        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(150, 50);
//        layoutParams.leftMargin = 50;
//        layoutParams.topMargin = 50;
//        layoutParams.bottomMargin = -250;
//        layoutParams.rightMargin = -250;
//        alphaMovieView1.setLayoutParams(layoutParams);
//
//        alphaMovieView1.setOnTouchListener(this);
//        _root.addView(alphaMovieView1);
    }

    public override fun onResume() {
        super.onResume()
        //        backgroundVideoPlayer.onResume();
        editVideoPlayer!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        //        backgroundVideoPlayer.onPause();
        editVideoPlayer!!.onPause()
    }

    fun play(view: View?) {
        if (uriFromCamera != null && uriFromGallery != null) {

//            String tttt = mux(backgroundVideoPath.toString(), overlayVideoPath.toString());
//
////            String pppp = mux(overlayVideoPath.toString(), backgroundVideoPath.toString());
//
//            File file = new File(tttt);
////            File file1 = new File(pppp);
//            Uri uri = Uri.fromFile(file);
//            Uri uri1 = Uri.fromFile(file1);


//
            backgroundVideoPlayer!!.setVideoURI(uriFromCamera)
            editVideoPlayer!!.setVideoFromUri(requireContext(), uriFromGallery)
            backgroundVideoPlayer!!.start()
            editVideoPlayer!!.start()
        } else Toast.makeText(requireContext(), "please select video", Toast.LENGTH_LONG).show()
    }

    fun pause(view: View?) {
        backgroundVideoPlayer!!.pause()
        editVideoPlayer!!.pause()
    }

    fun stop(view: View?) {
//        backgroundVideoPlayer.stop();
        editVideoPlayer!!.stop()
    }

    fun captureBackgroundVideo(view: View?) {
        if (checkPermission()) {
            //main logic or main code
            takeVideoFromCamera()
            // . write your main code to execute, It will execute if the permission is already given.
        } else {
            checkPermission()
        }

//        bgIndex = ++bgIndex % BG_ARRAY_LENGTH;
//        imageViewBackground.setImageResource(backgroundResources[bgIndex]);
    }

    fun captureEditingVideo(view: View?) {
        if (checkPermission()) {
            //main logic or main code
            chooseVideoFromGallary()
            // . write your main code to execute, It will execute if the permission is already given.
        } else {
            checkPermission()
        }
    }

    //    public void playVideo(View view) {
    //
    //
    ////        backgroundVideoPlayer.setMediaController(mediaController);
    ////        backgroundVideoPlayer.setVideoURI(uriFromCamera);
    //
    //
    //    }

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(requireContext())
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf(
            "Select video from gallery",
            "Record video from camera"
        )
        pictureDialog.setItems(
            pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> chooseVideoFromGallary()
                1 -> takeVideoFromCamera()
            }
        }
        pictureDialog.show()
    }

    fun chooseVideoFromGallary() {
        val galleryIntent = Intent(
            Intent.ACTION_OPEN_DOCUMENT,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        galleryIntent.type = "video/mp4"
        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun takeVideoFromCamera() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(intent, CAMERA)
    }

//    public override fun onActivityResult(
//        requestCode: Int,
//        resultCode: Int,
//        data: Intent?
//    ) {
//        Log.d("result", "" + resultCode)
//        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == this.RESULT_CANCELED) {
//            Log.d("what", "cancle")
//            return
//        }
//        if (requestCode == GALLERY) {
//            Log.d("what", "gale")
//            if (data != null) {
//                val contentURI = data.data
//                uriFromGallery = contentURI
//                overlayVideoPath = Utils.copyFileToExternalStorages(
//                    uriFromGallery,
//                    "overlayvideo.mp4",
//                    baseContext
//                )
//
//
////                String selectedVideoPath = getPath(contentURI);
//////                Log.d("path", selectedVideoPath);
////                saveVideoToInternalStorage(selectedVideoPath);
////                videoView.setVideoURI(contentURI);
////                videoView.requestFocus();
////                videoView.start();
//            }
//        } else if (requestCode == CAMERA) {
//            val contentURI = data!!.data
//            val recordedVideoPath = getPath(contentURI)
//            Log.d("frrr", recordedVideoPath)
//            saveVideoToInternalStorage(recordedVideoPath)
//            uriFromCamera = contentURI
//            backgroundVideoPath =
//                Utils.copyFileToExternalStorages(uriFromCamera, "Background.mp4", baseContext)
//
//
//            //            videoView.setVideoURI(contentURI);
////            videoView.requestFocus();
////            videoView.start();
//        }
//    }

    private fun saveVideoToInternalStorage(filePath: String?) {
        val newfile: File
        try {
            val currentFile = File(filePath)
            val wallpaperDirectory = File(
                Environment.getExternalStorageDirectory()
                    .toString() + VIDEO_DIRECTORY
            )
            newfile = File(
                wallpaperDirectory,
                Calendar.getInstance().timeInMillis.toString() + ".mp4"
            )
            if (!wallpaperDirectory.exists()) {
                wallpaperDirectory.mkdirs()
            }
            if (currentFile.exists()) {
                val `in`: InputStream = FileInputStream(currentFile)
                val out: OutputStream = FileOutputStream(newfile)

                // Copy the bits from instream to outstream
                val buf = ByteArray(1024)
                var len: Int
                while (`in`.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
                `in`.close()
                out.close()
                Log.v("vii", "Video file saved successfully.")
            } else {
                Log.v("vii", "Video saving failed. Source file missing.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPath(uri: Uri?): String? {
        val projection =
            arrayOf(MediaStore.Video.Media.DATA)
        val cursor =
            requireActivity().contentResolver.query(uri!!, projection, null, null, null)
        return if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            val column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(column_index)
        } else null
    }

    private fun checkPermission(): Boolean {
        val listPermissionNeeded: MutableList<String> =
            ArrayList()
        for (perm in appPermission) {
            if (ContextCompat.checkSelfPermission(requireActivity(), perm)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted
                listPermissionNeeded.add(perm)
            }
        }

        // Ask for non-granted permissions
        if (!listPermissionNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(), listPermissionNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val permissionResults =
                HashMap<String, Int>()
            var deniedCount = 0

            //Gather permission grant results
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults[permissions[i]] = grantResults[i]
                    deniedCount++
                }
            }

            //Check if all permissions granted
            if (deniedCount == 0) {

                //Process ahead with the app
                showPictureDialog()
            } else {
                for ((permName, permResult) in permissionResults) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            permName
                        )
                    ) {
                        showMessageOKCancel(
                            "",
                            "This App Need Camera & Storage Permissions",
                            "Yes, Grant Permission",
                            DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                                checkPermission()
                            }, "No, Exit app",
                            DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                                requireActivity().finish()
                            }, false
                        )
                    } else {
                        showMessageOKCancel(
                            "",
                            "You have denied some Permission",
                            "Go to setting ",
                            DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", requireActivity().packageName, null)
                                )
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                requireActivity().finish()
                            }, "No, Exit app",
                            DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                                requireActivity().finish()
                            }, false
                        )
                    }
                }
            }
        }
    }

    private fun showMessageOKCancel(
        title: String, msg: String, positiveLabel: String,
        positiveClick: DialogInterface.OnClickListener,
        negativeLabel: String, negativeClick: DialogInterface.OnClickListener,
        isCancelable: Boolean
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(msg)
            .setCancelable(isCancelable)
            .setPositiveButton(positiveLabel, positiveClick)
            .setNegativeButton(negativeLabel, negativeClick)
            .create()
            .show()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return if (v.id == v.id) {
            when (event.action) {
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
                    Log.e(
                        "ACTION_MOVE" + event.rawX + _xDelta,
                        "=====" + event.rawY + _yDelta
                    )
                }
                MotionEvent.ACTION_UP -> {
                    v.animate()
                        .x(event.rawX + _xDelta)
                        .y(event.rawY + _yDelta)
                        .setDuration(0)
                        .start()
                    Log.e(
                        "ACTION_UP" + event.rawX + _xDelta,
                        "=====" + event.rawY + _yDelta
                    )
                    return false
                }
                else -> return false
            }
            true
        } else {
            false
        }
    }

    companion object {
        const val FILENAME = "overlayvideo.mp4"
        val appPermission = arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        const val PERMISSION_REQUEST_CODE = 1240
        const val FIRST_BG_INDEX = 0
        const val BG_ARRAY_LENGTH = 3

        //        val backgroundResources = intArrayOf(
//            R.drawable.court_blue,
//            R.drawable.court_green, R.drawable.court_red
//        )
        private const val VIDEO_DIRECTORY = "/demonuts"
    }
}