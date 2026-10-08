package br.com.odin.general.notification.domain

enum class NotificationStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    CANCELLED,
    SKIPPED,
}
