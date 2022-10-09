package com.example.photobackup.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.NoTransition
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.transition.Transition.ViewAdapter
import com.bumptech.glide.request.transition.TransitionFactory
import com.example.photobackup.models.imageDownload.ImageData
import com.example.photobackup.other.Constants

class DemoGlideHelper {

    private val TRANSITION =
        Transition { current: Drawable?, adapter: ViewAdapter ->
            if (adapter.view is ImageView) {
                val image = adapter.view as ImageView
                if (image.drawable == null) {
                    image.alpha = 0f
                    image.animate().alpha(1f).duration = 150L
                }
            }
            false
        }

    private val TRANSITION_FACTORY =
        TransitionFactory { dataSource: DataSource, _: Boolean ->
            if (dataSource == DataSource.REMOTE) TRANSITION else NoTransition.get() }

    fun loadThumb(imageData: ImageData, image:ImageView, token:String){
        val thumbUrl = GlideUrl(
            Constants.BASE_GET_THUMBNAIL+imageData.id, LazyHeaders.Builder()
                .addHeader("Authorization", token)
                .build()
        )
        val mediumUrl = GlideUrl(
            Constants.BASE_GET_MID_THUMBNAIL+imageData.id, LazyHeaders.Builder()
                .addHeader("Authorization", token)
                .build()
        )
        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .dontTransform()

        val thumbOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .dontTransform()

        val thumbRequest: RequestBuilder<Drawable> = Glide.with(image)
            .load(thumbUrl)
            .apply(thumbOptions)
            .transition(DrawableTransitionOptions.with(TRANSITION_FACTORY))
        Glide.with(image)
            .load(mediumUrl)
            .apply(options)
            .thumbnail(thumbRequest)
            .into(image)
    }

    fun loadFull(imageData: ImageData, image: ImageView, token: String, listener: LoadingListener){
        val thumbUrl = GlideUrl(
            Constants.BASE_GET_MID_THUMBNAIL+imageData.id, LazyHeaders.Builder()
                .addHeader("Authorization", token)
                .build()
        )
        val fullUrl = GlideUrl(
            Constants.BASE_GET_IMAGES_URL+imageData.id, LazyHeaders.Builder()
                .addHeader("Authorization", token)
                .build()
        )
        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .override(2200, 2200)
            .dontTransform()

        val thumbOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .dontTransform()

        val thumbRequest: RequestBuilder<Drawable> = Glide.with(image)
            .load(thumbUrl)
            .apply(thumbOptions)
            .transition(DrawableTransitionOptions.with(TRANSITION_FACTORY))

        Glide.with(image)
            .load(fullUrl)
            .apply(RequestOptions().apply(options).placeholder(image.drawable))
            .thumbnail(thumbRequest)
            .listener(RequestListenerWrapper(listener))
            .into(image)
    }

    fun clear(view: ImageView) {
        Glide.with(view).clear(view)
        view.setImageDrawable(null)
    }

    interface LoadingListener {
        fun onSuccess()
        fun onError()
    }

    private class RequestListenerWrapper<T> constructor(listener: LoadingListener?) :
        RequestListener<T> {
        private val listener: LoadingListener?
        init {
            this.listener = listener
        }
        override fun onResourceReady(
            resource: T, model: Any, target: Target<T>,
            dataSource: DataSource, isFirstResource: Boolean,
        ): Boolean {
            listener?.onSuccess()
            return false
        }

        override fun onLoadFailed(
            ex: GlideException?, model: Any,
            target: Target<T>, isFirstResource: Boolean,
        ): Boolean {
            listener?.onError()
            return false
        }
    }

}