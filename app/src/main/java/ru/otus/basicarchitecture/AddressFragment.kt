package ru.otus.basicarchitecture

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.otus.basicarchitecture.databinding.FragmentAddressBinding

@AndroidEntryPoint
class AddressFragment : Fragment(R.layout.fragment_address) {
    private val viewModel: AddressViewModel by viewModels()
    private var _binding: FragmentAddressBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAddressBinding.bind(view)
        binding.nextButton.setOnClickListener {
            viewModel.save(
                binding.country.text.toString(),
                binding.city.text.toString(),
                binding.address.text.toString()
            )
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
