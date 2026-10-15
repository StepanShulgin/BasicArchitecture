package ru.otus.basicarchitecture

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.otus.basicarchitecture.databinding.FragmentSummaryBinding

@AndroidEntryPoint
class SummaryFragment : Fragment(R.layout.fragment_summary) {
    private val viewModel: SummaryViewModel by viewModels()
    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSummaryBinding.bind(view)
        val profile = viewModel.profile
        binding.summary.text = getString(
            R.string.profile_summary,
            profile.firstName,
            profile.lastName,
            profile.birthday,
            profile.country,
            profile.city,
            profile.address,
            profile.interests.joinToString().ifEmpty { "—" }
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
