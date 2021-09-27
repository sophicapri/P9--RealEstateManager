package com.sophieoc.realestatemanager.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.sophieoc.realestatemanager.R
import com.sophieoc.realestatemanager.databinding.ActivitySettingsBinding
import com.sophieoc.realestatemanager.model.UserWithProperties
import com.sophieoc.realestatemanager.ui.BaseActivity
import com.sophieoc.realestatemanager.ui.user.UserUiState
import com.sophieoc.realestatemanager.ui.user.UserViewModel
import com.sophieoc.realestatemanager.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.io.File
import java.util.*

@AndroidEntryPoint
class SettingsActivity : BaseActivity(), AddPicturesFromPhoneUtil.OnActivityResultListener {
    private val userViewModel by viewModels<UserViewModel>()
    private lateinit var currentUser: UserWithProperties
    private lateinit var binding: ActivitySettingsBinding
    private var dataChanged = false
    private lateinit var addPhotoUtil : AddPicturesFromPhoneUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // init AddPhotoUtil to init all the ActivityResultLaunchers
        addPhotoUtil = AddPicturesFromPhoneUtil(this, this)
        lifecycleScope.launchWhenStarted {
            userViewModel.currentUser.collect { userUiState ->
                bindViews(userUiState)
            }
        }
    }

    private fun bindViews(userUiState: UserUiState) {
        when (userUiState) {
            is UserUiState.Success -> {
                currentUser = userUiState.userWithProperties
                binding.apply {
                    user = userUiState.userWithProperties.user
                    lifecycleOwner = this@SettingsActivity
                    activity = this@SettingsActivity
                    toolbar.setNavigationOnClickListener { onBackPressed() }
                }
                binding.progressBar.visibility = GONE
                if (dataChanged) {
                    Toast.makeText(this@SettingsActivity, getString(R.string.changes_saved),
                        Toast.LENGTH_LONG).show()
                    dataChanged = false
                }
            }
            is UserUiState.Error -> {/*handleError(userUiState.exception) */}
            is UserUiState.Loading -> {/* TODO: */}
        }
    }

    override fun getLayout(): View {
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    fun editUsername() {
        checkConnection()
        if (PreferenceHelper.internetAvailable) {
            binding.editUsernameContainer.visibility = VISIBLE
            showSoftKeyboard(binding.editTextUsername)
            binding.editTextUsername.requestFocus()
            binding.editTextUsername.setSelection(currentUser.user.username.length)
            binding.editTextUsername.setOnKeyListener { _, keyCode, keyEvent ->
                if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    saveUsername()
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }
        } else
            Toast.makeText(this, getString(R.string.cannot_edit_username_offline), Toast.LENGTH_LONG).show()
    }

    fun saveUsername() {
        binding.apply {
            if (editTextUsername.text.toString().isNotEmpty()) {
                progressBar.visibility = VISIBLE
                currentUser.user.username = editTextUsername.text.toString()
                editUsernameContainer.visibility = GONE
                hideSoftKeyboard(editTextUsername)
                dataChanged = true
                userViewModel.updateUser(currentUser)
            } else
                editTextUsername.error = getString(R.string.empty_field)
        }
    }

    fun cancelUsernameEdit() {
        binding.editUsernameContainer.visibility = GONE
    }

    fun addPhoto() {
        checkConnection()
        if (PreferenceHelper.internetAvailable) {
            addPhotoUtil.addPhoto()
        } else
            Toast.makeText(this, getString(R.string.cannot_change_photo_offline), Toast.LENGTH_LONG).show()
    }


    override fun onActivityResult(requestCode: Int, activityResult: ActivityResult) {
        addPhotoUtil.bottomSheetDialog.dismiss()
        if (requestCode == RC_SELECT_PHOTO_GALLERY)
            handleResponseGallery(activityResult.resultCode, activityResult.data)
        else if (requestCode == RC_PHOTO_CAMERA)
            handleResponseCamera(activityResult.resultCode)
    }

    private fun handleResponseGallery(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK)
            data?.let { it -> it.data?.let { saveImage(it) } }
        else
            Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
    }

    private fun handleResponseCamera(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            val f = File(addPhotoUtil.currentPhotoPath)
            saveImage(Uri.fromFile(f))
        } else
            Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
    }

    private fun saveImage(data: Uri) {
        val uuid = UUID.randomUUID().toString()
        val imageRef = FirebaseStorage.getInstance().getReference(uuid)
        if (Utils.isInternetAvailable(this)) {
            binding.progressBar.visibility = VISIBLE
            imageRef.putFile(data)
                    .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri: Uri ->
                            val pathImage = uri.toString()
                            currentUser.user.urlPhoto = pathImage
                            dataChanged = true
                            userViewModel.updateUser(currentUser)
                        }
                    }
            PreferenceHelper.internetAvailable = true
        } else {
            Toast.makeText(this, getString(R.string.load_picture_unable), Toast.LENGTH_LONG).show()
            PreferenceHelper.internetAvailable = false
        }
    }

    private fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideSoftKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    companion object {
        const val TAG = "SettingsActivityLog"
    }
}
