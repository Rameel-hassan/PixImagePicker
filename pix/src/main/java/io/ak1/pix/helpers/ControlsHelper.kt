package io.ak1.pix.helpers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.camera.core.ImageCapture
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentActivity
import io.ak1.pix.R
import io.ak1.pix.databinding.FragmentPixBinding
import io.ak1.pix.models.Flash
import io.ak1.pix.models.Mode
import io.ak1.pix.models.Options
import io.ak1.pix.models.PixViewModel
import io.ak1.pix.utility.TAG

/**
 * Created By Akshay Sharma on 17,June,2021
 * https://ak1.io
 */

fun FragmentPixBinding.setDrawableIconForFlash(options: Options) {
     flashImage.setImageResource(
        when (options.flash) {
            Flash.Off -> R.drawable.ic_flash_off_black_24dp
            Flash.On -> R.drawable.ic_flash_on_black_24dp
            else -> R.drawable.ic_flash_auto_black_24dp
        }
    )
}

fun ViewGroup.setOnClickForFLash(options: Options, callback: (Options) -> Unit) {
    val iv = getChildAt(0) as ImageView
    setOnClickListener {
        val height = height
        iv.animate()
            .translationY(height.toFloat())
            .setDuration(100)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    iv.translationY = -(height / 2).toFloat()
                    when (options.flash) {
                        Flash.Auto -> {
                            options.flash = Flash.Off
                        }
                        Flash.Off -> {
                            options.flash = Flash.On
                        }
                        else -> {
                            options.flash = Flash.Auto
                        }
                    }
                    callback(options)
                    iv.animate().translationY(0f).setDuration(50).setStartDelay(100)
                        .setListener(null).start()
                }
            })
            .start()
    }
}

@SuppressLint("ClickableViewAccessibility,RestrictedApi")
internal fun FragmentPixBinding.setupClickControls(
    model: PixViewModel,
    cameraXManager: CameraXManager?,
    options: Options,
    callback: (Int, Uri) -> Unit
) {
     messageBottom.setText(
        when (options.mode) {
            Mode.Picture -> R.string.pix_bottom_message_without_video
            else -> R.string.pix_bottom_message_without_video
        }
    )
     primaryClickButton.apply {


        setOnClickListener {
            if (options.count <= model.selectionListSize) {
                 sendButton.context.toast(model.selectionListSize)
                return@setOnClickListener
            }
            cameraXManager?.takePhoto { uri, exc ->
                if (exc == null) {
                    val newUri = Uri.parse(uri.toString())
                    callback(3, newUri)
                } else {
                    Log.e(TAG, "$exc")
                }
            }
            isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                isEnabled = true
            }, 1000L)

        }
        var isRecording = false
        setOnLongClickListener {
            if (options.mode == Mode.Picture) {
                return@setOnLongClickListener false
            }

            if (options.count <= model.selectionListSize) {
                 sendButton.context.toast(model.selectionListSize)
                return@setOnLongClickListener false
            }
            callback(4, Uri.EMPTY)


            true
        }
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                 primaryClickBackground.hide()
                 primaryClickBackground.animate().scaleX(1f).scaleY(1f)
                    .setDuration(300).setInterpolator(
                        AccelerateDecelerateInterpolator()
                    ).start()
                 primaryClickButton.animate().scaleX(1f)
                    .scaleY(1f).setDuration(300).setInterpolator(
                        AccelerateDecelerateInterpolator()
                    ).start()
                root.requestDisallowInterceptTouchEvent(false)
            } else if (event.action == MotionEvent.ACTION_DOWN) {
                 primaryClickBackground.show()
                 primaryClickBackground.animate().scaleX(1.2f).scaleY(1.2f)
                    .setDuration(300).setInterpolator(AccelerateDecelerateInterpolator()).start()
                 primaryClickButton.animate().scaleX(1.2f)
                    .scaleY(1.2f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
                root.requestDisallowInterceptTouchEvent(true)
            }
            false
        }
         selectionOk.setOnClickListener { callback(0, Uri.EMPTY) }
         sendButton.setOnClickListener { callback(0, Uri.EMPTY) }
         selectionBack.setOnClickListener { callback(1, Uri.EMPTY) }
         selectionCheck.setOnClickListener {
             selectionCheck.hide()
            callback(2, Uri.EMPTY)
        }
    }
     flashButton.setOnClickForFLash(options) {
        setDrawableIconForFlash(it)
        cameraXManager?.imageCapture?.flashMode = when (options.flash) {
            Flash.Auto -> ImageCapture.FLASH_MODE_AUTO
            Flash.Off -> ImageCapture.FLASH_MODE_OFF
            Flash.On -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_AUTO
        }
    }
     lensFacing.setOnClickListener {
        val oa1 = ObjectAnimator.ofFloat(
             lensFacing,
            "scaleX",
            1f,
            0f
        ).setDuration(150)
        val oa2 = ObjectAnimator.ofFloat(
             lensFacing,
            "scaleX",
            0f,
            1f
        ).setDuration(150)
        oa1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                lensFacing.setImageResource(R.drawable.ic_photo_camera)
                oa2.start()
            }
        })
        oa1.start()
        options.isFrontFacing = !options.isFrontFacing
        cameraXManager?.bindCameraUseCases(this)
    }
}

fun FragmentPixBinding.longSelectionStatus(
    enabled: Boolean
) {
    val colorPrimaryDark = root.context.color(R.color.primary_color_pix)
    val colorSurface = root.context.color(R.color.surface_color_pix)

    if (enabled) {
        selectionCheck.hide()
        selectionCount.setTextColor(colorSurface)
        topbar.setBackgroundColor(colorPrimaryDark)
        DrawableCompat.setTint(selectionBack.drawable, colorSurface)
        DrawableCompat.setTint(selectionCheck.drawable, colorSurface)
    } else {
        selectionCheck.show()
        DrawableCompat.setTint(selectionBack.drawable, colorPrimaryDark)
        DrawableCompat.setTint(selectionCheck.drawable, colorPrimaryDark)
        topbar.setBackgroundColor(colorSurface)
    }
}

fun FragmentPixBinding.setSelectionText(fragmentActivity: FragmentActivity, size: Int = 0) {
    selectionCount.text = if (size == 0) {
        selectionOk.hide()
        fragmentActivity.resources.getString(R.string.pix_tap_to_select)
    } else {
        selectionOk.show()
        "$size ${fragmentActivity.resources.getString(R.string.pix_selected)}"
    }
    imgCount.text = size.toString()
}
