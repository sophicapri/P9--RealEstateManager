package com.sophieoc.realestatemanager.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sophieoc.realestatemanager.model.UserWithProperties
import com.sophieoc.realestatemanager.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val userSource: UserRepository) : ViewModel() {
    private var _currentUser: MutableStateFlow<UserUiState> = getMutableUser()
    val currentUser: StateFlow<UserUiState> = _currentUser

    private fun getMutableUser(): MutableStateFlow<UserUiState> {
        val userMutableStateFlow: MutableStateFlow<UserUiState> = MutableStateFlow(UserUiState.Loading)
        viewModelScope.launch {
            userSource.currentUser.catch { e -> UserUiState.Error(e) }
                .collect { user ->
                    if (user == null) userMutableStateFlow.value = UserUiState.Loading
                    else userMutableStateFlow.value = UserUiState.Success(user)
                }
        }
        return userMutableStateFlow
    }

    fun getUserById(uid: String): StateFlow<UserUiState> {
        val userMutable: MutableStateFlow<UserUiState> = MutableStateFlow(UserUiState.Loading)
        viewModelScope.launch {
            userSource.getUserWithProperties(uid)
                .catch { e -> userMutable.value = UserUiState.Error(e) }
                .collect { user ->
                    if (user == null)
                        userMutable.value = UserUiState.Loading
                    else
                        userMutable.value = UserUiState.Success(user)
                }
        }
        return userMutable
    }

    fun updateUser(user: UserWithProperties) {
        viewModelScope.launch {
            userSource.upsertUser(user).catch { e ->
                _currentUser.value = UserUiState.Error(e)
            }
                .collect { userUpdated ->
                    if (userUpdated == null)
                        _currentUser.value = UserUiState.Loading
                    else
                        _currentUser.value = UserUiState.Success(user)
                }
        }
    }
}