package com.sophieoc.realestatemanager.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.sophieoc.realestatemanager.R
import com.sophieoc.realestatemanager.utils.*
import com.sophieoc.realestatemanager.view.fragment.MapFragment
import com.sophieoc.realestatemanager.view.fragment.PropertyDetailFragment
import com.sophieoc.realestatemanager.view.fragment.PropertyListFragment
import com.sophieoc.realestatemanager.view.fragment.UserPropertiesFragment
import com.sophieoc.realestatemanager.viewmodel.MyViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

abstract class BaseActivity : AppCompatActivity() {
    lateinit var locationManager: LocationManager
    lateinit var auth: FirebaseAuth
    val today = Utils.todayDate.toDate()
    val fragmentMap = MapFragment()
    val fragmentList = PropertyListFragment()
    val fragmentUser = UserPropertiesFragment()
    val fragmentPropertyDetail = PropertyDetailFragment()
    val viewModel by viewModel<MyViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        auth = FirebaseAuth.getInstance()
        getLayout().first?.let { setContentView(it)}
        getLayout().second?.let { setContentView(it)}
    }

    override fun onResume() {
        super.onResume()
        checkConnection()
    }

    fun checkConnection() {
        if (!Utils.isConnectionAvailable(this)){
            Snackbar.make(window.decorView.findViewById(android.R.id.content)
                    , getString(R.string.internet_unavailable), LENGTH_SHORT).show()
            PreferenceHelper.internetAvailable = false
        } else
            PreferenceHelper.internetAvailable = true
    }

    abstract fun getLayout(): Pair<Int?, View?>

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun isCurrentUserLogged(): Boolean {
        return getCurrentUser() != null
    }

    fun <T> startNewActivity(activity: Class<T>) {
        val intent = Intent(this, activity)
        startActivity(intent)
    }

    fun configurePropertyDetailFragment() {
        val propertyDetailView = findViewById<View?>(R.id.frame_property_details)
        var fragment = supportFragmentManager.findFragmentById(R.id.frame_property_details)

        if (fragment == null && propertyDetailView != null) {
            fragment = fragmentPropertyDetail
            val fm = supportFragmentManager.beginTransaction()
            fm.add(R.id.frame_property_details, fragment, fragment::class.java.simpleName).addToBackStack(null).commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RQ_CODE_PROPERTY && resultCode == Activity.RESULT_OK && data != null
                && data.hasExtra(PROPERTY_ID))
            intent.putExtra(PROPERTY_ID, data.getStringExtra(PROPERTY_ID))
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}