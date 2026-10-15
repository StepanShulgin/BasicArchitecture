package ru.otus.basicarchitecture

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class AddressInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.autoCompleteTextViewStyle,
) : MaterialAutoCompleteTextView(context, attrs, defStyleAttr) {
    // DaData performs fuzzy search remotely. Local ArrayAdapter filtering can discard its
    // results or finish after the response and close a freshly opened dropdown.
    override fun performFiltering(text: CharSequence?, keyCode: Int) = Unit
}
