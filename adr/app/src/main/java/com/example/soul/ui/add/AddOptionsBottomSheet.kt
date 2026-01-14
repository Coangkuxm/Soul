package com.example.soul.ui.add

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.soul.databinding.DialogAddOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddOptionsBottomSheet(
    private val onAddCollection: () -> Unit,
    private val onAddItem: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogAddOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View {
        _binding = DialogAddOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionAddCollection.setOnClickListener {
            dismiss()
            onAddCollection()
        }

        binding.optionAddItem.setOnClickListener {
            dismiss()
            onAddItem()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddOptionsBottomSheet"
    }
}
