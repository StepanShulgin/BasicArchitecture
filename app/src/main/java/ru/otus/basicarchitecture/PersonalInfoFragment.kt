package ru.otus.basicarchitecture

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.otus.basicarchitecture.databinding.FragmentPersonalInfoBinding

@AndroidEntryPoint
class PersonalInfoFragment : Fragment(R.layout.fragment_personal_info) {
    private val viewModel: PersonalInfoViewModel by viewModels()
    private var _binding: FragmentPersonalInfoBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPersonalInfoBinding.bind(view)
        binding.birthday.addTextChangedListener(DateMaskWatcher(binding.birthday))
        binding.nextButton.setOnClickListener {
            val valid = viewModel.submit(
                binding.firstName.text.toString(),
                binding.lastName.text.toString(),
                binding.birthday.text.toString()
            )
            if (valid) {
                parentFragmentManager.commit {
                    replace(R.id.fragmentContainer, AddressFragment())
                    addToBackStack(null)
                }
            }
        }
        viewModel.validationError.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private class DateMaskWatcher(private val field: android.widget.EditText) : TextWatcher {
        private var formatting = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(editable: Editable?) {
            if (formatting || editable == null) return
            formatting = true
            val digits = editable.filter(Char::isDigit).take(8)
            val formatted = buildString {
                digits.forEachIndexed { index, digit ->
                    if (index == 2 || index == 4) append('.')
                    append(digit)
                }
            }
            if (editable.toString() != formatted) {
                field.setText(formatted)
                field.setSelection(formatted.length)
            }
            formatting = false
        }
    }
}
