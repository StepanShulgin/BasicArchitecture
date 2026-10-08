package ru.otus.basicarchitecture

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.otus.basicarchitecture.databinding.FragmentAddressBinding

@AndroidEntryPoint
class AddressFragment : Fragment(R.layout.fragment_address) {
    private val viewModel: AddressViewModel by viewModels()
    private var _binding: FragmentAddressBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAddressBinding.bind(view)
        val suggestionsAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line)
        binding.addressInput.setAdapter(suggestionsAdapter)
        binding.addressInput.threshold = 3
        var applyingSuggestion = false
        var selectedUnrestrictedAddress: String? = null
        binding.addressInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!applyingSuggestion) {
                    selectedUnrestrictedAddress = null
                    viewModel.search(s?.toString().orEmpty())
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        binding.addressInput.setOnItemClickListener { _, _, position, _ ->
            val selectedAddress = suggestionsAdapter.getItem(position).orEmpty()
            selectedUnrestrictedAddress = (viewModel.suggestions.value as? AddressSuggestionState.Success)
                ?.suggestions
                ?.firstOrNull { it.value == selectedAddress }
                ?.unrestricted_value
            applyingSuggestion = true
            binding.addressInput.setText(selectedAddress, false)
            applyingSuggestion = false
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.suggestions.collect { state ->
                    binding.addressProgress.visibility =
                        if (state is AddressSuggestionState.Loading) View.VISIBLE else View.GONE
                    when (state) {
                        AddressSuggestionState.Idle, AddressSuggestionState.Loading -> {
                            suggestionsAdapter.clear()
                            binding.addressStatus.text = ""
                        }
                        is AddressSuggestionState.Error -> binding.addressStatus.text = getString(
                            when (state.reason) {
                                AddressSuggestionError.MISSING_API_KEY -> R.string.dadata_key_missing
                                AddressSuggestionError.NETWORK -> R.string.address_suggestions_error
                            }
                        )
                        is AddressSuggestionState.Success -> {
                            suggestionsAdapter.clear()
                            suggestionsAdapter.addAll(state.suggestions.map(AddressSuggestion::value))
                            binding.addressStatus.text = if (state.suggestions.isEmpty()) {
                                getString(R.string.address_empty)
                            } else {
                                ""
                            }
                            if (state.suggestions.isNotEmpty() && binding.addressInput.hasFocus()) {
                                binding.addressInput.showDropDown()
                            }
                        }
                    }
                }
            }
        }
        binding.nextButton.setOnClickListener {
            viewModel.save(selectedUnrestrictedAddress ?: binding.addressInput.text.toString())
            parentFragmentManager.commit {
                replace(R.id.fragmentContainer, InterestsFragment())
                addToBackStack(null)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
