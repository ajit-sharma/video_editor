/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.mobilehelp.videoeditor

import android.content.Context
import android.util.Log
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import com.mobilehelp.videoeditor.interfaces.FFMpegCallback
import com.mobilehelp.videoeditor.utils.Constant
import com.mobilehelp.videoeditor.utils.OutputType
import java.io.File
import java.io.IOException

class VideoEditor private constructor(private val context: Context) {

    private var tagName: String = VideoEditor::class.java.simpleName
    private var videoFile: File? = null
    private var videoFileTwo: File? = null
    private var callback: FFMpegCallback? = null
    private var outputFilePath = ""
    private var type: Int? = null
    private var _xDelta: Int? = null
    private var _yDelta: Int? = null
    private var position: String? = null
    private var fixPosition: String? = null

    //for adding text
    private var font: File? = null
    private var text: String? = null
    private var color: String? = null
    private var size: String? = null
    private var border: String? = null
    private var BORDER_FILLED = ": box=1: boxcolor=black@0.5:boxborderw=5"
    private var BORDER_EMPTY = ""

    //for clip art
    private var imagePath: String? = null

    //for play back speed
    private var havingAudio = true
    private var ffmpegFS: String? = null

    //for merge audio video
    private var startTime = "00:00:00"
    private var endTime = "00:00:00"
    private var audioFile: File? = null

    //for filter
    private var filterCommand: String? = null

    companion object {
        lateinit var abc: VideoEditor
        fun with(context: Context): VideoEditor {
            abc = VideoEditor(context)
            return abc
        }

        fun getInstance(): VideoEditor {
            return abc
        }

        //for adding text
        var POSITION_BOTTOM_RIGHT = "x=w-tw-10:y=h-th-10"
        var POSITION_TOP_RIGHT = "x=w-tw-10:y=10"
        var POSITION_TOP_LEFT = "x=10:y=10"
        var POSITION_BOTTOM_LEFT = "x=10:h-th-10"
        var POSITION_CENTER_BOTTOM = "x=(main_w/2-text_w/2):y=main_h-(text_h*2)"
        var POSITION_CENTER_ALLIGN = "x=(w-text_w)/2: y=(h-text_h)/3"

//        130----and Y axis is -919

        //for adding clipart
        var BOTTOM_RIGHT = "overlay=W-w-130:H-h-(-919)"
        var TOP_RIGHT = "overlay=W-w-5:5"
        var TOP_LEFT = "overlay=130:-919"
        var BOTTOM_LEFT = "overlay=5:H-h-5"
        var CENTER_ALLIGN = "overlay=(W-w)/2:(H-h)/2"
    }

    fun setType(type: Int): VideoEditor {
        this.type = type
        return this
    }

    fun setFile(file: File): VideoEditor {
        this.videoFile = file
        return this
    }

    fun setFileTwo(file: File): VideoEditor {
        this.videoFileTwo = file
        return this
    }

    fun setVideoPosition(x: Int, y: Int): VideoEditor {
        _xDelta = x
        _yDelta = y
        fixPosition = "overlay=" + _xDelta + ":" + _yDelta
        return this
    }

    fun setAudioFile(file: File): VideoEditor {
        this.audioFile = file
        return this
    }

    fun setCallback(callback: FFMpegCallback): VideoEditor {
        this.callback = callback
        return this
    }

    fun setImagePath(imagePath: String): VideoEditor {
        this.imagePath = imagePath
        return this
    }

    fun setOutputPath(outputPath: String): VideoEditor {
        this.outputFilePath = outputPath
        return this
    }

    fun setFont(font: File): VideoEditor {
        this.font = font
        return this
    }

    fun setText(text: String): VideoEditor {
        this.text = text
        return this
    }

    fun setPosition(position: String): VideoEditor {
        this.position = position
        return this
    }

    fun setColor(color: String): VideoEditor {
        this.color = color
        return this
    }

    fun setSize(size: String): VideoEditor {
        this.size = size
        return this
    }

    fun addBorder(isBorder: Boolean): VideoEditor {
        if (isBorder)
            this.border = BORDER_FILLED
        else
            this.border = BORDER_EMPTY
        return this
    }

    fun setIsHavingAudio(havingAudio: Boolean): VideoEditor {
        this.havingAudio = havingAudio
        return this
    }

    fun setSpeedTempo(playbackSpeed: String, tempo: String): VideoEditor {
        this.ffmpegFS =
            if (havingAudio) "[0:v]setpts=$playbackSpeed*PTS[v];[0:a]atempo=$tempo[a]"
            else
                "setpts=$playbackSpeed*PTS"
        Log.v(tagName, "ffmpegFS: $ffmpegFS")
        return this
    }

    fun setStartTime(startTime: String): VideoEditor {
        this.startTime = startTime
        return this
    }

    fun setEndTime(endTime: String): VideoEditor {
        this.endTime = endTime
        return this
    }

    fun setFilter(filter: String): VideoEditor {
        this.filterCommand = filter
        return this
    }

