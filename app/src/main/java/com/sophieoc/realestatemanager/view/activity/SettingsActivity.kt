package com.sophieoc.realestatemanager.view.activity

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.sophieoc.realestatemanager.R
import com.sophieoc.realestatemanager.base.BaseActivity
import com.sophieoc.realestatemanager.databinding.ActivitySettingsBinding
import com.sophieoc.realestatemanager.model.UserWithProperties
import com.sophieoc.realestatemanager.utils.PreferenceHelper
import com.sophieoc.realestatemanager.utils.RC_PERMISSION_PHOTO
import com.sophieoc.realestatemanager.utils.RC_SELECT_PHOTO
import com.sophieoc.realestatemanager.utils.Utils
import com.sophieoc.realestatemanager.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_settings.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class SettingsActivity : BaseActivity() {
    private val userViewModel by viewModel<UserViewModel>()
    private var currentUser: UserWithProperties? = null
    private lateinit var binding: ActivitySettingsBinding
    override fun getLayout() = Pair(null, binding.root)

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        binding.userViewModel = userViewModel
        binding.lifecycleOwner = this
        userViewModel.userUpdated.observe(this, { it ->
            if (it != null) {
                currentUser?.let {
                    Toast.makeText(this, getString(R.string.changes_saved), Toast.LENGTH_LONG).show()
                }
                currentUser = it
            }
        })
        Log.d(TAG, "onCreate: ")
        super.onCreate(savedInstanceState)
    }

    fun editUsername(view: View) {
        binding.editUsernameContainer.visibility = VISIBLE
        showSoftKeyboard(binding.editTextUsername)
        binding.editTextUsername.requestFocus()
        currentUser?.let { binding.editTextUsername.setSelection(it.user.username.length) }
    }

    fun saveUsername(view: View) {
        val editTextUsername = binding.editUsernameContainer.edit_text_username
        if (editTextUsername.text.toString().isNotEmpty()) {
            currentUser?.user?.username = editTextUsername.text.toString()
            binding.editUsernameContainer.visibility = GONE
            hideSoftKeyboard(binding.editTextUsername)
            currentUser?.let { userViewModel.updateUser(it) }
            currentUser = null
        } else
            editTextUsername.error = getString(R.string.empty_field)
    }

    fun cancelUsernameEdit(view: View) {
        binding.editUsernameContainer.visibility = GONE
        binding.editTextUsername.text.clear()
    }

    fun addPhoto(view: View?) {
        if (ActivityCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission.READ_EXTERNAL_STORAGE), RC_PERMISSION_PHOTO)
            return
        }
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        this.startActivityForResult(intent, RC_SELECT_PHOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleResponse(requestCode, resultCode, data)
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == RC_PERMISSION_PHOTO && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            addPhoto(null)
        } else
            Log.d(TAG, "onRequestPermissionsResult: refused")
    }

    private fun handleResponse(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SELECT_PHOTO) {
            if (resultCode == RESULT_OK) {
                data?.let { it -> it.data?.let { saveImage(it) } }
            } else {
                Toast.makeText(this, getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImage(data: Uri) {
        val uuid = UUID.randomUUID().toString()
        val imageRef = FirebaseStorage.getInstance().getReference(uuid)
        if (Utils.isConnectionAvailable(this)) {
            imageRef.putFile(data)
                    .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri: Uri ->
                            val pathImage = uri.toString()
                            currentUser?.user?.urlPhoto = pathImage
                            currentUser?.let { userViewModel.updateUser(it) }
                            currentUser = null
                        }
                    }
            PreferenceHelper.internetAvailable = true
        } else {
            Toast.makeText(this, getString(R.string.load_picture_unable), Toast.LENGTH_LONG).show()
            PreferenceHelper.internetAvailable = false
        }
    }

    // todo: add progress bar
    private fun updateUser(user: UserWithProperties) {

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


    fun onBackPressed(view: View) {
        onBackPressed()
    }

    companion object {
        const val TAG = "SettingsActivityLog"
    }
}
