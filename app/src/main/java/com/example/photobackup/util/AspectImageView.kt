package com.example.photobackup.util

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import android.view.View.MeasureSpec
import androidx.appcompat.widget.AppCompatImageView
import com.example.photobackup.R

class AspectImageView : AppCompatImageView {
    private var aspect = DEFAULT_ASPECT

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val arr = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.aspect))
        aspect = arr.getFloat(0, aspect)
        arr.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            height = calculate(width, aspect, VERTICAL)
        } else if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
            width = calculate(height, aspect, HORIZONTAL)
        } else if (width != 0) {
            height = calculate(width, aspect, VERTICAL)
        } else if (height != 0) {
            width = calculate(height, aspect, HORIZONTAL)
        } else {
            Log.e(AspectImageView::class.java.simpleName,
                "Either width or height should have exact value")
        }
        val specWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val specHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        super.onMeasure(specWidth, specHeight)
    }

    private fun calculate(size: Int, aspect: Float, direction: Int): Int {
        val wp = paddingLeft + paddingRight
        val hp = paddingTop + paddingBottom
        return if (direction == VERTICAL) Math.round((size - wp) / aspect) + hp else Math.round((size - hp) * aspect) + wp
    }

    companion object {
        const val DEFAULT_ASPECT = 16f / 9f
        private const val VERTICAL = 0
        private const val HORIZONTAL = 0
    }
}
