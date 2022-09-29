package com.example.photobackup.ui.main.photos

import android.graphics.pdf.PdfDocument.Page
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.example.photobackup.R
import com.example.photobackup.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotosPagerFragment : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val position = intent.extras!!.getInt("current")
        setContentView(R.layout.fragment_photos_pager)
        val mainViewModel: MainViewModel by viewModels()
        val authDetails = mainViewModel.authDetails
        val viewPager2 = findViewById<ViewPager2>(R.id.PhotosPager)
        mainViewModel.urls.observe(this, Observer { it ->
            val pagerAdapter = PagerAdapter(applicationContext, it.data!!,
                authDetails.token!!)
            viewPager2.adapter = pagerAdapter
            viewPager2.setCurrentItem(position, false)
        })
    }
}