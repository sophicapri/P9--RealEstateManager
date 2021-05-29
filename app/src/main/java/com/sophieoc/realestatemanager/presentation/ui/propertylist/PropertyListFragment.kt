package com.sophieoc.realestatemanager.presentation.ui.propertylist

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.slider.RangeSlider
import com.sophieoc.realestatemanager.R
import com.sophieoc.realestatemanager.databinding.DialogFilterBinding
import com.sophieoc.realestatemanager.databinding.FragmentPropertyListBinding
import com.sophieoc.realestatemanager.model.EntriesFilter
import com.sophieoc.realestatemanager.model.Property
import com.sophieoc.realestatemanager.presentation.BaseActivity
import com.sophieoc.realestatemanager.presentation.ui.PropertyViewModel
import com.sophieoc.realestatemanager.presentation.ui.editproperty.EditAddPropertyActivity
import com.sophieoc.realestatemanager.presentation.ui.filter.FilterViewModel
import com.sophieoc.realestatemanager.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class PropertyListFragment : Fragment(), DatePickerDialog.OnDateSetListener,
    DialogInterface.OnShowListener,
    DialogInterface.OnDismissListener {
    private val propertyViewModel: PropertyViewModel by viewModels()
    private val filterViewModel: FilterViewModel by viewModels()
    private var _binding: FragmentPropertyListBinding? = null
    private val binding: FragmentPropertyListBinding
        get() = _binding!!
    private var _bindingFilter: DialogFilterBinding? = null
    private val bindingFilter: DialogFilterBinding
        get() = _bindingFilter!!
    private lateinit var adapter: PropertyListAdapter
    private lateinit var mainContext: BaseActivity
    private var filterDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainContext = activity as BaseActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPropertyListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureRecyclerView(binding.recyclerViewProperties)
        binding.apply {
            swipeRefreshView.setOnRefreshListener {
                if (resultsSearchLayout.resultsSearchContainer.visibility == VISIBLE) {
                    displayResults()
                } else
                    updatePropertyList()
                swipeRefreshView.isRefreshing = false
            }
            fabAddProperty.setOnClickListener {
                if (Utils.isInternetAvailable(mainContext))
                    mainContext.startNewActivity(EditAddPropertyActivity::class.java)
                else {
                    Toast.makeText(
                        mainContext,
                        getString(R.string.cant_add_property_offline),
                        LENGTH_LONG
                    ).show()
                    PreferenceHelper.internetAvailable = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainContext.checkConnection()
        if (binding.resultsSearchLayout.resultsSearchContainer.visibility == VISIBLE)
            displayResults()
        else
            updatePropertyList()
    }

    private fun updatePropertyList() {
        lifecycleScope.launchWhenStarted {
            propertyViewModel.getProperties().collect { propertyListUiState ->
                when (propertyListUiState) {
                    is PropertyListUiState.Loading -> {/* TODO: showProgressBar() */
                    }
                    is PropertyListUiState.Empty -> binding.noPropertiesInDb.visibility = VISIBLE
                    is PropertyListUiState.Error -> {/* TODO: handleError */
                    }
                    is PropertyListUiState.Success -> {
                        adapter.updateList(ArrayList(propertyListUiState.propertyList))
                        binding.noPropertiesInDb.visibility = GONE
                    }
                }
            }
        }
    }

    private fun configureRecyclerView(recyclerView: RecyclerView) {
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PropertyListAdapter(getListener())
        recyclerView.adapter = adapter
    }

    private fun getListener() = object : PropertyListAdapter.OnPropertyClickListener {
        override fun onPropertyClick(propertyId: String) {
            mainContext.onPropertyClick(propertyId)
        }
    }

    private fun updateList(filteredList: ArrayList<Property>) {
        adapter.updateList(filteredList)
    }

    private fun resetFilter() {
        updatePropertyList()
    }

    @SuppressLint("InflateParams")
    fun showFilterDialog() {
        val alertBuilder = AlertDialog.Builder(requireContext(), R.style.Dialog)
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.title_filter_dialog, null)
        filterViewModel.entries = EntriesFilter()
        _bindingFilter = DataBindingUtil.inflate(inflater, R.layout.dialog_filter, null, false)
        bindFilterViews()
        alertBuilder.setCustomTitle(view)
            .setView(bindingFilter.root)
            .setPositiveButton(getString(R.string.ok_btn), null)
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener(this)
        filterDialog = alertBuilder.create()
        filterDialog?.setOnShowListener(this)
        filterDialog?.show()
    }

    private fun bindFilterViews() {
        bindingFilter.selectDate.setOnClickListener { showDatePickerDialog() }
        bindingFilter.btnDeleteDate.setOnClickListener {
            bindingFilter.selectDate.text = getString(R.string.click_to_select_a_date)
            filterViewModel.entries.dateOnMarket = null
            filterViewModel.entries.dateSold = null
            bindingFilter.btnDeleteDate.visibility = GONE
        }
        bindingFilter.rangeSliderPrice.addOnChangeListener(getPriceSliderListener())
        lifecycleScope.launchWhenStarted {
            filterViewModel.getPriceOfPriciestProperty().collect { price ->
                bindingFilter.rangeSliderPrice.valueFrom = 0f
                bindingFilter.rangeSliderPrice.valueTo = price.toFloat()
                bindingFilter.rangeSliderPrice.values = arrayListOf(0.0f, price.toFloat() / 2)
                bindingFilter.rangeSliderPrice.stepSize = price.toFloat() / STEP_SIZE_PRICE
            }

            bindingFilter.rangeSliderSurface.addOnChangeListener(getSurfaceSliderListener())
            filterViewModel.getSurfaceOfBiggestProperty().collect {  surface ->
                bindingFilter.rangeSliderSurface.valueFrom = 0f
                bindingFilter.rangeSliderSurface.valueTo = surface.toFloat()
                bindingFilter.rangeSliderSurface.values = arrayListOf(0.0f, surface.toFloat() / 2)
            }
        }
        bindingFilter.minPrice.text = getString(
            R.string.dollar_value,
            bindingFilter.rangeSliderPrice.values.first().toInt().formatToDollarsOrMeters()
        )
        bindingFilter.maxPrice.text = getString(
            R.string.dollar_value,
            bindingFilter.rangeSliderPrice.values.last().toInt().formatToDollarsOrMeters()
        )
        bindingFilter.minSurface.text =
            getString(R.string.sqft_value, bindingFilter.rangeSliderSurface.values.first().toInt())
        bindingFilter.maxSurface.text =
            getString(R.string.sqft_value, bindingFilter.rangeSliderSurface.values.last().toInt())
        bindingFilter.nbrOfPicInput.addTextChangedListener(getTextWatcher())
    }

    private fun getPriceSliderListener() = RangeSlider.OnChangeListener { slider, value, _ ->
        if (slider.activeThumbIndex == 0)
            bindingFilter.minPrice.text =
                getString(R.string.dollar_value, value.toInt().formatToDollarsOrMeters())
        else
            bindingFilter.maxPrice.text =
                getString(R.string.dollar_value, value.toInt().formatToDollarsOrMeters())
    }

    private fun getSurfaceSliderListener() = RangeSlider.OnChangeListener { slider, value, _ ->
        if (slider.activeThumbIndex == 0)
            bindingFilter.minSurface.text = getString(R.string.sqft_value, value.toInt())
        else
            bindingFilter.maxSurface.text = getString(R.string.sqft_value, value.toInt())
    }

    private fun getTextWatcher() = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (!s.isNullOrBlank())
                bindingFilter.checkboxPictures.isChecked = true
        }
    }

    private fun startSearch() {
        getEntries()
        displayResultsText()
        filterViewModel.startSearch()
        displayResults()
    }

    private fun getEntries() {
        val chipType = filterDialog?.findViewById<Chip>(bindingFilter.typeChipGroup.checkedChipId)
        chipType?.let { filterViewModel.entries.propertyType = it.text.toString() }
        if (bindingFilter.nbrOfBedsInput.text.toString().isNotEmpty())
            filterViewModel.entries.nbrOfBed = bindingFilter.nbrOfBedsInput.text.toString().toInt()
        if (bindingFilter.nbrOfBathInput.text.toString().isNotEmpty())
            filterViewModel.entries.nbrOfBath = bindingFilter.nbrOfBathInput.text.toString().toInt()
        if (bindingFilter.nbrOfRoomsInput.text.toString().isNotEmpty())
            filterViewModel.entries.nbrOfRoom =
                bindingFilter.nbrOfRoomsInput.text.toString().toInt()
        val chipAvailability =
            filterDialog?.findViewById<Chip>(bindingFilter.availabilityChipGroup.checkedChipId)
        chipAvailability?.let {
            if (it.id == R.id.for_sale) filterViewModel.entries.propertyAvailability =
                PropertyAvailability.AVAILABLE.toString()
            else filterViewModel.entries.propertyAvailability = it.text.toString()
        }
        if (bindingFilter.areaInput.text.toString().isNotEmpty())
            filterViewModel.entries.area = bindingFilter.areaInput.text.toString().trim()
        filterViewModel.entries.priceMin = bindingFilter.rangeSliderPrice.values.first().toInt()
        filterViewModel.entries.priceMax = bindingFilter.rangeSliderPrice.values.last().toInt()
        filterViewModel.entries.surfaceMin = bindingFilter.rangeSliderSurface.values.first().toInt()
        filterViewModel.entries.surfaceMax = bindingFilter.rangeSliderSurface.values.last().toInt()
        if (bindingFilter.checkboxPictures.isChecked)
            filterViewModel.entries.nbrOfPictures = MINIMUM_PICTURES
        if (bindingFilter.nbrOfPicInput.text.toString().isNotEmpty())
            filterViewModel.entries.nbrOfPictures =
                bindingFilter.nbrOfPicInput.text.toString().trim().toInt()
    }

    private fun displayResultsText() {
        binding.apply {
            resultsSearchLayout.apply {
                resultsSearchContainer.visibility = VISIBLE
                dataSearched.text = getTextToDisplay()
                btnResetSearch.setOnClickListener {
                    resetFilter()
                    noPropertiesFound.visibility = GONE
                    resultsSearchContainer.visibility = GONE
                }
            }
        }
    }

    private fun displayResults() {
        lifecycleScope.launchWhenStarted {
            filterViewModel.resultSearch.collect { propertyListUiState ->
                when (propertyListUiState) {
                    is PropertyListUiState.Loading -> {/*TODO: show progressBar*/
                    }
                    is PropertyListUiState.Empty -> {
                        binding.noPropertiesFound.visibility = VISIBLE
                    }
                    is PropertyListUiState.Success -> {
                        updateList(ArrayList(propertyListUiState.propertyList))
                        binding.noPropertiesFound.visibility = GONE
                    }
                    is PropertyListUiState.Error -> {/* TODO: handleError */ }
                }
                filterDialog?.dismiss()
            }
        }
    }

    override fun onShow(dialogInterface: DialogInterface?) {
        if (filterDialog != null) {
            val positiveButton = filterDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton?.setOnClickListener { startSearch() }
            val negativeButton = filterDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton?.setOnClickListener { filterDialog?.dismiss() }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (dialog === filterDialog) {
            filterDialog = null
        }
    }

    private fun showDatePickerDialog() {
        Locale.setDefault(Locale.US)
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            this, Calendar.getInstance()[Calendar.YEAR],
            Calendar.getInstance()[Calendar.MONTH],
            Calendar.getInstance()[Calendar.DAY_OF_MONTH]
        )
        datePickerDialog.show()
        val okButton = datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        okButton.id = R.id.calendar_ok_button
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        val selectedDate = GregorianCalendar(year, month, dayOfMonth).time
        bindingFilter.selectDate.text = df.format(selectedDate)
        bindingFilter.selectDate.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.colorPrimaryLight
            )
        )
        bindingFilter.btnDeleteDate.visibility = VISIBLE
        val chip =
            filterDialog?.findViewById<Chip>(bindingFilter.availabilityChipGroup.checkedChipId)
        if (chip != null) {
            if (chip.id == R.id.for_sale) {
                filterViewModel.entries.dateOnMarket = selectedDate
            } else
                filterViewModel.entries.dateSold = selectedDate
        } else {
            filterViewModel.entries.dateOnMarket = selectedDate
            bindingFilter.availabilityChipGroup.check(R.id.for_sale)
        }
    }

    private fun getTextToDisplay(): String {
        val entries = filterViewModel.entries
        var msg = ""
        entries.propertyType?.let { msg += "$it - " }
        entries.nbrOfRoom?.let {
            msg += resources.getQuantityString(R.plurals.nbr_of_room_filter, it, it)
        }
        entries.nbrOfBed?.let {
            msg +=  resources.getQuantityString(R.plurals.nbr_of_bed_filter, it, it)
        }
        entries.nbrOfBath?.let {
            msg +=  resources.getQuantityString(R.plurals.nbr_of_bath_filter, it, it)
        }
        entries.propertyAvailability?.let {
            msg += if (it == PropertyAvailability.AVAILABLE.name) "${PropertyAvailability.AVAILABLE.s} "
            else "${PropertyAvailability.SOLD.s} "
            entries.dateOnMarket?.let { date ->
                msg += getString(
                    R.string.since_date,
                    date.toStringFormat()
                )
            }
            entries.dateSold?.let { date ->
                msg += getString(
                    R.string.since_date,
                    date.toStringFormat()
                )
            }
            msg += "- "
        }
        entries.area?.let { msg += "$it - " }
        if (entries.priceMin != null && entries.priceMax != null) {
            msg += getString(
                R.string.price_between,
                entries.priceMin?.formatToDollarsOrMeters(),
                entries.priceMax?.formatToDollarsOrMeters()
            )
        }
        if (entries.surfaceMin != null && entries.surfaceMax != null) {
            msg += getString(R.string.sqft_between, entries.surfaceMin, entries.surfaceMax)
        }
        entries.nbrOfPictures?.let {
            msg +=  resources.getQuantityString(R.plurals.with_x_pictures, it, it)
        }
        return msg
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _bindingFilter = null
    }
}