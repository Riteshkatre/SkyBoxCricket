package com.example.skyboxcricket

import java.util.Locale
import kotlin.math.abs

object PaymentSplitValidator {

    fun getRemainingAmount(totalAmount: Double, onlineAmount: Double, offlineAmount: Double): Double {
        return totalAmount - (onlineAmount + offlineAmount)
    }

    fun isValid(totalAmount: Double, onlineAmount: Double, offlineAmount: Double): Boolean {
        return abs(getRemainingAmount(totalAmount, onlineAmount, offlineAmount)) <= 0.01
    }

    fun formatAmount(amount: Double): String {
        val rounded = String.format(Locale.US, "%.2f", abs(amount))
        return rounded.removeSuffix("00").removeSuffix(".")
    }
}
