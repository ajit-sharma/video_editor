/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.mobilehelp.videoeditor.utils

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object OptiUtils {

    val outputPath: String
        get() {
            val path = Environment.getExternalStorageDirectory()
                .toString() + File.separator + Constant.APP_NAME + File.separator

            val folder = File(path)
            if (!folder.exists())
                folder.mkdirs()

            return path
        }

    fun copyFileToInternalStorage(resourceId: Int, resourceName: String, context: Context): File {
        val path = Environment.getExternalStorageDirectory()
            .toString() + File.separator + Constant.APP_NAME + File.separator + Constant.CLIP_ARTS + File.separator
        val folder = File(path)
        if (!folder.exists())
            folder.mkdirs()

        val dataPath = "$path$resourceName.png"
        Log.v("OptiUtils", "path: $dataPath")
        try {
            val inputStream = context.resources.openRawResource(resourceId)
            inputStream.toFile(dataPath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return File(dataPath)
    }

    fun copyFontToInternalStorage(resourceId: Int, resourceName: String, context: Context): File {
        val path = Environment.getExternalStorageDirectory()
            .toString() + File.separator + Constant.APP_NAME + File.separator + Constant.FONT + File.separator
        val folder = File(path)
        if (!folder.exists())
            folder.mkdirs()

        val dataPath = "$path$resourceName.ttf"
        Log.v("OptiUtils", "path: $dataPath")
        try {
            val inputStream = context.resources.openRawResource(resourceId)
            inputStream.toFile(dataPath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return File(dataPath)
    }

    private fun InputStream.toFile(path: String) {
        File(path).outputStream().use { this.copyTo(it) }
    }

    fun getConvertedFile(folder: String, fileName: String): File {
        val f = File(folder)

        if (!f.exists())
            f.mkdirs()

        return File(f.path + File.separator + fileName)
    }

    fun refreshGallery(path: String, context: Context) {

        val file = File(path)
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(file)
            mediaScanIntent.data = contentUri
            context.sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshGalleryAlone(context: Context) {
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            context.sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isVideoHaveAudioTrack(path: String): Boolean {
        var audioTrack = false

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
        audioTrack = hasAudioStr == "yes"

        return audioTrack
    }


    fun createVideoFile(context: Context): File {
        val timeStamp: String =
            SimpleDateFormat(Constant.DATE_FORMAT, Locale.getDefault()).format(Date())
        val imageFileName: String = Constant.APP_NAME + timeStamp + "_"
        val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
        if (!storageDir.exists()) storageDir.mkdirs()
        return File.createTempFile(imageFileName, Constant.VIDEO_FORMAT, storageDir)
    }

    fun createAudioFile(context: Context): File {
        val timeStamp: String =
            SimpleDateFormat(Constant.DATE_FORMAT, Locale.getDefault()).format(Date())
        val imageFileName: String = Constant.APP_NAME + timeStamp + "_"
        val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
        if (!storageDir.exists()) storageDir.mkdirs()
        return File.createTempFile(imageFileName, Constant.AUDIO_FORMAT, storageDir)
    }

    fun getVideoDuration(context: Context, file: File): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMillis = time.toLong()
        retriever.release()
        return timeInMillis
    }

    fun getVideoDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMillis = time.toLong()
        retriever.release()
        return timeInMillis
    }


    @Throws(IOException::class)
    fun copyFile(src: File, dst: File) {
        val var2 = FileInputStream(src)
        val var3 = FileOutputStream(dst)
        val var4 = ByteArray(1024)
        var var5: Int
        while (var2.read(var4).also { var5 = it } > 0) {
            var3.write(var4, 0, var5)
        }
        var2.close()
        var3.close()
    }

}


