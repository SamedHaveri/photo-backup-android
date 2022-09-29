package com.example.photobackup.ui.main.photos

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.photobackup.R
import com.example.photobackup.other.Urls


class RecyclerAdapter(
    private val context: Context,
    private val urls: Urls,
    private val token: String,
):
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        mListener = listener
    }

    class ViewHolder(view: View, listener: OnItemClickListener) : RecyclerView.ViewHolder(view) {
        val image: ImageView
        init {
            // Define click listener for the ViewHolder's View.
            image = view.findViewById(R.id.image)
//            image.transitionName = "image_focused"
            image.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.image, viewGroup, false)
        return ViewHolder(view, mListener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
//        viewHolder.image = urls[position]

        val url = GlideUrl(
            urls.thumbnail[position], LazyHeaders.Builder()
                .addHeader("Authorization", token)
                .build()
        )
        Glide
            .with(context)
            .load(url)
            .centerCrop()
            .into(viewHolder.image)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = urls.thumbnail.size

}