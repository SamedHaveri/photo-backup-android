package com.example.photobackup.ui.main.photos

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.annotation.AnimRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.photobackup.R
import com.example.photobackup.databinding.FragmentPhotosBinding
import com.example.photobackup.other.Constants
import com.example.photobackup.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotosFragment : Fragment() {

    private var _binding: FragmentPhotosBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPhotosBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val mainViewModel: MainViewModel by viewModels()
        val authDetails = mainViewModel.authDetails

        val url = GlideUrl(
            Constants.BASE_GET_IMAGES_URL + 13, LazyHeaders.Builder()
                .addHeader("Authorization", authDetails.token!!)
                .build()
        )
        Glide.with(requireContext()).load(url).into(binding.img)

//        val recyclerView: RecyclerView = binding.recyclerView
//        val gridLayoutManager = GridLayoutManager(context, 3)
//        recyclerView.layoutManager = gridLayoutManager
//
//        viewModel.imageData.observe(this, Observer {
//            when(it.status){
//                Status.SUCCESS -> {
//                    val urls: List<String> = it.data!!.map { Constants.BASE_GET_IMAGES_URL + id }
//                    val customAdapter = CustomAdapter(requireContext(), urls)
//                    recyclerView.adapter = customAdapter
//                }
//                Status.LOADING ->{
////                    Toast.makeText(this@LoginActivity, "Loading", Toast.LENGTH_SHORT).show()
//                }
//                Status.ERROR ->{
//                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
//                }
//            }
//        })
//        viewModel.getImagesData(token!!)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}