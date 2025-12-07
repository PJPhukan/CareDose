package com.example.caredose

sealed class States<out T> {
    object Idle : States<Nothing>()
    object Loading : States<Nothing>()
    data class Success<T>(val data: T) : States<T>()
    data class Error(val message: String) : States<Nothing>()
}