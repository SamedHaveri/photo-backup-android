package com.example.photobackup.ui.main.photos
import android.util.Log
import androidx.lifecycle.*
import com.example.photobackup.models.imageDownload.ImageData
import com.example.photobackup.other.Constants
import com.example.photobackup.other.Resource
import com.example.photobackup.other.Urls
import com.example.photobackup.repository.MainRepository
import com.example.photobackup.util.MyPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.net.ConnectException
import javax.inject.Inject
import kotlin.math.log

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val myPreference: MyPreference,
): ViewModel() {

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
                if (it.code() == 200){
                    _imagesData.postValue(Resource.success(it.body()))
                    Log.d("resp", it.body().toString())
                } else if (it.code() == 403){
                    _imagesData.postValue(Resource.error("Token Expired", null))
                }else {
                    _imagesData.postValue(Resource.error(it.code().toString(), null))
                }
            }
        }catch (ex: ConnectException){
            _imagesData.postValue(Resource.error("API Connection Error", null))
        }
    }

    init {
        getImagesData()
    }

}