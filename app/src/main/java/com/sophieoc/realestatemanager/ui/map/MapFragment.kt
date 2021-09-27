package com.sophieoc.realestatemanager.ui.map

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.sophieoc.realestatemanager.R
import com.sophieoc.realestatemanager.databinding.FragmentMapBinding
import com.sophieoc.realestatemanager.model.Property
import com.sophieoc.realestatemanager.ui.BaseActivity
import com.sophieoc.realestatemanager.ui.property.PropertyDetailActivity
import com.sophieoc.realestatemanager.ui.property.PropertyDetailFragment
import com.sophieoc.realestatemanager.ui.property.PropertyViewModel
import com.sophieoc.realestatemanager.ui.propertylist.PropertyListUiState
import com.sophieoc.realestatemanager.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback {
    private var map: GoogleMap? = null
    private var propertyDetailView: View? = null
    private lateinit var currentLocation: Location
    private var fragmentNotRestarted = true
    private var updateView = false
    private var _binding: FragmentMapBinding? = null
    private val binding: FragmentMapBinding
        get() = _binding!!
    private lateinit var progressBar: ProgressBar
    val propertyViewModel: PropertyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initMap()
        propertyDetailView = activity?.findViewById(R.id.frame_property_details)
        progressBar = binding.progressBar
        binding.apply {
            refocusBtn.setOnClickListener {
                if (PreferenceHelper.locationEnabled) {
                    fetchLastLocation()
                } else
                    (requireActivity() as BaseActivity).checkLocationEnabled()
            }
            progressBar.visibility = VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        handleMapSize()
        if (fragmentNotRestarted && getLocationFromIntent() == null)
            fetchLastLocation()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.setOnInfoWindowClickListener { marker: Marker? ->
            if (marker != null) startPropertyDetail(marker)
        }
        map = googleMap
        if (requireActivity().intent.hasExtra(LATITUDE_PROPERTY) && requireActivity().intent.hasExtra(
                LONGITUDE_PROPERTY
            )
        )
            getLocationFromIntent()?.let { it ->
                focusMap(it)
                binding.progressBar.visibility = GONE
            }
        initMarkers()
        if (PreferenceHelper.locationEnabled)
            map?.isMyLocationEnabled = true
        else
            binding.progressBar.visibility = GONE
    }

    private fun startPropertyDetail(marker: Marker) {
        if (propertyDetailView == null) {
            val intent = Intent(requireContext(), PropertyDetailActivity::class.java)
            intent.putExtra(PROPERTY_ID, marker.tag?.toString())
            (requireActivity() as MapActivity).startDetailActivityForResult.launch(intent)
        } else {
            propertyDetailView?.visibility = VISIBLE
            binding.btnMapSize.text = getString(R.string.fullscreen)
            val bundle = Bundle()
            bundle.putString(PROPERTY_ID, marker.tag?.toString())
            val propertyDetailFragment = PropertyDetailFragment()
            propertyDetailFragment.arguments = bundle
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_property_details, propertyDetailFragment).commit()
        }
    }

    private fun handleMapSize() {
        binding.apply {
            if (propertyDetailView?.visibility == VISIBLE) {
                btnMapSize.visibility = VISIBLE
                btnMapSize.setOnClickListener {
                    if (propertyDetailView?.visibility == VISIBLE) {
                        propertyDetailView?.visibility = GONE
                        btnMapSize.text = getString(R.string.reduce_map)
                    } else {
                        propertyDetailView?.visibility = VISIBLE
                        btnMapSize.text = getString(R.string.fullscreen)
                    }
                }
            } else if (propertyDetailView == null)
                btnMapSize.visibility = GONE
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchLastLocation() {
        if (PreferenceHelper.locationEnabled) {
            map?.isMyLocationEnabled = true
            val fusedLocationProviderClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(requireActivity() as MapActivity)
            val task: Task<Location?> = fusedLocationProviderClient.lastLocation
            task.addOnCompleteListener { getLocationTask: Task<Location?> ->
                if (getLocationTask.isSuccessful) {
                    val currentLocation = getLocationTask.result
                    if (currentLocation != null && currentLocation.longitude != 0.0 && currentLocation.longitude != 0.0) {
                        this.currentLocation = currentLocation
                        focusMap(currentLocation)
                        progressBar.visibility = GONE
                    } else {
                        // -> update view after location enabled
                        requireActivity().supportFragmentManager.beginTransaction().detach(this)
                            .attach(this).commit()
                        updateView = true
                    }
                } else {
                    Toast.makeText(activity, R.string.cant_get_location, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun focusMap(currentLocation: Location) {
        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5F))
    }

    private fun initMap() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_container) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun initMarkers() {
        lifecycleScope.launchWhenStarted {
            propertyViewModel.getProperties().collect { propertyListUiState ->
                when (propertyListUiState) {
                    is PropertyListUiState.Loading -> {/*TODO: */}
                    is PropertyListUiState.Success -> {
                        for (property in propertyListUiState.propertyList) {
                            if (property.address.toString().isNotEmpty())
                                pinPropertyOnMap(property)
                        }
                    }
                    is PropertyListUiState.Empty -> {/*TODO: */}
                    is PropertyListUiState.Error -> {/*TODO: */}
                }
            }
        }
    }

    private fun pinPropertyOnMap(property: Property) {
        val latLng = property.address.toLatLng(requireContext())
        if (latLng.toStringFormat() != LAT_LNG_NOT_FOUND) {
            val marker: Marker? = map?.addMarker(
                MarkerOptions().title(property.type.toString())
                    .position(latLng)
                    .icon(R.drawable.ic_baseline_house_24.toBitmap(resources))
            )
            marker?.tag = property.id
        }
    }

    private fun getLocationFromIntent(): Location? {
        val extras = requireActivity().intent.extras
        val lat = extras?.get(LATITUDE_PROPERTY) as Double?
        val lng = extras?.get(LONGITUDE_PROPERTY) as Double?
        val location = Location("")
        lat?.let { location.latitude = it }
        lng?.let { location.longitude = it }
        lat?.let { return location }
        return null
    }

    override fun onStop() {
        super.onStop()
        fragmentNotRestarted = false
        if (!PreferenceHelper.locationEnabled || updateView)
            fragmentNotRestarted = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentNotRestarted = true
        updateView = false
    }
}