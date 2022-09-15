package com.example.photobackup.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photobackup.models.AuthRequest
import com.example.photobackup.models.AuthResponse
import com.example.photobackup.other.Resource
import com.example.photobackup.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.net.ConnectException
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val mainRepository: MainRepository,
) : ViewModel() {
    private val _res = MutableLiveData<Resource<AuthResponse>>()

    val res: LiveData<Resource<AuthResponse>>
        get() = _res

    fun authenticate(username: String, password: String) = viewModelScope.launch {
        _res.postValue(Resource.loading(null))
        try {
            mainRepository.authenticate(AuthRequest(username, password)).let {
                if (it.code() == 200) {
                    _res.postValue(Resource.success(it.body()))
                } else if (it.code() == 403){
                    _res.postValue(Resource.error("Wrong username or password", null))
                } else {
                    _res.postValue(Resource.error(it.errorBody().toString(), null))
                }
            }
        }catch (ex:ConnectException){
            _res.postValue(Resource.error("Error API Connection", null))
        }
    }
}