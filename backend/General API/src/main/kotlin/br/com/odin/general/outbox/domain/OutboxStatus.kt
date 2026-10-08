package br.com.odin.general.outbox.domain

enum class OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
