package com.example.photobackup.ui.main.photos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.alexvasilkov.android.commons.texts.SpannableBuilder
import com.alexvasilkov.gestures.commons.DepthPageTransformer
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.alexvasilkov.gestures.transition.tracker.SimpleTracker
import com.example.photobackup.R
import com.example.photobackup.databinding.FragmentPhotosBinding
import com.example.photobackup.models.imageDownload.ImageData
import com.example.photobackup.other.Status
import com.example.photobackup.ui.main.photos.adapter.PagerAdapter
import com.example.photobackup.ui.main.photos.adapter.RecyclerAdapter
import com.example.photobackup.util.DecorUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotosFragment : Fragment(), RecyclerAdapter.OnPhotoListener {

    private var _binding: FragmentPhotosBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val NO_POSITION = -1

    private var views: ViewHolder? = null
    private var imageAnimator: ViewsTransitionAnimator<*>? = null
    private var listAnimator: ViewsTransitionAnimator<Int>? = null
    private var gridAdapter: RecyclerAdapter? = null
    private var pagerAdapter: PagerAdapter? = null
    private var pagerListener: OnPageChangeCallback? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
//        _binding = FragmentPhotosBinding.inflate(inflater, container, false)
        val root = inflater.inflate(R.layout.fragment_photos, container, false)
        this.views = ViewHolder(root)
        val photosViewModel: PhotosViewModel by viewModels()
        val authDetails = photosViewModel.authDetails

        //        setAppBarStateListAnimator(views.appBar)
//        initDecorMargins()

        val imagesData = photosViewModel.imageData.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.SUCCESS -> {
                    initGrid(it.data!!, authDetails.token!!)
                    initPager(it.data, authDetails.token)
                    initPagerAnimator()

//                    if (savedPagerPosition != NO_POSITION) {
//                        // A photo was shown in the pager, we should switch to pager mode instantly
//                        applyFullPagerState(1f, false)
//                    }//todo add this future feature
                }
                Status.LOADING -> {
//                    Toast.makeText(this@LoginActivity, "Loading", Toast.LENGTH_SHORT).show()
                }
                Status.ERROR -> {
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    //get photo thumbnail from cache
                }
            }
        })

        return root
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        activity?.onBackPressedDispatcher?.addCallback(this,
//            object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                if (!listAnimator!!.isLeaving) {
//                    listAnimator.exit(true) // Exiting from full pager
//                } else if (!imageAnimator!!.isLeaving) {
//                    imageAnimator.exit(true) // Exiting from full top image
//                } else {
//                    activity?.onBackPressed()
//                }
//            }
//        })
//    }

    private fun onBackPressed() {
        if (!listAnimator!!.isLeaving) {
            listAnimator!!.exit(true) // Exiting from full pager
        } else if (!imageAnimator!!.isLeaving) {
            imageAnimator!!.exit(true) // Exiting from full top image
        } else {
            @Suppress("DEPRECATION")
            activity?.onBackPressed()
        }
    }

    private fun initDecorMargins() {
//        DecorUtils.size(views!!.appBar, Gravity.TOP)
//        DecorUtils.padding(views.toolbar, Gravity.TOP)
//        DecorUtils.padding(views!!.pagerToolbar, Gravity.TOP)
//        DecorUtils.padding(views!!.grid, Gravity.BOTTOM)
//        DecorUtils.margin(views!!.pagerTitle, Gravity.BOTTOM)
    }

    private fun initGrid(imagesData: List<ImageData>, token: String) {
        // Setting up images grid
        val cols = resources.getInteger(R.integer.images_grid_columns)
        views!!.grid.layoutManager = GridLayoutManager(requireContext(), cols)
        gridAdapter = RecyclerAdapter(this, requireContext(), imagesData, token)
        views!!.grid.adapter = gridAdapter
    }

    private fun initPager(imagesData: List<ImageData>, token: String) {
        // Setting up pager adapter

        pagerAdapter = PagerAdapter(requireContext(), imagesData, token)
        // Enabling immersive mode by clicking on full screen image
        pagerAdapter!!.setImageClickListener(object : PagerAdapter.ImageClickListener {
            override fun onFullImageClick() {
                if (!listAnimator!!.isLeaving) {
                    // Toggle immersive mode
                    showSystemUi(!isSystemUiShown())
                }
            }
        })
        pagerListener = object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                onPhotoInPagerSelected(position)
            }
        }
        views!!.pager.adapter = pagerAdapter
        views!!.pager.registerOnPageChangeCallback(pagerListener as OnPageChangeCallback)
        views!!.pager.setPageTransformer(DepthPageTransformer())

        // Setting up pager toolbar
        views!!.pagerToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        views!!.pagerToolbar.setNavigationOnClickListener { onBackPressed() }

        DecorUtils.onInsetsChanged(views!!.pagerToolbar) {
            val alpha = if (isSystemUiShown()) 1f else 0f
            views!!.pagerToolbar.animate().alpha(alpha)
            views!!.pagerTitle.animate().alpha(alpha)
        }
    }

    /**
     * Setting up photo title for current pager position.
     */
    private fun onPhotoInPagerSelected(position: Int) {
        val imageData: ImageData? = pagerAdapter!!.getImageData(position)
        if (imageData == null) {
            views!!.pagerTitle.text = null
        } else {
            val title = SpannableBuilder(requireContext())
            title.append(imageData.name)
                .createStyle().setColorResId(R.color.demo_photo_subtitle).apply()
            views!!.pagerTitle.text = title.build()
        }
    }

    private fun initPagerAnimator() {
        val gridTracker: SimpleTracker = object : SimpleTracker() {
            override fun getViewAt(pos: Int): View? {
                return gridAdapter!!.getImage(pos)
            }
        }
        val pagerTracker: SimpleTracker = object : SimpleTracker() {
            override fun getViewAt(pos: Int): View? {
                return pagerAdapter!!.getImage(pos)
            }
        }
        listAnimator = GestureTransitions
            .from(views!!.grid, gridTracker)
            .into(views!!.pager, pagerTracker)

        // Setting up and animating image transition
        listAnimator!!.addPositionUpdateListener { position: Float, isLeaving: Boolean ->
            this.applyFullPagerState(position,
                isLeaving)
        }
    }

    /**
     * Applying pager image animation state: fading out toolbar, title and background.
     */
    private fun applyFullPagerState(position: Float, isLeaving: Boolean) {
        views!!.fullBackground.visibility = if (position == 0f) View.INVISIBLE else View.VISIBLE
        views!!.fullBackground.alpha = position
        views!!.pagerToolbar.visibility = if (position == 0f) View.INVISIBLE else View.VISIBLE
        views!!.pagerToolbar.alpha = if (isSystemUiShown()) position else 0f
        views!!.pagerTitle.visibility = if (position == 1f) View.VISIBLE else View.INVISIBLE
        if (isLeaving && position == 0f) {
            pagerAdapter!!.setActivated(false)
            showSystemUi(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPhotoClick(position: Int) {
        pagerAdapter!!.setActivated(true)
        listAnimator!!.enter(position, true)
    }

    /**
     * Checks if system UI (status bar and navigation bar) is shown or we are in fullscreen mode.
     */
    // New insets controller API is not back-ported yet
    @Suppress("DEPRECATED_IDENTITY_EQUALS")
    private fun isSystemUiShown(): Boolean {
        @Suppress("DEPRECATION")
        return (requireActivity().window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) === 0
    }

    /**
     * Shows or hides system UI (status bar and navigation bar).
     */
    @Suppress("DEPRECATED_IDENTITY_EQUALS", "DEPRECATION")
    // New insets controller API is not back-ported yet
    private fun showSystemUi(show: Boolean) {
        val flags = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
        requireActivity().window.decorView.systemUiVisibility = if (show) 0 else flags
    }

    private class ViewHolder(activity: View) {
//        val toolbar: Toolbar
//        val appBar: View

        //        val appBarImage: ImageView
        val grid: RecyclerView
        val fullBackground: View
        val pager: ViewPager2
        val pagerTitle: TextView
        val pagerToolbar: Toolbar
//        val fullImage: GestureImageView
//        val fullImageToolbar: Toolbar

        init {
//            toolbar = activity.findViewById(R.id.toolbar)
//            appBar = activity.findViewById(R.id.demo_app_bar)
//            appBarImage = activity.findViewById(R.id.demo_app_bar_image)
            grid = activity.findViewById(R.id.demo_grid)
            fullBackground = activity.findViewById(R.id.demo_full_background)
            pager = activity.findViewById(R.id.demo_pager)
            pagerTitle = activity.findViewById(R.id.demo_pager_title)
            pagerToolbar = activity.findViewById(R.id.demo_pager_toolbar)
//            fullImage = activity.findViewById(R.id.demo_full_image)
//            fullImageToolbar = activity.findViewById(R.id.demo_full_image_toolbar)
        }
    }

}