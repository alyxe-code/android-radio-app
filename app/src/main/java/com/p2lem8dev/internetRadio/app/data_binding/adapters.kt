package com.p2lem8dev.internetRadio.app.data_binding

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.lang.Exception


@BindingAdapter("app:image_url")
fun setImageViewByURL(imageView: ImageView, imageUrl: String?) {
    try {
        Glide.with(imageView.context)
            .load(imageUrl)
            .apply(RequestOptions().apply {
                transform(FitCenter())
                transform(RoundedCorners(16))
            })
            .into(imageView)
    } catch (e: Exception) {
        Log.wtf(
            "VIEW_LOAD_IMAGE", "${e.message}" +
                    e.stackTrace.joinToString("\n")
        )
    }
}

@BindingAdapter("app:num_value")
fun setTextViewFromInt(textView: TextView, num: Number) {
    textView.text = num.toString()
}

@BindingAdapter("app:visibility_true")
fun setViewVisibleOnTrue(view: View, condition: Boolean) {
    view.visibility = if (condition) View.VISIBLE else View.GONE
}

@BindingAdapter("app:visibility_false")
fun setViewVisibleOnFalse(view: View, condition: Boolean) {
    view.visibility = if (condition) View.GONE else View.VISIBLE
}

@BindingAdapter("app:formatter_amount")
fun setTextViewByListSize(textView: TextView, amount: Int) {
    textView.text = "${amount} items shown"
}

@BindingAdapter(value = ["app:progress_total", "app:progress_current"], requireAll = true)
fun setTextViewProgress(textView: TextView, progressTotal: Int, progressCurrent: Int) {
    var current = progressCurrent
    if (current > progressTotal) {
        current = progressTotal
    }
    textView.text = if (current == progressTotal && current != 0) "Finishing..."
    else "$current / $progressTotal"
}

@BindingAdapter(value = ["app:country", "app:region", "app:city"], requireAll = true)
fun setTextByLocation(textView: TextView, country: String?, region: String?, city: String?) {
    val array = arrayListOf<String>()
    if (country != null) array.add(country)

    if (region != null && city != null && region.contains(city)) {
        array.add(city)
    } else {
        if (region != null) array.add(region)
        if (city != null) array.add(city)
    }
    setTextByArray(textView, array)
}

@BindingAdapter("app:text_array")
fun setTextByArray(textView: TextView, text_array: List<String>?) {
    textView.text = text_array?.joinToString(" - ") ?: ""
}