    fun main() {
        if (type == Constant.AUDIO_TRIM) {
            if (audioFile == null || !audioFile!!.exists()) {
                callback!!.onFailure(IOException("File not exists"))
                return
            }
            if (!audioFile!!.canRead()) {
                callback!!.onFailure(IOException("Can't read the file. Missing permission?"))
                return
            }
        } else {
            if (videoFile == null || !videoFile!!.exists()) {
                callback!!.onFailure(IOException("File not exists"))
                return
            }
            if (!videoFile!!.canRead()) {
                callback!!.onFailure(IOException("Can't read the file. Missing permission?"))
                return
            }
        }


        val outputFile = File(outputFilePath)
        Log.v(tagName, "outputFilePath: $outputFilePath")
        var cmd: Array<String>? = null

        when (type) {
            Constant.VIDEO_FLIRT -> {
                //Video filter - Need video file, filter command & output file
                cmd = arrayOf("-y", "-i", videoFile!!.path, "-vf", filterCommand!!, outputFile.path)
            }

            Constant.VIDEO_TEXT_OVERLAY -> {
                //Text overlay on video - Need video file, font file, text, text color, text size, border if needed, position to apply & output file
                cmd = arrayOf(
                    "-y", "-i", videoFile!!.path, "-vf",
                    "drawtext=fontfile=" + font!!.path + ": text=" + text + ": fontcolor=" + color + ": fontsize=" + size + border + ": " + position,
                    "-c:v", "libx264", "-c:a", "copy", "-movflags", "+faststart", outputFile.path
                )
            }

            Constant.VIDEO_CLIP_ART_OVERLAY -> {
                //Clipart overlay on video - Need video file, image path, position to apply & output file
//                cmd = arrayOf(
//                    "-y",
//                    "-i",
//                    videoFile!!.path,
//                    "-i",
//                    imagePath!!,
//                    "-filter_complex",
//                    position!!,
//                    "-codec:a",
//                    "copy",
//                    outputFile.path
//                )

//                cmd = arrayOf(
//                    "-y",
//                    "-i",
//                    videoFile!!.path,
//                    "-filter_complex",
//                    "[0:v]colorkey=0x00ff00:0.4:0.2[ckout]",
//
//                    "-c:v",
//                    "libx264",
//                    "-preset",
//                    "ultrafast",
//                    outputFile.path
//                )

                cmd = arrayOf(
                    "-y",
                    "-i",
                    videoFile!!.path,
                    "-vf",
                    "chromakey=0x70de77:0.4:0.2",
                    "-c",
                    "copy",
                    "-c:v",
                    "libx264",
                    "-crf",
                    "23",
                    "-preset",
                    "ultrafast",
                    outputFile.path
                )
            }

            Constant.VIDEO_CLIP_VIDEO_OVERLAY -> {

                Log.e("overlay", "VIDEO_CLIP_VIDEO_OVERLAY" + fixPosition)
                cmd = arrayOf(
                    "-y",//
                    "-i",//input
                    videoFile!!.path,
                    "-i", // input
                    videoFileTwo!!.path,
                    "-filter_complex", //filter
//                    "[1:v]colorkey=0x00ff00:0.4:0.2[ckout];[0:v][ckout]overlay=(W-w)/2:(H-h)/2[out]", //colorkey = which color:0.4=similarity:0.2= blend; [0:v]= 1st video,
                    "[1:v]colorkey=0x00ff00:0.4:0.2[ckout];[0:v][ckout]" + fixPosition!! + "[out]",
                    "-map",//merge
                    "[out]",// output
                    "-vsync",// necessary to prevent same frame to process
                    "0",// best vsync option
                    "-map", // merge
                    "0:a",// audio from 1st video
                    "-c:v", // -c = codec v = video
                    "libx264", // video codac name
//                    "mpeg4", // video codac name
                    "-crf", // control bitrat
                    "23", // optimal
                    "-preset", // option
                    "ultrafast",// preset param
//                    "-r", // bitrat
//                    "30", // 30 FSP
                    outputFile.path // output path
                )


//                "[1:v]colorkey=0x00ff00:0.1:0.2[ckout];[0:v][ckout]"
//                cmd = arrayOf(
//                    "-y",
//                    "-i",
//                    videoFile!!.path,
//                    "-i",
//                    videoFileTwo!!.path,
//                    "-filter_complex",
//                    "[1:v]colorkey=0x00ff00:0.4:0.2[ckout];[0:v][ckout]"+fixPosition!!+"[out]",
////                    fixPosition!! + "[out]",
//                    "-map",
//                    "[out]",
//                    "-map", "1:a", "-c:a", "copy",
//                    "-vsync",
//                    "0",
//                    "-ab",
//                    "48000",
//                    "-c:v",
//                    "libx264",
//                    "-crf",
//                    "23",
//                    "-preset",
//                    "veryfast",
//                    outputFile.path
//                )

            }

            Constant.MERGE_VIDEO -> {
                //Merge videos - Need two video file, approx video size & output file
//                cmd = arrayOf(
//                    "-y",
//                    "-i",
//                    videoFileTwo!!.path,
//                    "-filter_complex",
//                    "[1:v]colorkey=0x00ff00:0.4:0.2[ckout];[0:v][ckout]",
//                    "-map",
//                    "[v]",
//                    "-map",
//                    "[a]",
//                    outputFile.path
//                )

//                cmd = arrayOf(
//                    "-y",
//                    "-i",
//                    videoFileTwo!!.path,
//                    "-vf",
//                    "chromakey=0x70de77:0.1:0.2",
//                                       outputFile.path
//                )


//                ffmpeg -i input.mp4 -vf "" -c copy -c:v png output.mov

            }

            Constant.VIDEO_PLAYBACK_SPEED -> {
                //Video playback speed - Need video file, speed & tempo value according to playback and output file
                cmd = if (havingAudio) {
                    arrayOf(
                        "-y",
                        "-i",
                        videoFile!!.path,
                        "-filter_complex",
                        "[1:v]colorkey=0x00ff00:0.4:0.2[ckout];[0:v][ckout]" + fixPosition!! + "[out]",
                        "-map",
                        "[v]",
                        "-map",
                        "[a]",
                        outputFile.path
                    )
                } else {
                    arrayOf("-y", "-i", videoFile!!.path, "-filter:v", ffmpegFS!!, outputFile.path)
                }
            }

            Constant.AUDIO_TRIM -> {
                //Audio trim - Need audio file, start time, end time & output file
                cmd = arrayOf(
                    "-y",
                    "-i",
                    audioFile!!.path,
                    "-ss",
                    startTime,
                    "-to",
                    endTime,
                    "-c",
                    "copy",
                    outputFile.path
                )
            }

            Constant.VIDEO_AUDIO_MERGE -> {
                //Video audio merge - Need audio file, video file & output file
                cmd = arrayOf(
                    "-y",
                    "-i",
                    videoFile!!.path,
                    "-i",
                    audioFile!!.path,
                    "-c",
                    "copy",
                    "-map",
                    "0:v:0",
                    "-map",
                    "1:a:0",
                    outputFile.path
                )
            }

            Constant.VIDEO_TRIM -> {
                //Video trim - Need video file, start time, end time & output file
//                ffmpeg -i 1.mp4 -f gdigrab -framerate 25 -video_size 300x200 -i title="MyWindow"
//
//                "[1]split[m][a];
//                [a]format=yuv444p,geq='if(gt(lum(X,Y),0),255,0)',hue=s=0[al];
//                [m][al]alphamerge[ovr];
//                [0][ovr]overlay=(main_w-overlay_w):main_h-overlay_h[v]"
//                -map "[v]" -c:v libx264 -r 25 out.mp4

                cmd = arrayOf(
                    "-y",
                    "-i",
                    videoFile!!.path,
                    "-filter_complex",
                    "chromakey=green",
                    "-c",
                    "copy",
                    outputFile.path
                )
            }

            Constant.VIDEO_TRANSITION -> {
                //Video transition - Need video file, transition command & output file
                cmd = arrayOf(
                    "-y",
                    "-i",
                    videoFile!!.absolutePath,
                    "-acodec",
                    "copy",
                    "-vf",
                    "fade=t=in:st=0:d=5",
                    outputFile.path
                )
            }

            Constant.CONVERT_AVI_TO_MP4 -> {
                //Convert .avi to .mp4 - Need avi video file, command, mp4 output file
                cmd = arrayOf(
                    "-y",
                    "-i",
                    videoFile!!.path,
                    "-c:v",
                    "libx264",
                    "-crf",
                    "19",
                    "-preset",
                    "slow",
                    "-c:a",
                    "aac",
                    "-b:a",
                    "192k",
                    "-ac",
                    "2",
                    outputFile.path
                )
            }
            Constant.VIDEO_REMOVE_CHROMA -> {
                //remove chroma key only -i input.mp4 -vf "chromakey=0x00ff00:0.1:0.2" -c copy -c:v png output.mov
                cmd = arrayOf(
                    "-y",
                    "-i",
                    videoFile!!.path,
                    "-vf",
                    "chromakey=0x00ff00:0.1:0.2",
                    "-c",
                    "copy",
                    "-c:v",
                    "-preset",
                    "ultrafast",
                    outputFile.path
                )
            }
        }

        try {
            FFmpeg.getInstance(context).execute(cmd, object : ExecuteBinaryResponseHandler() {
                override fun onStart() {

                }

                override fun onProgress(message: String?) {
                    callback!!.onProgress(message!!)
                }

                override fun onSuccess(message: String?) {
                    callback!!.onSuccess(outputFile, OutputType.TYPE_VIDEO)
                }

                override fun onFailure(message: String?) {
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                    callback!!.onFailure(IOException(message))
                }

                override fun onFinish() {
                    callback!!.onFinish()
                }
            })
        } catch (e: Exception) {
            callback!!.onFailure(e)
        } catch (e2: FFmpegCommandAlreadyRunningException) {
            callback!!.onNotAvailable(e2)
        }
    }
}