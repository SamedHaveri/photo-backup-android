package com.example.photobackup.ui.main.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.photobackup.models.imageDownload.ImageData

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }

    private val _images = MutableLiveData<List<ImageData>>().apply {}

    val text: LiveData<String> = _text
}