package com.university.attendance

/**
 * Centralized validation rules for the Add Teacher form.
 * Mirrors the logic used for students to keep CNIC/phone formatting
 * consistent across the whole app.
 */
object TeacherValidationUtils {

    /** Pakistani CNIC format: 13 digits, optionally with dashes (e.g. 12345-1234567-1) */
    private val CNIC_REGEX = Regex("^\\d{5}-?\\d{7}-?\\d{1}$")

    /** Pakistani mobile number format: 03XXXXXXXXX (11 digits) or +923XXXXXXXXX */
    private val PHONE_REGEX = Regex("^(\\+92|0)3\\d{9}$")

    fun isValidCnic(cnic: String): Boolean = CNIC_REGEX.matches(cnic.trim())

    fun isValidPhone(phone: String): Boolean = PHONE_REGEX.matches(phone.trim().replace(" ", ""))

    fun isNotBlank(value: String): Boolean = value.trim().isNotEmpty()

    /** Strips dashes/spaces so CNIC is stored in one consistent format: 13 raw digits. */
    fun normalizeCnic(cnic: String): String = cnic.trim().replace("-", "").replace(" ", "")

    /** Normalizes phone to start with 0 (converts +92 to 0) for consistent storage. */
    fun normalizePhone(phone: String): String {
        val trimmed = phone.trim().replace(" ", "")
        return if (trimmed.startsWith("+92")) "0" + trimmed.removePrefix("+92") else trimmed
    }
}