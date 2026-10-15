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
            val chip = layoutInflater.inflate(R.layout.item_interest, binding.interestsGroup, false) as Chip
            binding.interestsGroup.addView(chip.apply {
                text = interest
                isCheckable = true
                isChecked = interest in viewModel.selectedInterests
            })
        }
        binding.nextButton.setOnClickListener {
            saveSelection()
            parentFragmentManager.commit {
                replace(R.id.fragmentContainer, SummaryFragment())
                addToBackStack(null)
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val selected = viewModel.selectedInterests
        for (index in 0 until binding.interestsGroup.childCount) {
            (binding.interestsGroup.getChildAt(index) as? Chip)?.apply {
                isChecked = text.toString() in selected
                setOnCheckedChangeListener { _, _ -> saveSelection() }
            }
        }
    }

    private fun saveSelection() {
        viewModel.save((0 until binding.interestsGroup.childCount)
            .mapNotNull { binding.interestsGroup.getChildAt(it) as? Chip }
            .filter { it.isChecked }.map { it.text.toString() }.toSet())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
