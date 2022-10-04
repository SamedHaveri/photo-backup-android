package com.example.photobackup.ui.main.photos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.photobackup.R
import com.example.photobackup.models.imageDownload.ImageData
import com.example.photobackup.util.DemoGlideHelper


class RecyclerAdapter(
    private val listener: OnPhotoListener,
    private val context: Context,
    private val imagesData: List<ImageData>,
    private val token: String,
) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    private var recyclerView: RecyclerView? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        internal val image: ImageView

        init {
            image = view.findViewById(R.id.demo_item_image)
            image.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION)
                listener.onPhotoClick(position)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.demo_item_photo, viewGroup, false)
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
//        viewHolder.image.setTag(R.tag_item, imagesData[position])
        DemoGlideHelper().loadThumb(imagesData[position], viewHolder.image, token)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        DemoGlideHelper().clear(holder.image)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    fun getImage(pos: Int): ImageView? {
        val holder: RecyclerView.ViewHolder? = if (recyclerView == null) null else recyclerView?.findViewHolderForLayoutPosition(pos)
        return if (holder == null) null else (holder as ViewHolder).image
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = imagesData.size
    interface OnPhotoListener {
        fun onPhotoClick(position: Int)
    }
}