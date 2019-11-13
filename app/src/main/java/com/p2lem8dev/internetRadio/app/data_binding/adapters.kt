package com.p2lem8dev.internetRadio.app.data_binding

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide


@BindingAdapter("app:image_url")
fun setImageViewByURL(imageView: ImageView, imageUrl: String?) {
    Glide.with(imageView.context)
        .load(imageUrl)
        .centerCrop()
        .circleCrop()
        .into(imageView)
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

@BindingAdapter(
    value = ["app:formatter_country", "app:formatter_region", "app:formatter_city"],
    requireAll = true
)
fun setTextViewByLocation(textView: TextView, country: String?, region: String?, city: String?) {
    textView.text = "$country - $region - $city"
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