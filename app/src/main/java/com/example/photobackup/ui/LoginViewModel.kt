package com.example.photobackup.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.photobackup.models.AuthResponse
import com.example.photobackup.other.Resource
import com.example.photobackup.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val mainRepository: MainRepository
):ViewModel() {
    private val username = MutableLiveData<String>()
    private val password = MutableLiveData<String>()
}