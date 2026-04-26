package com.example.skyboxcricket

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.example.skyboxcricket.databinding.LoadingDialogBinding

class AppLoadingDialog(activity: Activity) {

    private val safeActivity = activity
    private val dialog: AlertDialog by lazy {
        val themedContext = ContextThemeWrapper(safeActivity, R.style.Theme_SkyBoxCricket)
        val binding = LoadingDialogBinding.inflate(LayoutInflater.from(themedContext))
        AlertDialog.Builder(themedContext)
            .setView(binding.root)
            .setCancelable(false)
            .create()
            .apply {
                setCanceledOnTouchOutside(false)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
    }

    fun show() {
        if (safeActivity.isFinishing || safeActivity.isDestroyed || dialog.isShowing) return
        dialog.show()
        dialog.window?.apply {
            setDimAmount(0f)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
}
