package com.example.photobackup.ui.main.photos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.GestureController.OnStateChangeListener
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.State
import com.alexvasilkov.gestures.views.GestureFrameLayout
import com.alexvasilkov.gestures.views.GestureImageView
import com.example.photobackup.R
import com.example.photobackup.models.imageDownload.MediaData
import com.example.photobackup.other.Constants
import com.example.photobackup.util.DemoGlideHelper
import com.example.photobackup.util.GestureViewSettings
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource


class PagerAdapter(
    private val context: Context,
    private var mediaData: MutableList<MediaData>,
    private val token: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var recyclerView: RecyclerView? = null
    private val PROGRESS_DELAY = 200L
    private var activated: Boolean = false
    private lateinit var listener: ImageClickListener
    fun setImageClickListener(clickListener: ImageClickListener) {
        listener = clickListener
    }

    fun setActivated(activated: Boolean) {
        if (this.activated != activated) {
            this.activated = activated
            notifyDataSetChanged()
        }
    }

    val exoPlayerPositionMap = mutableMapOf<Int, ExoPlayer>()
    interface OnVideoPlayerReady {
        fun addVideoPlayer(position: Int, player: ExoPlayer)
    }


    companion object {
        const val IMAGE_VIEW = 1
        const val VIDEO_VIEW = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (mediaData[position].mediaType) {
            Constants.VIDEO_TYPE -> VIDEO_VIEW
            Constants.IMAGE_TYPE -> IMAGE_VIEW
            else -> -1
        }
    }

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        internal var image: GestureImageView
        internal var thumbImage: GestureImageView
        internal var progress: View

        init {
            image = view.findViewById(R.id.photo_full_image)
            image.setOnClickListener(this)
            thumbImage = view.findViewById(R.id.photo_thumb_image)
            progress = view.findViewById(R.id.photo_full_progress)
        }

        override fun onClick(v: View?) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION)
                listener.onFullImageClick()
        }

        fun bind(position: Int) {
            GestureViewSettings().applyDefault(image)
            progress.animate().setDuration(150L)
                .setStartDelay(PROGRESS_DELAY)
                .alpha(1f)
            // Loading image

            DemoGlideHelper().loadThumb(mediaData[position], thumbImage, token)

            // Loading image
            DemoGlideHelper().loadFull(mediaData[position],
                image,
                token,
                object : DemoGlideHelper.LoadingListener {
                    override fun onSuccess() {
                        progress.animate().cancel()
                        progress.animate().alpha(0f)
                    }

                    override fun onError() {
                        progress.animate().alpha(0f)
                    }
                })
        }
    }

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        //Minimum Video you want to buffer while Playing
        private val MIN_BUFFER_DURATION = 2000
        //Max Video you want to buffer during PlayBack
        private val MAX_BUFFER_DURATION = 2000
        //Min Video you want to buffer before start Playing it
        private val MIN_PLAYBACK_START_BUFFER = 1500
        //Min video You want to buffer when user resumes video
        private val MIN_PLAYBACK_RESUME_BUFFER = 2000
        private val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16))
            .setBufferDurationsMs(
                MIN_BUFFER_DURATION,
                MAX_BUFFER_DURATION,
                MIN_PLAYBACK_START_BUFFER,
                MIN_PLAYBACK_RESUME_BUFFER
            )
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        private val defaultHttpDataSource = DefaultHttpDataSource.Factory()
        private val dataSourceFactory = DataSource.Factory {
            val dataSource: HttpDataSource = defaultHttpDataSource.createDataSource()
            // Set a custom authentication request header.
            dataSource.setRequestProperty("Authorization", token)
            dataSource
        }
        private val exoPlayer by lazy {
            ExoPlayer.Builder(itemView.context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(dataSourceFactory))
                .setLoadControl(loadControl)
                .apply {
                    setSeekBackIncrementMs(5000)
                    setSeekForwardIncrementMs(5000)
                }.build()
        }
        val gestureView: GestureFrameLayout = itemView.findViewById(R.id.video_view)
        private val video: StyledPlayerView = itemView.findViewById(R.id.full_videoPlayer)

        fun bind(position: Int) {

            exoPlayerPositionMap[position] = exoPlayer

            video.apply {
                player = exoPlayer
                keepScreenOn = true
            }

            val url = Constants.BASE_GET_MEDIA_URL + mediaData[position].id

            exoPlayer.apply {
                setMediaItem(MediaItem.fromUri(url))
            }
        }

        override fun onClick(v: View?) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION)
                listener.onFullImageClick()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIDEO_VIEW) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.demo_item_video_full, parent, false)
            val holder = VideoViewHolder(view)
//            GestureViewSettings().applyDefault(holder.videoThumbnail)

//            holder.videoThumbnail.positionAnimator.addPositionUpdateListener { position: Float, isLeaving: Boolean ->
//                holder.videoThumbnail.visibility = if (position == 1f) View.INVISIBLE else View.VISIBLE
//            }

            return holder
        } else {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.demo_item_photo_full, parent, false)
            val holder = ImageViewHolder(view)

            GestureViewSettings().applyDefault(holder.image)
            GestureViewSettings().applyDefault(holder.thumbImage)

            holder.thumbImage.positionAnimator.addPositionUpdateListener { position: Float, isLeaving: Boolean ->
                holder.progress.visibility = if (position == 1f) View.VISIBLE else View.INVISIBLE
                holder.thumbImage.visibility = if (position == 1f) View.INVISIBLE else View.VISIBLE
                holder.image.visibility = if (position == 1f) View.VISIBLE else View.INVISIBLE
            }

            val controller: GestureController = holder.image.controller
            controller.addOnStateChangeListener(DynamicZoom(controller.settings))
            return holder
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == IMAGE_VIEW) {
            (holder as ImageViewHolder).bind(position)
        } else {
            (holder as VideoViewHolder).bind(position)
        }
    }

    //todo fix below commented code we need to properly recycle pages
//    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
//        super.onViewRecycled(holder)
//        DemoGlideHelper().clear(holder.image)
//        holder.progress.animate().cancel()
//        holder.progress.alpha = 0f
//        holder.image.setImageDrawable(null)
//    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    //pause video on next page
    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        exoPlayerPositionMap?.values?.forEach {
            it.pause()
        }
        val player = exoPlayerPositionMap[holder.absoluteAdapterPosition] ?: return
        player.prepare()
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        exoPlayerPositionMap?.values?.forEach {
            it.prepare()
        }
    }

    fun getMedia(pos: Int): View? {
        val holder =
            if (recyclerView == null) null else recyclerView!!.findViewHolderForLayoutPosition(pos)
        return if (holder == null) null
            else if(getItemViewType(pos) == VIDEO_VIEW) (holder as VideoViewHolder).gestureView
            else (holder as ImageViewHolder).thumbImage
    }

    fun getMediaData(pos: Int): MediaData? {
        return if (pos < 0 || pos >= mediaData.size) null else mediaData[pos]
    }

    fun removeItem(imgToDel: MediaData) {
        val pos = mediaData.indexOf(imgToDel)
        mediaData.remove(imgToDel)
        notifyItemRemoved(pos)
    }

    override fun getItemCount(): Int {
        return if (!activated) 0 else mediaData.size
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