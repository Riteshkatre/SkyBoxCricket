package com.example.skyboxcricket

data class Booking(
    val id: String = "",
    val customerName: String = "",
    val boxSelection: String = "",
    val bookingDateTime: String = "",
    val toDateTime: String = "",
    val boxPrice: Double = 0.0,
    val cafePrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val onlineAmount: Double = 0.0,
    val offlineAmount: Double = 0.0,
    val createdAt: Long = 0L
)
