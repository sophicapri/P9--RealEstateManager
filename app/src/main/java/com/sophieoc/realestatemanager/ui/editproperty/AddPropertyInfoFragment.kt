package com.sophieoc.realestatemanager.ui.editproperty

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.DatePicker
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sophieoc.realestatemanager.R
import com.sophieoc.realestatemanager.databinding.FragmentAddInfoBinding
import com.sophieoc.realestatemanager.ui.editproperty.EditAddPropertyFragment.Companion.emptyFieldsInMainInfo
import com.sophieoc.realestatemanager.ui.property.PropertyViewModel
import com.sophieoc.realestatemanager.util.PropertyAvailability
import com.sophieoc.realestatemanager.util.PropertyType
import com.sophieoc.realestatemanager.util.toStringFormat
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.*

@AndroidEntryPoint
class AddPropertyInfoFragment : Fragment(), DatePickerDialog.OnDateSetListener {
    private var _binding: FragmentAddInfoBinding? = null
    private val binding: FragmentAddInfoBinding
        get() = _binding!!
    private val sharedViewModel by activityViewModels<PropertyViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_info, container, false)
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            propertyViewModel = sharedViewModel
            executePendingBindings()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        bindViews()
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
        binding.apply {
            btnDate.text = df.format(selectedDate)
            errorDate.visibility = GONE
            btnDate.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.colorPrimaryLight
                )
            )
            if (sharedViewModel.property.availability == PropertyAvailability.AVAILABLE) {
                sharedViewModel.property.dateOnMarket = selectedDate
                sharedViewModel.property.dateSold = null
            } else {
                sharedViewModel.property.dateSold = selectedDate
            }
        }
    }

    private fun bindViews() {
        val property = sharedViewModel.property
        binding.apply {
            typesSpinner.setSelection(getSpinnerPosition(property.type.s, R.array.property_types))
            typesSpinner.onItemSelectedListener = getOnTypeSelectedListener()
            if (requireActivity().intent.extras != null) {
                availabilitySpinner.setSelection(
                    getSpinnerPosition(
                        property.availability.s,
                        R.array.property_availability
                    )
                )
                availabilitySpinner.onItemSelectedListener = getOnAvailabilitySelectedListener()
                forSaleTextView.visibility = GONE
            } else {
                property.availability = PropertyAvailability.AVAILABLE
                forSaleTextView.visibility = VISIBLE
                availabilitySpinner.visibility = INVISIBLE
            }
            btnDate.setOnClickListener { showDatePickerDialog() }
            btnDate.text = getString(R.string.click_to_select_a_date)
            property.dateSold?.let { btnDate.text = it.toStringFormat() }
            property.dateOnMarket?.let { btnDate.text = it.toStringFormat() }
        }
    }

    private fun getSpinnerPosition(value: String, array: Int): Int {
        val arrayList = resources.getStringArray(array)
        var position = -1
        arrayList.forEachIndexed { index, s -> if (s == value) position = index }
        return position
    }

    private fun getOnAvailabilitySelectedListener() = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            sharedViewModel.property.availability = PropertyAvailability.values()[position]
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private fun getOnTypeSelectedListener() = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            sharedViewModel.property.type = PropertyType.values()[position]
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    fun checkMainInfoPage(): Boolean {
        emptyFieldsInMainInfo = false
        try {
            binding.apply {
                sharedViewModel.apply {
                    if (property.price <= 0 || property.surface <= 0 || property.numberOfRooms <= 0 ||
                        (property.dateSold == null && property.dateOnMarket == null)
                    ) {
                        if (property.price <= 0)
                            priceInput.error = getString(R.string.empty_field)
                        if (property.surface <= 0)
                            surfaceInput.error = getString(R.string.empty_field)
                        if (property.numberOfRooms <= 0)
                            nbrOfRoomsInput.error = getString(R.string.empty_field)
                        if (property.dateSold == null && property.dateOnMarket == null) {
                            errorDate.visibility = VISIBLE
                            errorDate.text = getString(R.string.please_select_a_date)
                        }
                        emptyFieldsInMainInfo = true
                        return false
                    }
                }
            }
        } catch (e: NullPointerException) {
            Log.d(EditAddPropertyFragment.TAG, "checkMainInfoPage: ${e.stackTrace}")
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddInfoFragment"
    }
}
