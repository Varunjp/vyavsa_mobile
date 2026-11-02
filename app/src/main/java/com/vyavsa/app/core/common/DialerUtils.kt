package com.vyavsa.app.core.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utility for safely launching phone dialer actions without requiring runtime permissions.
 */
object DialerUtils {

    /**
     * Creates an [Intent] configured with [Intent.ACTION_DIAL] for the provided phone number.
     * Uses Uri.fromParts("tel", phoneNumber, null) to safely populate the dialer.
     */
    fun createDialIntent(phoneNumber: String): Intent {
        val cleanNumber = phoneNumber.trim()
        val dialUri = Uri.fromParts("tel", cleanNumber, null)
        return Intent(Intent.ACTION_DIAL, dialUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Safely opens the device's default phone dialer with [phoneNumber] pre-filled.
     *
     * Invariants:
     * - Uses [Intent.ACTION_DIAL] exclusively (never [Intent.ACTION_CALL]).
     * - Does NOT require or request android.permission.CALL_PHONE.
     * - Handles blank, null, or invalid phone numbers gracefully.
     * - Catches [ActivityNotFoundException] or other unexpected launch errors without crashing.
     *
     * @return true if the intent was dispatched, false otherwise.
     */
    fun dialPhoneNumber(context: Context, phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank()) {
            return false
        }
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isEmpty()) {
            return false
        }

        return try {
            val intent = createDialIntent(cleanNumber)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            try {
                Toast.makeText(context, "No dialer application found", Toast.LENGTH_SHORT)?.show()
            } catch (_: Throwable) {}
            false
        } catch (e: Exception) {
            try {
                Toast.makeText(context, "Unable to open dialer", Toast.LENGTH_SHORT)?.show()
            } catch (_: Throwable) {}
            false
        }
    }
}
