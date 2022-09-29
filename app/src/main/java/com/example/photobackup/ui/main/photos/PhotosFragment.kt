package com.example.photobackup.ui.main.photos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.photobackup.R
import com.example.photobackup.databinding.FragmentPhotosBinding
import com.example.photobackup.other.Status
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

        val recyclerView: RecyclerView = binding.recyclerView
        val gridLayoutManager = GridLayoutManager(context, 3)
        recyclerView.layoutManager = gridLayoutManager
        mainViewModel.urls.observe(viewLifecycleOwner, Observer {
            when(it.status){
                Status.SUCCESS -> {
                    val recyclerAdapter = RecyclerAdapter(requireContext().applicationContext,
                        it.data!!, authDetails.token!!)
                    recyclerView.adapter = recyclerAdapter
                    recyclerAdapter.setOnItemClickListener(object : RecyclerAdapter.OnItemClickListener{
                        override fun onItemClick(position: Int) {
                            mainViewModel.setCurrent(position)
                            val intent = Intent(requireContext().applicationContext, PhotosPagerFragment::class.java)
                            intent.putExtra("current", position)
//                            val currentImage = recyclerView.findViewHolderForAdapterPosition(position)!!.itemView
//                            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                                activity!!, currentImage, currentImage.transitionName)
                            startActivity(intent)
                            Log.d("click", "Item $position clicked")
                        }
                    })
                }
                Status.LOADING ->{
//                    Toast.makeText(this@LoginActivity, "Loading", Toast.LENGTH_SHORT).show()
                }
                Status.ERROR ->{
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    //get photo thumbnail from cache
                }
            }
        })
        mainViewModel.getImagesData()
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}