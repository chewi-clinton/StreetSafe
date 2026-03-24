package com.example.safesense.domain.model

data class AlertResult(
    val contactName: String,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
