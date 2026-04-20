package com.example.skyboxcricket

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BookingRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    fun addBooking(
        booking: Booking,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onError("User session expired. Please login again.")
            return
        }

        val reference = database.getReference("users").child(userId).child("bookings").push()
        val bookingToSave = booking.copy(id = reference.key.orEmpty())

        reference.setValue(bookingToSave)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: "Unable to save booking.")
            }
    }

    fun observeBookings(
        onChanged: (List<Booking>) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener? {
        val userId = auth.currentUser?.uid ?: run {
            onError("User session expired. Please login again.")
            return null
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookings = snapshot.children.mapNotNull { it.getValue(Booking::class.java) }
                    .sortedByDescending { it.createdAt }
                onChanged(bookings)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        database.getReference("users").child(userId).child("bookings")
            .addValueEventListener(listener)

        return listener
    }

    fun removeListener(listener: ValueEventListener?) {
        val userId = auth.currentUser?.uid ?: return
        if (listener == null) return
        database.getReference("users").child(userId).child("bookings")
            .removeEventListener(listener)
    }
}
