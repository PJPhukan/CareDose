package com.example.caredose.database.entities
enum class DurationType(val displayName: String) {
    CONTINUOUS("Continuous"),
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months");

    companion object {
        fun fromString(value: String): DurationType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                CONTINUOUS
            }
        }
        fun getDisplayNames(): List<String> {
            return DurationType.entries.map { it.displayName }
        }

        fun fromDisplayName(displayName: String): DurationType {
            return DurationType.entries.find {
                it.displayName.equals(displayName, ignoreCase = true)
            } ?: CONTINUOUS
        }
    }
}