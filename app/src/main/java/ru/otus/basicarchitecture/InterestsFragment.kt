package ru.otus.basicarchitecture

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import ru.otus.basicarchitecture.databinding.FragmentInterestsBinding

@AndroidEntryPoint
class InterestsFragment : Fragment(R.layout.fragment_interests) {
    private val viewModel: InterestsViewModel by viewModels()
    private var _binding: FragmentInterestsBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentInterestsBinding.bind(view)
        viewModel.interests.forEach { interest ->
            binding.interestsGroup.addView(Chip(requireContext()).apply {
                text = interest
                isCheckable = true
            })
        }
        binding.nextButton.setOnClickListener {
            val selected = (0 until binding.interestsGroup.childCount)
                .mapNotNull { binding.interestsGroup.getChildAt(it) as? Chip }
                .filter { it.isChecked }
                .map { it.text.toString() }
                .toSet()
            viewModel.save(selected)
            parentFragmentManager.commit {
                replace(R.id.fragmentContainer, SummaryFragment())
                addToBackStack(null)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
