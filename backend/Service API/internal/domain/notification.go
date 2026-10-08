package domain

import (
	"time"

	"github.com/google/uuid"
)

type NotificationStatus string

const (
	StatusPending    NotificationStatus = "PENDING"
	StatusProcessing NotificationStatus = "PROCESSING"
	StatusSent       NotificationStatus = "SENT"
	StatusFailed     NotificationStatus = "FAILED"
	StatusCancelled  NotificationStatus = "CANCELLED"
	StatusSkipped    NotificationStatus = "SKIPPED"
)

type NotificationJob struct {
	ID             uuid.UUID
	UserID         uuid.UUID
	TaskID         *uuid.UUID
	GroupID        *uuid.UUID
	Type           string
	Title          string
	Message        string
	ScheduledAt    time.Time
	Status         NotificationStatus
	Attempts       int
	MaxAttempts    int
	IdempotencyKey string
}

type Device struct {
	ID        uuid.UUID
	UserID    uuid.UUID
	Platform  string
	PushToken string
	Provider  string
}

type Delivery struct {
	JobID            uuid.UUID
	DeviceID         uuid.UUID
	Provider         string
	Status           string
	ProviderResponse string
	ErrorMessage     string
	SentAt           *time.Time
}
