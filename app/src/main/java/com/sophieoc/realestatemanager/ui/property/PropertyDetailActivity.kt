package com.sophieoc.realestatemanager.ui.property

import android.content.Intent
import android.os.Bundle
import com.sophieoc.realestatemanager.R
import com.sophieoc.realestatemanager.databinding.ActivityPropertyDetailBinding
import com.sophieoc.realestatemanager.ui.BaseActivity
import com.sophieoc.realestatemanager.util.PROPERTY_ID
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PropertyDetailActivity : BaseActivity() {
    private lateinit var binding : ActivityPropertyDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityPropertyDetailBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
    }

    override fun getLayout() = binding.root

    override fun onResume() {
        super.onResume()
        supportFragmentManager.beginTransaction()
                .replace(R.id.frame_property_details, fragmentPropertyDetail,
                        fragmentPropertyDetail::class.java.simpleName).commit()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (intent.hasExtra(PROPERTY_ID)) {
            val propertyId = intent.extras?.get(PROPERTY_ID) as String
            setResult(RESULT_OK, Intent().putExtra(PROPERTY_ID, propertyId))
        }
        finish()
    }
}