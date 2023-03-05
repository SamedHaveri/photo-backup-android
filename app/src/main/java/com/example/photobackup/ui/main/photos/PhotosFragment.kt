package com.example.photobackup.ui.main.photos

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.example.photobackup.models.imageDownload.ImageData
import com.example.photobackup.other.Status
import com.example.photobackup.service.PhotosContentJob
import com.example.photobackup.ui.login.LoginActivity
import com.example.photobackup.ui.main.photos.adapter.PagerAdapter
import com.example.photobackup.ui.main.photos.adapter.RecyclerAdapter
import com.example.photobackup.util.DecorUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PhotosFragment : AppCompatActivity(), RecyclerAdapter.OnPhotoListener {

    private val NO_POSITION = -1
    private val STORAGE_PERMISSION_CODE = 1
    private var views: ViewHolder? = null
    private var imageAnimator: ViewsTransitionAnimator<*>? = null
    private var listAnimator: ViewsTransitionAnimator<Int>? = null
    private var gridAdapter: RecyclerAdapter? = null
    private var pagerAdapter: PagerAdapter? = null
    private var pagerListener: OnPageChangeCallback? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_photos)
        this.views = ViewHolder(this)
        val photosViewModel: PhotosViewModel by viewModels()
        val authDetails = photosViewModel.authDetails

        if (ContextCompat.checkSelfPermission(applicationContext,
                READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("Storage permission required for media sync")
                    .setPositiveButton("ok") { dialog, which ->
                        ActivityCompat.requestPermissions(this,
                            arrayOf(READ_EXTERNAL_STORAGE),
                            STORAGE_PERMISSION_CODE)
                    }
                    .setNegativeButton("cancel") { dialog, which ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE)
            }
        }

        if(!PhotosContentJob.isScheduled(baseContext)){
            PhotosContentJob.scheduleJob(baseContext);
        }else{
            Log.d("upload", "Job already scheduled")
        }

//        setAppBarStateListAnimator(views.appBar)
        initDecorMargins()

        var bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_home
        @Suppress("DEPRECATION")
        bottomNavigationView.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
//                    startActivity(Intent(applicationContext, DashBoard::class.java))
                    overridePendingTransition(0, 0)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.home -> return@OnNavigationItemSelectedListener true
                R.id.navigation_notifications -> {
//                    startActivity(Intent(applicationContext, About::class.java))
                    overridePendingTransition(0, 0)
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        })

        val imagesData = photosViewModel.imageData.observe(this, Observer {
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
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    //get photo thumbnail from cache
                }
            }
        })
        //todo save position
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show()
            }
        }
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

    override fun onBackPressed() {
        if (!listAnimator!!.isLeaving) {
            listAnimator!!.exit(true) // Exiting from full pager
        } else if (!imageAnimator!!.isLeaving) {
            imageAnimator!!.exit(true) // Exiting from full top image
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun initDecorMargins() {
        DecorUtils.size(views!!.appBar, Gravity.TOP)
//        DecorUtils.padding(views!!.toolbar, Gravity.TOP)
        DecorUtils.padding(views!!.pagerToolbar, Gravity.TOP)
        DecorUtils.padding(views!!.grid, Gravity.BOTTOM)
        DecorUtils.margin(views!!.pagerTitle, Gravity.BOTTOM)
    }

    private fun initGrid(imagesData: List<ImageData>, token: String) {
        // Setting up images grid
        val cols = resources.getInteger(R.integer.images_grid_columns)
        views!!.grid.layoutManager = GridLayoutManager(this, cols)
        gridAdapter = RecyclerAdapter(this, this, imagesData, token)
        views!!.grid.adapter = gridAdapter
    }

    private fun initPager(imagesData: List<ImageData>, token: String) {
        // Setting up pager adapter
        pagerAdapter = PagerAdapter(this, imagesData, token)
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
        views!!.pager.offscreenPageLimit = 10
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
        views!!.pager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                val playerRight = pagerAdapter!!.exoPlayerPositionMap[position + 1]
                playerRight?.stop()
                val playerLeft = pagerAdapter!!.exoPlayerPositionMap[position - 1]
                playerLeft?.stop()
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
            }
        })
    }

    /**
     * Setting up photo title for current pager position.
     */
    private fun onPhotoInPagerSelected(position: Int) {
        val imageData: ImageData? = pagerAdapter!!.getImageData(position)
        if (imageData == null) {
            views!!.pagerTitle.text = null
        } else {
            val title = SpannableBuilder(this)
//            title.append(imageData.name)
//                .createStyle().setColorResId(R.color.demo_photo_subtitle).apply()
//            views!!.pagerTitle.text = title.build()
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
                return pagerAdapter!!.getMedia(pos)
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

    override fun onPhotoClick(position: Int) {
        pagerAdapter!!.setActivated(true)
        listAnimator!!.enter(position, true)
    }

    override fun onPause() {
        super.onPause()
        pagerAdapter?.exoPlayerPositionMap?.values?.forEach {
            it.stop()
        }
    }

    /**
     * Checks if system UI (status bar and navigation bar) is shown or we are in fullscreen mode.
     */
    // New insets controller API is not back-ported yet
    @Suppress("DEPRECATED_IDENTITY_EQUALS")
    private fun isSystemUiShown(): Boolean {
        @Suppress("DEPRECATION")
        return (window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) === 0
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
        window.decorView.systemUiVisibility = if (show) 0 else flags
    }

    private class ViewHolder(activity: Activity) {
        //        val toolbar: Toolbar
        val appBar: View
        val grid: RecyclerView
        val fullBackground: View
        val pager: ViewPager2
        val pagerTitle: TextView
        val pagerToolbar: Toolbar

        init {
//            toolbar = activity.findViewById(R.id.toolbar)
            appBar = activity.findViewById(R.id.demo_app_bar)
            grid = activity.findViewById(R.id.demo_grid)
            fullBackground = activity.findViewById(R.id.demo_full_background)
            pager = activity.findViewById(R.id.demo_pager)
            pagerTitle = activity.findViewById(R.id.demo_pager_title)
            pagerToolbar = activity.findViewById(R.id.demo_pager_toolbar)
        }
    }

}