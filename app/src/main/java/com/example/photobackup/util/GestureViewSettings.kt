package com.example.photobackup.util

import android.view.Gravity
import android.view.View
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.views.interfaces.GestureView

class GestureViewSettings{

    private val OVERSCROLL_VAL = 0f
    private val OVERZOOM_VAL = 1f
    private val FIT_METHOD = Settings.Fit.INSIDE
    private val BOUNDS_TYPE = Settings.Bounds.NORMAL
    private val GRAVITY = Gravity.CENTER
    private val ANIMATION_DURATION = Settings.ANIMATIONS_DURATION

    fun applyDefault(view: GestureView) {
        val context = (view as View).context

        view.controller.settings
            .setPanEnabled(true)
            .setZoomEnabled(true)
            .setDoubleTapEnabled(true)
            .setRotationEnabled(false)
            .setRestrictRotation(false)
            .setOverscrollDistance(context, OVERSCROLL_VAL, OVERSCROLL_VAL)
            .setOverzoomFactor(OVERZOOM_VAL)
            .setFillViewport(true)
            .setFitMethod(FIT_METHOD)
            .setBoundsType(BOUNDS_TYPE)
            .setGravity(GRAVITY)
            .setAnimationsDuration(ANIMATION_DURATION)
    }

}