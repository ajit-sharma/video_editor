/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.mobilehelp.videoeditor.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mobilehelp.videoeditor.OptiVideoEditor
import com.mobilehelp.videoeditor.R
import com.mobilehelp.videoeditor.adapter.OptiTransitionAdapter
import com.mobilehelp.videoeditor.interfaces.OptiFFMpegCallback
import com.mobilehelp.videoeditor.interfaces.OptiFilterListener
import com.mobilehelp.videoeditor.utils.OptiConstant
import com.mobilehelp.videoeditor.utils.OptiUtils
import java.io.File
import java.util.*

class OptiTransitionFragment : BottomSheetDialogFragment(), OptiFilterListener, OptiFFMpegCallback {

    private var tagName: String = OptiTransitionFragment::class.java.simpleName
    private lateinit var rootView: View
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var rvTransition: RecyclerView
    private lateinit var ivClose: ImageView
    private lateinit var ivDone: ImageView
    private var videoFile: File? = null
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null
    private var transitionList: ArrayList<String> = ArrayList()
    private lateinit var optiTransitionAdapter: OptiTransitionAdapter
    private var selectedTransition: String? = null
    private var mContext: Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.opti_fragment_transition, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTransition = rootView.findViewById(R.id.rvTransition)
        ivClose = rootView.findViewById(R.id.iv_close)
        ivDone = rootView.findViewById(R.id.iv_done)
        linearLayoutManager = LinearLayoutManager(requireActivity().applicationContext)

        mContext = context

        ivClose.setOnClickListener {
            dismiss()
        }

        ivDone.setOnClickListener {
            optiTransitionAdapter.setTransition()

            if (selectedTransition != null) {
                dismiss()

                when (selectedTransition) {
                    "Fade in/out" -> {
                        applyTransitionAction()
                    }
                }
            }
        }

        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvTransition.layoutManager = linearLayoutManager

        transitionList.add("Fade in/out")

        optiTransitionAdapter = OptiTransitionAdapter(transitionList, requireActivity().applicationContext, this)
        rvTransition.adapter = optiTransitionAdapter
        optiTransitionAdapter.notifyDataSetChanged()
    }

    private fun applyTransitionAction() {
        //output file is generated and send to video processing
        val outputFile = OptiUtils.createVideoFile(requireContext())
        Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

        OptiVideoEditor.with(requireContext())
            .setType(OptiConstant.VIDEO_TRANSITION)
            .setFile(videoFile!!)
             //.setFilter(command)
            .setOutputPath(outputFile.path)
            .setCallback(this)
            .main()

        helper?.showLoading(true)
    }

    override fun selectedFilter(filter: String) { //here transition
        selectedTransition = filter
    }

    fun setHelper(helper: OptiBaseCreatorDialogFragment.CallBacks) {
        this.helper = helper
    }

    fun setFilePathFromSource(file: File) {
        videoFile = file
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
    }
}