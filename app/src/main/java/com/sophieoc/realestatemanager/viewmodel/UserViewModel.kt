package com.sophieoc.realestatemanager.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.sophieoc.realestatemanager.model.UserWithProperties
import com.sophieoc.realestatemanager.repository.UserRepository
import com.sophieoc.realestatemanager.utils.AbsentLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val userSource: UserRepository) : ViewModel() {
    private var _currentUser = userSource.currentUser
    val currentUser : LiveData<UserWithProperties>
        get() = _currentUser
    val userUpdated: LiveData<UserWithProperties> = Transformations.switchMap(_currentUser) {
        if (_currentUser.value != null)
            userSource.upsertUser(it)
        else
            AbsentLiveData.create()
    }

    fun getUserById(uid: String): LiveData<UserWithProperties> = userSource.getUserWithProperties(uid)

    fun updateUser(user: UserWithProperties) {
        _currentUser.value = user
    }
}