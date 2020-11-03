package com.sophieoc.realestatemanager.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.sophieoc.realestatemanager.R
import com.sophieoc.realestatemanager.base.BaseActivity
import com.sophieoc.realestatemanager.utils.PreferenceHelper
import com.sophieoc.realestatemanager.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.activity_login.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginActivity : BaseActivity() {
    val userViewModel by viewModel<UserViewModel>()

    companion object {
        const val TAG = "LoginActivity"
        const val RC_SIGN_IN = 123
    }

    override fun getLayout() = Pair(R.layout.activity_login, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        google_sign_in_btn.setOnClickListener { startSignInWithGoogle()}
    }

    private fun startSignInWithGoogle() {
        if (PreferenceHelper.locationEnabled) {
            startActivityForResult(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(listOf(IdpConfig.GoogleBuilder().build()))
                    .setIsSmartLockEnabled(false, true)
                    .build(), RC_SIGN_IN)
        } else
            Toast.makeText(this, getString(R.string.please_connect_to_internet), LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN && resultCode == RESULT_OK)
            handleResponseAfterSignIn(requestCode, resultCode, data)
    }

    private fun handleResponseAfterSignIn(requestCode: Int, resultCode: Int, data: Intent?) {
        val response: IdpResponse? = IdpResponse.fromResultIntent(data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                userViewModel.currentUser.observe(this, {
                    startNewActivity(MainActivity::class.java)
                    finish()
                })
            } else {
                    if (response?.error?.errorCode == ErrorCodes.NO_NETWORK) {
                        Toast.makeText(this, getString(R.string.internet_unavailable), Toast.LENGTH_SHORT).show()
                    } else if (response?.error?.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                        Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
