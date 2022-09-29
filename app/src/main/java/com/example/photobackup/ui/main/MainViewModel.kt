package com.example.photobackup.ui.main
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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val myPreference: MyPreference,
): ViewModel() {

    val authDetails by lazy { myPreference.getStoredAuthDetails() }

    private val _urls = MutableLiveData<Resource<Urls>>()
    val urls: LiveData<Resource<Urls>>
        get() = _urls

    private val _imagesData = MutableLiveData<Resource<List<ImageData>>>()
    val imageData: LiveData<Resource<List<ImageData>>>
        get() = _imagesData

    private val _current = MutableLiveData<Int>()
    val current: LiveData<Int>
        get() = _current

    fun setCurrent(value:Int){
        _current.value = value
    }

    fun getImagesData() = viewModelScope.launch {
        try {
            mainRepository.getImages(myPreference.getStoredToken()).let {
                if (it.code() == 200){
                    Log.d("imgdata", it.body().toString())
                    _imagesData.postValue(Resource.success(it.body()))
                    val urls = Urls(
                        it.body()!!.map { item -> Constants.BASE_GET_IMAGES_URL + item.id },
                        it.body()!!.map { item -> Constants.BASE_GET_THUMBNAIL + item.id })
                    _urls.postValue(Resource.success(urls))
                } else if (it.code() == 403){
                    _imagesData.postValue(Resource.error("Token Expired", null))
                    _urls.postValue(Resource.error("Token Expired", null))
                }else {
                    _imagesData.postValue(Resource.error("API Error", null))
                    _urls.postValue(Resource.error("API Error", null))
                }
            }
        }catch (ex: ConnectException){
            _imagesData.postValue(Resource.error("API Connection Error", null))
            _urls.postValue(Resource.error("API Connection Error", null))
        }
    }

    init {
        getImagesData()
    }

}