package com.example.skyboxcricket

import java.util.Locale

object UserAccess {

    private val privilegedEmails = setOf(
        "skye.boxcricket2023@gamil.com",
        "skye.boxcricket2023@gmail.com",
        "katrelucky810@gmail.com",
        "securee12.11@gmail.com"
    )

    fun isPrivilegedUser(email: String?): Boolean {
        val normalizedEmail = email?.trim()?.lowercase(Locale.ROOT) ?: return false
        return privilegedEmails.contains(normalizedEmail)
    }
}
