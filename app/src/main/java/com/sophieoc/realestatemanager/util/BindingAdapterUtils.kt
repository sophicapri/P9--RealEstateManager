package com.sophieoc.realestatemanager.util

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sophieoc.realestatemanager.R
import com.sophieoc.realestatemanager.model.Photo


@BindingAdapter("imageUrl")
fun ImageView.bindImageUrl(imageUrl: String?) {
    val url = if (imageUrl != null && imageUrl.isNotEmpty())
        imageUrl
    else if (this.id == R.id.profile_picture)
        NO_IMAGE_AVAILABLE_USER
    else
        NO_IMAGE_AVAILABLE_PROPERTY

    Glide.with(this.context)
            .load(url)
            .apply(RequestOptions().circleCrop())
            .into(this)
}

@BindingAdapter("propertyImageUrl")
fun ImageView.bindPropertyImage(photo: Photo?) {
    val url = if (photo != null) {
        if (photo.urlPhoto.isNotEmpty())
            photo.urlPhoto
        else
            NO_IMAGE_AVAILABLE_PROPERTY
    } else
        NO_IMAGE_AVAILABLE_PROPERTY
    Glide.with(this.context)
            .load(url)
            .apply(RequestOptions().centerCrop())
            .into(this)
}

@BindingAdapter("dollarFormat")
fun TextView.formatIntToDollar(price: Int) {
    this.text = price.formatToDollarsOrMeters()
}

/*@BindingAdapter("adapter")
fun ViewPager2.bindAdapter(photos : List<Photo>?){
    photos?.let {
        val listPhotos = if (photos.isNotEmpty()) photos
        else
            arrayListOf(Photo("", ""))
        this.adapter = SliderAdapter(listPhotos)
    }
}*/

@BindingAdapter("checkUser", "withCurrentUser")
fun FloatingActionButton.setVisibility(userId: String?, currentUserId : String?){
    if (userId != null && currentUserId != null)
        this.visibility = if (userId == currentUserId) View.VISIBLE else View.GONE
}

@BindingAdapter("emptyIfNegativeValue")
fun EditText.setText(text: String?) {
    if (text == "-1") this.setText("") else this.setText(text)
}

@InverseBindingAdapter(attribute = "emptyIfNegativeValue", event = "android:textAttrChanged")
fun EditText.getText(): String {
    return if (this.text.toString().isEmpty())
        "-1"
    else
        this.text.toString()
}
