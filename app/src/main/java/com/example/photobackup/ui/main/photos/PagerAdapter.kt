package com.example.photobackup.ui.main.photos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.photobackup.R
import com.example.photobackup.other.Urls

class PagerAdapter(
    private val context: Context,
    private val urls: Urls,
    private val token: String,
) : RecyclerView.Adapter<PagerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView
        init {
            image = view.findViewById(R.id.pagerImage)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pager_content, parent, false)
        return PagerAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
//        viewHolder.image = urls[position]
        val url = GlideUrl(
            urls.original[position], LazyHeaders.Builder()
                .addHeader("Authorization", token)
                .build()
        )
        Glide
            .with(context)
            .load(url)
            .into(holder.image)
    }

    override fun getItemCount(): Int {
        return urls.original.size
    }

}