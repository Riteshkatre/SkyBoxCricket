package com.example.skyboxcricket

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import kotlin.math.max

class PaymentSplitAutoFillHelper(
    private val totalAmountEditText: TextInputEditText,
    private val onlineAmountEditText: TextInputEditText,
    private val offlineAmountEditText: TextInputEditText
) {

    private var isUpdating = false
    private var lastEditedField = Field.ONLINE

    private val onlineWatcher = simpleWatcher {
        if (onlineAmountEditText.hasFocus()) {
            lastEditedField = Field.ONLINE
        }
        syncFrom(Field.ONLINE)
    }

    private val offlineWatcher = simpleWatcher {
        if (offlineAmountEditText.hasFocus()) {
            lastEditedField = Field.OFFLINE
        }
        syncFrom(Field.OFFLINE)
    }

    fun attach() {
        onlineAmountEditText.addTextChangedListener(onlineWatcher)
        offlineAmountEditText.addTextChangedListener(offlineWatcher)
    }

    fun detach() {
        onlineAmountEditText.removeTextChangedListener(onlineWatcher)
        offlineAmountEditText.removeTextChangedListener(offlineWatcher)
    }

    private fun syncFrom(source: Field) {
        if (isUpdating) return

        val total = totalAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val online = onlineAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val offline = offlineAmountEditText.text.toString().toDoubleOrNull() ?: 0.0

        val remaining = when (source) {
            Field.ONLINE -> max(0.0, total - online)
            Field.OFFLINE -> max(0.0, total - offline)
        }

        isUpdating = true
        when (source) {
            Field.ONLINE -> setIfChanged(offlineAmountEditText, formatAmount(remaining))
            Field.OFFLINE -> setIfChanged(onlineAmountEditText, formatAmount(remaining))
        }
        isUpdating = false
    }

    private fun setIfChanged(editText: TextInputEditText, newValue: String) {
        if (editText.text?.toString() == newValue) return
        editText.setText(newValue)
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun formatAmount(value: Double): String {
        val rounded = String.format(Locale.US, "%.2f", value)
        return rounded
            .removeSuffix("00")
            .removeSuffix(".")
    }

    private fun simpleWatcher(afterChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                afterChanged()
            }
        }
    }

    private enum class Field {
        ONLINE,
        OFFLINE
    }
}
