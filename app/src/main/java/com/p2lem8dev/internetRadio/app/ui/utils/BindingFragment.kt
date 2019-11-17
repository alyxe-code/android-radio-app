package com.p2lem8dev.internetRadio.app.ui.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

open class BindingFragment<TBinding : ViewDataBinding>(private val layoutId: Int) : Fragment() {

    protected lateinit var binding: TBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate<TBinding>(
            inflater, layoutId, container, false
        )
        return binding.root
    }

}