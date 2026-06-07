package com.example.soul.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.soul.databinding.FragmentPlaceholderBinding

class PlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = requireArguments().getString(ARG_TITLE, "Sắp ra mắt")
        binding.tvSubtitle.text = requireArguments().getString(ARG_SUBTITLE, "")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_SUBTITLE = "arg_subtitle"

        fun newInstance(title: String, subtitle: String): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_SUBTITLE, subtitle)
            }
            return fragment
        }
    }
}
