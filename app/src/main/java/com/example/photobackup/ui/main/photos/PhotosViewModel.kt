package com.example.photobackup.ui.main.photos

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobackup.models.imageDownload.ImageData
import com.example.photobackup.other.Constants
import com.example.photobackup.other.Resource
import com.example.photobackup.repository.MainRepository
import com.example.photobackup.util.MyPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.net.ConnectException
import javax.inject.Inject


@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val myPreference: MyPreference,
) : ViewModel() {

    val authDetails by lazy { myPreference.getStoredAuthDetails() }

    private val _imagesData = MutableLiveData<Resource<List<ImageData>>>()
    val imageData: LiveData<Resource<List<ImageData>>>
        get() = _imagesData

    private val _savedPagerPosition = MutableLiveData<Int>()
    val savedPagerPosition: LiveData<Int>
        get() = _savedPagerPosition

    private val _savedGridPosition = MutableLiveData<Int>()
    val savedGridPosition: LiveData<Int>
        get() = _savedGridPosition

    private val _savedGridPositionFromTop = MutableLiveData<Int>()
    val savedGridPositionFromTop: LiveData<Int>
        get() = _savedGridPositionFromTop

    private val _savedPhotoCount = MutableLiveData<Int>()
    val savedPhotoCount: LiveData<Int>
        get() = _savedPhotoCount


    fun getImagesData() = viewModelScope.launch {
        try {
            mainRepository.getImages(myPreference.getStoredToken()).let {
                if (it.code() == 200) {
                    _imagesData.postValue(Resource.success(it.body()))
                } else if (it.code() == 403) {
                    _imagesData.postValue(Resource.error("Token Expired", null))
                } else {
                    _imagesData.postValue(Resource.error(it.code().toString(), null))
                }
            }
        } catch (ex: ConnectException) {
            _imagesData.postValue(Resource.error("API Connection Error", null))
        }

    }

    fun deleteMedia(id: Int) {
        viewModelScope.launch {
            try {
                //todo classic repository failing me :D raw OkHttp it is
//                mainRepository.deleteMedia(myPreference.getStoredToken(), id)
                val policy = ThreadPolicy.Builder()
                    .permitAll().build()
                StrictMode.setThreadPolicy(policy)
                val client = OkHttpClient().newBuilder().build()
                val mediaType = MediaType.parse("text/plain")
                val body = RequestBody.create(mediaType, "")
                val request = Request.Builder()
                    .url(Constants.BASE_URL+"media/id/"+id)
                    .method("DELETE", body)
                    .addHeader("Authorization", myPreference.getStoredToken())
                    .build()

                val response = client.newCall(request).execute()

            } catch ( e : java.lang.Exception){
                Log.e("exception", e.stackTrace.contentToString())
            }
        }
    }


    init {
        getImagesData()
    }

}