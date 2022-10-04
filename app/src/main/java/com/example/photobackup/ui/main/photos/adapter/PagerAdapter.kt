package com.example.photobackup.ui.main.photos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.GestureController.OnStateChangeListener
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.State
import com.alexvasilkov.gestures.views.GestureImageView
import com.example.photobackup.R
import com.example.photobackup.models.imageDownload.ImageData
import com.example.photobackup.util.DemoGlideHelper
import com.example.photobackup.util.GestureViewSettings

class PagerAdapter(
    private val context: Context,
    private val imagesData: List<ImageData>,
    private val token: String,
) : RecyclerView.Adapter<PagerAdapter.ViewHolder>() {

    private var recyclerView: RecyclerView? = null
    private val PROGRESS_DELAY = 200L
    private var activated: Boolean = false
    //todo fix the onclick function call
    private lateinit var listener: ImageClickListener
    fun setImageClickListener(clickListener:ImageClickListener) {
        listener = clickListener
    }

    fun setActivated(activated: Boolean) {
        if (this.activated != activated) {
            this.activated = activated
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        internal var image: GestureImageView
        internal var progress: View
        init {
            image = view.findViewById(R.id.photo_full_image)
            image.setOnClickListener(this)
            progress = view.findViewById(R.id.photo_full_progress)
        }
        override fun onClick(v: View?) {
            val position = bindingAdapterPosition
//            if (position != RecyclerView.NO_POSITION)
//                listener.onFullImageClick()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.demo_item_photo_full, parent, false)
        val holder = ViewHolder(view)//todo check if it works
        GestureViewSettings().applyDefault(holder.image)
        holder.image.positionAnimator.addPositionUpdateListener { position: Float, isLeaving: Boolean ->
            holder.progress.visibility = if (position == 1f) View.VISIBLE else View.INVISIBLE
        }

        val controller: GestureController = holder.image.controller
        controller.addOnStateChangeListener(DynamicZoom(controller.settings))
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        GestureViewSettings().applyDefault(holder.image)
        holder.progress.animate().setDuration(150L)
            .setStartDelay(PROGRESS_DELAY)
            .alpha(1f)
        // Loading image

        // Loading image
        DemoGlideHelper().loadFull(imagesData[position], holder.image, token, object : DemoGlideHelper.LoadingListener {
            override fun onSuccess() {
                holder.progress.animate().cancel()
                holder.progress.animate().alpha(0f)
            }

            override fun onError() {
                holder.progress.animate().alpha(0f)
            }
        })
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        DemoGlideHelper().clear(holder.image)
        holder.progress.animate().cancel()
        holder.progress.alpha = 0f
        holder.image.setImageDrawable(null)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    fun getImage(pos: Int): GestureImageView? {
        val holder = if (recyclerView == null) null else recyclerView!!.findViewHolderForLayoutPosition(pos)
        return if (holder == null) null else (holder as ViewHolder).image
    }

    fun getImageData(pos: Int): ImageData? {
        return if (pos < 0 || pos >= imagesData.size) null else imagesData[pos]
    }

    override fun getItemCount(): Int {
        return if (!activated) 0 else imagesData.size
    }

    interface ImageClickListener {
        fun onFullImageClick()
    }

    // Dynamically set double tap zoom level to fill the viewport
    private class DynamicZoom constructor(private val settings: Settings) :
        OnStateChangeListener {
        override fun onStateChanged(state: State) {
            updateZoomLevels()
        }

        override fun onStateReset(oldState: State, newState: State) {
            updateZoomLevels()
        }

        private fun updateZoomLevels() {
            val scaleX = settings.viewportW.toFloat() / settings.imageW
            val scaleY = settings.viewportH.toFloat() / settings.imageH
            settings.doubleTapZoom = Math.max(scaleX, scaleY)
        }
    }
}