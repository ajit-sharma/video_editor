package com.mobilehelp.videoeditor.interfaces

import android.net.Uri
import com.mobilehelp.videoeditor.fragments.OptiBaseCreatorDialogFragment
import java.io.File


interface OptiVideoHelper {
    fun setHelper(helper: OptiBaseCreatorDialogFragment.CallBacks)
    fun setFilePathFromSource(backgroundUri: Uri, overlayUri:Uri)
}
