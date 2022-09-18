package com.example.photobackup.ui.main
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobackup.models.imageDownload.ImageData
import com.example.photobackup.other.Resource
import com.example.photobackup.repository.MainRepository
import com.example.photobackup.util.MyPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val myPreference: MyPreference,
): ViewModel() {

    val authDetails by lazy { myPreference.getStoredAuthDetails() }

    private val _imagesData = MutableLiveData<Resource<List<ImageData>>>()
    val imageData: LiveData<Resource<List<ImageData>>>
        get() = _imagesData

    fun getImagesData() = viewModelScope.launch {
        mainRepository.getImages(myPreference.getStoredToken()).let {
            if (it.code() == 200){
                _imagesData.postValue(Resource.success(it.body()))
            } else if (it.code() == 403){
                _imagesData.postValue(Resource.error("Token Expired", null))
            }else {
                _imagesData.postValue(Resource.error("API Error", null))
            }
        }
    }
}